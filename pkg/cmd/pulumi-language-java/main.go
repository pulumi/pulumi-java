// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"encoding/xml"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"regexp"
	"strings"
	"syscall"
	"time"

	pbempty "github.com/golang/protobuf/ptypes/empty"
	"github.com/hashicorp/hcl/v2"
	"github.com/pkg/errors"
	hclsyntax "github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/syntax"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/errutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/executable"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/fsutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/workspace"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
	"golang.org/x/sync/errgroup"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/structpb"

	codegen "github.com/pulumi/pulumi-java/pkg/codegen/java"
	"github.com/pulumi/pulumi-java/pkg/internal/executors"
	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
	"github.com/pulumi/pulumi-java/pkg/version"
)

// Launches the language host RPC endpoint, which in turn fires up an RPC server implementing the
// LanguageRuntimeServer RPC endpoint.
func main() {
	var tracing string
	flag.StringVar(&tracing, "tracing", "", "Emit tracing to a Zipkin-compatible tracing endpoint")

	flag.String("root", "", "[obsolete] Project root path to use")
	flag.String("binary", "", "[obsolete] JAR or a JBang entry-point file to execute")
	flag.String("use-executor", "", "[obsolete] Use the given program as the executor instead of looking for one on PATH")

	flag.Parse()
	var cancelChannel chan bool
	args := flag.Args()
	logging.InitLogging(false, 0, false)
	cmdutil.InitTracing("pulumi-language-java", "pulumi-language-java", tracing)

	// Optionally pluck out the engine so we can do logging, etc.
	var engineAddress string
	if len(args) > 0 {
		engineAddress = args[0]
		var err error
		cancelChannel, err = setupHealthChecks(engineAddress)
		if err != nil {
			cmdutil.Exit(errors.Wrapf(err, "could not start health check host RPC server"))
		}
	}

	// Fire up a gRPC server, letting the kernel choose a free port.
	handle, err := rpcutil.ServeWithOptions(rpcutil.ServeOptions{
		Cancel: cancelChannel,
		Init: func(srv *grpc.Server) error {
			host := newLanguageHost(engineAddress, tracing)
			pulumirpc.RegisterLanguageRuntimeServer(srv, host)
			return nil
		},
	})
	if err != nil {
		cmdutil.Exit(errors.Wrapf(err, "could not start language host RPC server"))
	}

	// Otherwise, print out the port so that the spawner knows how to reach us.
	fmt.Printf("%d\n", handle.Port)

	// And finally wait for the server to stop serving.
	if err := <-handle.Done; err != nil {
		cmdutil.Exit(errors.Wrapf(err, "language host RPC stopped serving"))
	}
}

func setupHealthChecks(engineAddress string) (chan bool, error) {
	// If the health check begins failing or we receive a SIGINT,
	// we'll cancel the context.
	//
	// The returned channel is used to notify the server that it should
	// stop serving and exit.
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt)

	// map the context Done channel to the rpcutil boolean cancel channel
	cancelChannel := make(chan bool)
	go func() {
		<-ctx.Done()
		cancel() // deregister the interrupt handler
		close(cancelChannel)
	}()
	err := rpcutil.Healthcheck(ctx, engineAddress, 5*time.Minute, cancel)
	if err != nil {
		return nil, err
	}
	return cancelChannel, nil
}

// javaLanguageHost implements the LanguageRuntimeServer interface
// for use as an API endpoint.
type javaLanguageHost struct {
	pulumirpc.UnimplementedLanguageRuntimeServer

	engineAddress string
	tracing       string
}

func newLanguageHost(
	engineAddress, tracing string,
) pulumirpc.LanguageRuntimeServer {
	return &javaLanguageHost{
		engineAddress: engineAddress,
		tracing:       tracing,
	}
}

func (host *javaLanguageHost) Executor(
	execOptions executors.JavaExecutorOptions, attachDebugger bool,
) (*executors.JavaExecutor, error) {
	executor, err := executors.NewJavaExecutor(execOptions, attachDebugger)
	if err != nil {
		return nil, err
	}
	return executor, nil
}

func (host *javaLanguageHost) parseExecOptions(info *pulumirpc.ProgramInfo) (executors.JavaExecutorOptions, error) {
	javaOptions := executors.JavaExecutorOptions{
		WD: fsys.DirFS(info.ProgramDirectory),
	}

	options := info.Options.AsMap()

	if binary, ok := options["binary"]; ok {
		if binary, ok := binary.(string); ok {
			javaOptions.Binary = binary
		} else {
			return javaOptions, errors.New("binary option must be a string")
		}
	}

	if executor, ok := options["use-executor"]; ok {
		if executor, ok := executor.(string); ok {
			javaOptions.UseExecutor = executor
		} else {
			return javaOptions, errors.New("use-executor option must be a string")
		}
	}

	return javaOptions, nil
}

// GetRequiredPackages computes the complete set of anticipated packages required by a program.
func (host *javaLanguageHost) GetRequiredPackages(
	ctx context.Context,
	req *pulumirpc.GetRequiredPackagesRequest,
) (*pulumirpc.GetRequiredPackagesResponse, error) {
	logging.V(5).Infof("GetRequiredPackages: programDirectory=%v", req.Info.ProgramDirectory)

	execOptions, err := host.parseExecOptions(req.Info)
	if err != nil {
		return nil, fmt.Errorf("parsing options: %w", err)
	}

	pulumiPackages, err := host.determinePulumiPackages(ctx, execOptions, req.Info.ProgramDirectory)
	if err != nil {
		return nil, errors.Wrapf(err, "language host could not determine Pulumi packages")
	}

	pkgs := []*pulumirpc.PackageDependency{}
	for _, pulumiPackage := range pulumiPackages {
		// Skip over any packages that don't correspond to Pulumi resource plugins.
		if !pulumiPackage.Resource {
			continue
		}

		pkg := &pulumirpc.PackageDependency{
			Kind:    "resource",
			Name:    pulumiPackage.Name,
			Version: pulumiPackage.Version,
			Server:  pulumiPackage.Server,
		}
		if pulumiPackage.Parameterization != nil {
			pkg.Parameterization = &pulumirpc.PackageParameterization{
				Name:    pulumiPackage.Parameterization.Name,
				Version: pulumiPackage.Parameterization.Version,
				Value:   pulumiPackage.Parameterization.Value,
			}
		}

		pkgs = append(pkgs, pkg)
	}

	logging.V(5).Infof("GetRequiredPackages: packages=%v", pkgs)
	return &pulumirpc.GetRequiredPackagesResponse{Packages: pkgs}, nil
}

// GetRequiredPlugins computes the complete set of anticipated plugins required by a program.
func (host *javaLanguageHost) GetRequiredPlugins(
	context.Context,
	*pulumirpc.GetRequiredPluginsRequest,
) (*pulumirpc.GetRequiredPluginsResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetRequiredPlugins not implemented")
}

func (host *javaLanguageHost) determinePulumiPackages(
	ctx context.Context,
	execOptions executors.JavaExecutorOptions,
	programDirectory string,
) ([]plugin.PulumiPluginJSON, error) {
	logging.V(3).Infof("GetRequiredPlugins: Determining Pulumi plugins")

	exec, err := host.Executor(execOptions, false)
	if err != nil {
		return nil, err
	}

	// Run our classpath introspection from the SDK and parse the resulting JSON
	cmd := exec.Cmd
	args := exec.PluginArgs
	quiet := true
	output, err := host.runJavaCommand(ctx, programDirectory, cmd, args, quiet)
	if err != nil {
		// Plugin determination is an advisory feature so it does not need to escalate to an error.
		logging.V(3).Infof("language host could not run plugin discovery command successfully, "+
			"returning empty plugins; cause: %s", err)
		logging.V(3).Infof("%s", output.stdout)
		logging.V(3).Infof("%s", output.stderr)
		return []plugin.PulumiPluginJSON{}, nil
	}

	logging.V(5).Infof("GetRequiredPlugins: bootstrap raw output=%v", output)

	var plugins []plugin.PulumiPluginJSON
	err = json.Unmarshal([]byte(output.stdout), &plugins)
	if err != nil {
		if e, ok := err.(*json.SyntaxError); ok {
			logging.V(5).Infof("JSON syntax error at byte offset %d", e.Offset)
		}
		// Plugin determination is an advisory feature so it doe not need to escalate to an error.
		logging.V(3).Infof("language host could not unmarshall plugin package information, "+
			"returning empty plugins; cause: %s", err)
		return []plugin.PulumiPluginJSON{}, nil
	}

	return plugins, nil
}

type javaCommandResponse struct {
	stdout string
	stderr string
}

func (host *javaLanguageHost) runJavaCommand(
	_ context.Context, dir, name string, args []string, quiet bool,
) (javaCommandResponse, error) {
	commandStr := strings.Join(args, " ")
	if logging.V(5) {
		logging.V(5).Infoln("Language host launching process: ", name, commandStr)
	}

	stdoutBuffer := &bytes.Buffer{}
	stderrBuffer := &bytes.Buffer{}

	var stdoutWriter io.Writer = stdoutBuffer
	var stderrWriter io.Writer = stderrBuffer

	if !quiet {
		stdoutWriter = io.MultiWriter(os.Stdout, stdoutWriter)
		stderrWriter = io.MultiWriter(os.Stderr, stderrWriter)
	}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	cmd := exec.Command(name, args...) // nolint: gas // intentionally running dynamic program name.
	if dir != "" {
		cmd.Dir = dir
	}

	cmd.Stdout = stdoutWriter
	cmd.Stderr = stderrWriter

	err := runCommand(cmd)

	if err == nil && logging.V(5) {
		logging.V(5).Infof("'%v %v' completed successfully\n", name, commandStr)
	}

	return javaCommandResponse{
		stdout: stdoutBuffer.String(),
		stderr: stderrBuffer.String(),
	}, err
}

func (host *javaLanguageHost) connectToEngine() (pulumirpc.EngineClient, io.Closer, error) {
	conn, err := grpc.NewClient(
		host.engineAddress,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		rpcutil.GrpcChannelOptions(),
	)
	if err != nil {
		return nil, nil, fmt.Errorf("language host could not make connection to engine: %w", err)
	}

	engineClient := pulumirpc.NewEngineClient(conn)
	return engineClient, conn, nil
}

// Run is an RPC endpoint for LanguageRuntimeServer::Run
func (host *javaLanguageHost) Run(ctx context.Context, req *pulumirpc.RunRequest) (*pulumirpc.RunResponse, error) {
	logging.V(5).Infof("Run: program=%v", req.GetProgram()) //nolint:staticcheck

	engineClient, closer, err := host.connectToEngine()
	if err != nil {
		return nil, err
	}
	defer contract.IgnoreClose(closer)

	execOptions, err := host.parseExecOptions(req.Info)
	if err != nil {
		return nil, fmt.Errorf("parsing options: %w", err)
	}
	execOptions.ProgramArgs = req.Args

	config, err := host.constructConfig(req)
	if err != nil {
		err = errors.Wrap(err, "failed to serialize configuration")
		return nil, err
	}
	configSecretKeys, err := host.constructConfigSecretKeys(req)
	if err != nil {
		err = errors.Wrap(err, "failed to serialize configuration secret keys")
		return nil, err
	}

	executor, err := host.Executor(execOptions, req.GetAttachDebugger())
	if err != nil {
		return nil, err
	}

	// Run from source.
	executable := executor.Cmd
	args := executor.RunArgs

	if logging.V(5) {
		commandStr := strings.Join(args, " ")
		logging.V(5).Infoln("Language host launching process: ", executable, commandStr)
	}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	var errResult string
	cmd := exec.Command(executable, args...) // nolint: gas // intentionally running dynamic program name.
	cmd.Dir = req.Info.ProgramDirectory

	var stdoutBuf bytes.Buffer
	var stderrBuf bytes.Buffer

	// We need to process the output of the command to determine when the debugger is ready, but
	// also want to tee the output to the engine so that it can be displayed to the user.
	pr, pw := io.Pipe()

	cmd.Stdout = pw
	cmd.Stderr = &stderrBuf

	tr := io.TeeReader(pr, &stdoutBuf)

	cmd.Env = host.constructEnv(req, config, configSecretKeys)
	go func() {
		err := WaitForDebuggerReady(tr)
		if err != nil {
			logging.Errorf("failed to wait for debugger: %v", err)
			contract.IgnoreError(cmd.Process.Kill())
		}

		// emit a debug configuration
		debugConfig, err := structpb.NewStruct(map[string]interface{}{
			"name":     "Pulumi: Program (Java)",
			"type":     "java",
			"request":  "attach",
			"hostName": "localhost",
			"port":     8000,
		})
		if err != nil {
			logging.Errorf("failed to serialize debug configuration: %v", err)
			contract.IgnoreError(cmd.Process.Kill())
		}
		_, err = engineClient.StartDebugging(ctx, &pulumirpc.StartDebuggingRequest{
			Config:  debugConfig,
			Message: "on port 8000",
		})
		if err != nil {
			logging.Errorf("unable to start debugging: %v", err)
			contract.IgnoreError(cmd.Process.Kill())
		}
	}()
	if err := runCommand(cmd); err != nil {
		// The command failed. Dump any data we collected to
		// the actual stdout/stderr streams, so they get
		// displayed to the user.
		os.Stdout.Write(stdoutBuf.Bytes())
		os.Stderr.Write(stderrBuf.Bytes())

		errResult = err.Error()
	}

	return &pulumirpc.RunResponse{Error: errResult}, nil
}

// RunPlugin executes a plugin program and returns its result.
func (host *javaLanguageHost) RunPlugin(
	req *pulumirpc.RunPluginRequest, server pulumirpc.LanguageRuntime_RunPluginServer,
) error {
	logging.V(5).Infof("Attempting to run java plugin in %s", req.Pwd)

	closer, stdout, stderr, err := rpcutil.MakeRunPluginStreams(server, false)
	if err != nil {
		return err
	}
	// Best effort close, but we try an explicit close and error check at the end as well
	defer closer.Close()

	execOptions, err := host.parseExecOptions(req.Info)
	if err != nil {
		return fmt.Errorf("parsing options: %w", err)
	}
	execOptions.ProgramArgs = req.Args

	executor, err := executors.NewJavaExecutor(execOptions, req.GetAttachDebugger())
	if err != nil {
		return err
	}

	executable := executor.Cmd
	args := executor.RunPluginArgs

	if len(args) == 0 {
		return errors.Errorf("executor %s does not currently support running plugins", executor.Cmd)
	}

	commandStr := strings.Join(args, " ")
	logging.V(5).Infof("Language host launching process: %s %s", executable, commandStr)

	cmd := exec.Command(executable, args...)
	cmd.Dir = req.Pwd
	cmd.Env = req.Env
	if req.GetAttachDebugger() {
		var tr io.Reader
		pr, pw := io.Pipe()
		readyPr, readyPw := io.Pipe()
		tr = io.TeeReader(pr, readyPw)
		cmd.Stdout = pw
		cmd.Stderr = stderr

		go func() {
			// If we have a debugger attached filter the output
			scanner := bufio.NewScanner(tr)
			for scanner.Scan() {
				// only print if we have the port number in the output.
				// mvnDebug prints other stuff, which RunPlugin doesn't
				// expect, so we have to filter it out.
				if regexp.MustCompile("%d+").MatchString(scanner.Text()) {
					stdout.Write(scanner.Bytes())
				}
			}
		}()

		go func() {
			err := WaitForDebuggerReady(readyPr)
			if err != nil {
				logging.Errorf("failed to wait for debugger: %v", err)
				contract.IgnoreError(cmd.Process.Kill())
			}

			// emit a debug configuration
			debugConfig, err := structpb.NewStruct(map[string]any{
				"name":     fmt.Sprintf("Pulumi: Plugin (%s)", req.GetName()),
				"type":     "java",
				"request":  "attach",
				"hostName": "localhost",
				"port":     8000,
			})
			if err != nil {
				logging.Errorf("failed to serialize debug configuration: %v", err)
				contract.IgnoreError(cmd.Process.Kill())
			}
			engineClient, closer, err := host.connectToEngine()
			if err != nil {
				logging.Errorf("unable to connect to host engine: %v", err)
				contract.IgnoreError(cmd.Process.Kill())
			}
			defer contract.IgnoreClose(closer)
			_, err = engineClient.StartDebugging(server.Context(), &pulumirpc.StartDebuggingRequest{
				Config:  debugConfig,
				Message: fmt.Sprintf("on port %d", 8000),
			})
			if err != nil {
				logging.Errorf("unable to start debugging: %v", err)
				contract.IgnoreError(cmd.Process.Kill())
			}
		}()

	} else {
		cmd.Stdout = stdout
		cmd.Stderr = stderr
	}

	if err = cmd.Run(); err != nil {
		var exiterr *exec.ExitError
		if errors.As(err, &exiterr) {
			if status, ok := exiterr.Sys().(syscall.WaitStatus); ok {
				return server.Send(&pulumirpc.RunPluginResponse{
					//nolint:gosec // WaitStatus always uses the lower 8 bits for the exit code.
					Output: &pulumirpc.RunPluginResponse_Exitcode{Exitcode: int32(status.ExitStatus())},
				})
			}
			if len(exiterr.Stderr) > 0 {
				return fmt.Errorf("program exited unexpectedly: %w: %s", exiterr, exiterr.Stderr)
			}
			return fmt.Errorf("program exited unexpectedly: %w", exiterr)
		}
		return fmt.Errorf("problem executing plugin program (could not run language executor): %w", err)
	}

	return closer.Close()
}

// constructEnv constructs an environ for `pulumi-language-java`
// by enumerating all of the optional and non-optional evn vars present
// in a RunRequest.
func (host *javaLanguageHost) constructEnv(req *pulumirpc.RunRequest, config, configSecretKeys string) []string {
	env := os.Environ()

	maybeAppendEnv := func(k, v string) {
		if v != "" {
			env = append(env, strings.ToUpper("PULUMI_"+k)+"="+v)
		}
	}

	maybeAppendEnv("monitor", req.GetMonitorAddress())
	maybeAppendEnv("engine", host.engineAddress)
	maybeAppendEnv("organization", req.GetOrganization())
	maybeAppendEnv("project", req.GetProject())
	maybeAppendEnv("stack", req.GetStack())
	maybeAppendEnv("pwd", req.GetPwd())
	maybeAppendEnv("dry_run", fmt.Sprintf("%v", req.GetDryRun()))
	maybeAppendEnv("parallel", fmt.Sprint(req.GetParallel()))
	maybeAppendEnv("tracing", host.tracing)
	maybeAppendEnv("config", config)
	maybeAppendEnv("config_secret_keys", configSecretKeys)

	return env
}

func (host *javaLanguageHost) Link(
	_ context.Context, req *pulumirpc.LinkRequest,
) (*pulumirpc.LinkResponse, error) {
	if len(req.Packages) == 0 {
		return &pulumirpc.LinkResponse{}, nil
	}

	instructions := ""
	if len(req.Packages) > 1 {
		names := make([]string, len(req.Packages))
		for i, pkg := range req.Packages {
			names[i] = pkg.Package.Name
		}
		instructions += fmt.Sprintf("Successfully generated Java SDKs for packages %s\n\n", strings.Join(names, ", "))
		instructions += "To use these SDKs in your Java project, complete the following steps:\n"
		instructions += "1. Copy the contents of the generated SDKs to your Java project:\n"
	} else {
		instructions += fmt.Sprintf("Successfully generated a Java SDK for the package %s\n\n", req.Packages[0].Package.Name)
		instructions += "To use this SDK in your Java project, complete the following steps:\n"
		instructions += "1. Copy the contents of the generated SDK to your Java project:\n"
	}

	dest := filepath.Join(req.Info.ProgramDirectory, "src")
	for _, dep := range req.Packages {
		instructions += fmt.Sprintf("     cp -r %s/src/* %s\n", dep.Path, dest)
	}

	instructions += "\n"
	instructions += "2. Add the SDK dependencies to your Java project's build configuration.\n"
	instructions += "   If you are using Maven, add the following dependencies to your pom.xml:\n"
	instructions += "\n"
	instructions += "     <dependencies>\n"
	instructions += "         <dependency>\n"
	instructions += "             <groupId>com.google.code.findbugs</groupId>\n"
	instructions += "             <artifactId>jsr305</artifactId>\n"
	instructions += "             <version>3.0.2</version>\n"
	instructions += "         </dependency>\n"
	instructions += "         <dependency>\n"
	instructions += "             <groupId>com.google.code.gson</groupId>\n"
	instructions += "             <artifactId>gson</artifactId>\n"
	instructions += "             <version>2.8.9</version>\n"
	instructions += "         </dependency>\n"
	instructions += "     </dependencies>\n"
	instructions += "\n"

	return &pulumirpc.LinkResponse{
		ImportInstructions: instructions,
	}, nil
}

// constructConfig json-serializes the configuration data given as part of a RunRequest.
func (host *javaLanguageHost) constructConfig(req *pulumirpc.RunRequest) (string, error) {
	configMap := req.GetConfig()
	if configMap == nil {
		return "", nil
	}

	configJSON, err := json.Marshal(configMap)
	if err != nil {
		return "", err
	}

	return string(configJSON), nil
}

// constructConfigSecretKeys JSON-serializes the list of keys that contain secret values given as part of
// a RunRequest.
func (host *javaLanguageHost) constructConfigSecretKeys(req *pulumirpc.RunRequest) (string, error) {
	configSecretKeys := req.GetConfigSecretKeys()
	if configSecretKeys == nil {
		return "[]", nil
	}

	configSecretKeysJSON, err := json.Marshal(configSecretKeys)
	if err != nil {
		return "", err
	}

	return string(configSecretKeysJSON), nil
}

func (host *javaLanguageHost) GetPluginInfo(_ context.Context, _ *pbempty.Empty) (*pulumirpc.PluginInfo, error) {
	return &pulumirpc.PluginInfo{
		Version: version.Version,
	}, nil
}

func (host *javaLanguageHost) InstallDependencies(req *pulumirpc.InstallDependenciesRequest,
	server pulumirpc.LanguageRuntime_InstallDependenciesServer,
) error {
	execOptions, err := host.parseExecOptions(req.Info)
	if err != nil {
		return fmt.Errorf("parsing options: %w", err)
	}

	executor, err := host.Executor(execOptions, false)
	if err != nil {
		return err
	}

	// Executor may not support the build command (for example, jar executor).
	if executor.BuildArgs == nil {
		logging.V(5).Infof("InstallDependencies(Directory=%s): skipping", req.Directory) //nolint:staticcheck
		return nil
	}

	logging.V(5).Infof("InstallDependencies(Directory=%s): starting", req.Directory) //nolint:staticcheck

	closer, stdout, stderr, err := rpcutil.MakeInstallDependenciesStreams(server, req.IsTerminal)
	if err != nil {
		return err
	}
	defer closer.Close()

	// intentionally running dynamic program name.
	cmd := exec.Command(executor.Cmd, executor.BuildArgs...) // nolint: gosec
	cmd.Dir = req.Info.ProgramDirectory
	cmd.Stdout = stdout
	cmd.Stderr = stderr

	if err := runCommand(cmd); err != nil {
		logging.V(5).Infof("InstallDependencies(Directory=%s): failed", req.Directory) //nolint:staticcheck
		return err
	}

	logging.V(5).Infof("InstallDependencies(Directory=%s): done", req.Directory) //nolint:staticcheck
	return nil
}

func (host *javaLanguageHost) GetProgramDependencies(
	ctx context.Context,
	req *pulumirpc.GetProgramDependenciesRequest,
) (*pulumirpc.GetProgramDependenciesResponse, error) {
	execOptions, err := host.parseExecOptions(req.Info)
	if err != nil {
		return nil, fmt.Errorf("parsing options: %w", err)
	}

	executor, err := host.Executor(execOptions, false)
	if err != nil {
		return nil, err
	}

	if executor.GetProgramDependencies != nil {
		return executor.GetProgramDependencies(ctx, req)
	}

	return &pulumirpc.GetProgramDependenciesResponse{}, nil
}

func (host *javaLanguageHost) RuntimeOptionsPrompts(_ context.Context,
	_ *pulumirpc.RuntimeOptionsRequest,
) (*pulumirpc.RuntimeOptionsResponse, error) {
	return &pulumirpc.RuntimeOptionsResponse{}, nil
}

// WaitForDebuggerReady waits for the debugger to be ready by scanning the output of the process.
// Note that we currently only support `mvnDebug` as debugger.  If we support more debuggers in the
// future this will need to be updated.
func WaitForDebuggerReady(out io.Reader) error {
	scanner := bufio.NewScanner(out)
	for scanner.Scan() {
		if strings.Contains(scanner.Text(), "Listening for transport dt_socket at address") {
			return nil
		}
	}
	return scanner.Err()
}

func (host *javaLanguageHost) About(ctx context.Context, _ *pulumirpc.AboutRequest) (*pulumirpc.AboutResponse, error) {
	getResponse := func(ctx context.Context, execString string, args ...string) (string, string, error) {
		ex, err := executable.FindExecutable(execString)
		if err != nil {
			return "", "", fmt.Errorf("could not find executable '%s': %w", execString, err)
		}
		cmd := exec.CommandContext(ctx, ex, args...)
		var out []byte
		if out, err = cmd.Output(); err != nil {
			cmd := ex
			if len(args) != 0 {
				cmd += " " + strings.Join(args, " ")
			}
			return "", "", fmt.Errorf("failed to execute '%s'", cmd)
		}
		return ex, strings.TrimSpace(string(out)), nil
	}

	var javaExec, javaOutput, javacOutput, mavenOutput, gradleOutput string

	g, ctx := errgroup.WithContext(ctx)

	g.Go(func() error {
		var err error
		javaExec, javaOutput, err = getResponse(ctx, "java", "--version")
		return err
	})

	g.Go(func() error {
		_, javacOutput, _ = getResponse(ctx, "javac", "--version")
		return nil // Don't fail the group if javac is not found
	})

	g.Go(func() error {
		_, mavenOutput, _ = getResponse(ctx, "mvn", "--version")
		return nil // Don't fail the group if maven is not found
	})

	g.Go(func() error {
		_, gradleOutput, _ = getResponse(ctx, "gradle", "--version")
		return nil // Don't fail the group if gradle is not found
	})

	if err := g.Wait(); err != nil {
		return nil, err
	}

	metadata := make(map[string]string)
	metadata["java"] = strings.Split(javaExec, "\n")[0]

	if javacOutput != "" {
		metadata["javac"] = strings.TrimPrefix(javacOutput, "javac ")
	}

	if mavenOutput != "" {
		metadata["maven"] = strings.Split(mavenOutput, "\n")[0]
	}

	if gradleOutput != "" {
		for _, line := range strings.Split(gradleOutput, "\n") {
			if strings.HasPrefix(line, "Gradle") {
				metadata["gradle"] = strings.TrimPrefix(line, "Gradle ")
				break
			}
		}
	}

	javaVersion := strings.Split(javaOutput, "\n")[0]

	return &pulumirpc.AboutResponse{
		Executable: javaExec,
		Version:    javaVersion,
		Metadata:   metadata,
	}, nil
}

// Implements the `LanguageRuntime.GenerateProject` RPC method, which generates a Java project for the given request.
// This is just a thin RPC wrapper around the `pkg/codegen` implementation of `GenerateProject`. A project encompasses a
// program (that is, Java source code) and any other pieces needed to build, run and package that program (e.g. build
// system files, dependencies, etc.). Presently Java project generation uses Maven to this end.
func (host *javaLanguageHost) GenerateProject(
	_ context.Context,
	req *pulumirpc.GenerateProjectRequest,
) (*pulumirpc.GenerateProjectResponse, error) {
	loader, err := schema.NewLoaderClient(req.LoaderTarget)
	if err != nil {
		return nil, err
	}

	var extraOptions []pcl.BindOption
	if !req.Strict {
		extraOptions = append(extraOptions, pcl.NonStrictBindOptions()...)
	}

	extraOptions = append(extraOptions, pcl.PreferOutputVersionedInvokes)

	program, diags, err := pcl.BindDirectory(req.SourceDirectory, loader, extraOptions...)
	if err != nil {
		return nil, err
	}

	if diags.HasErrors() {
		rpcDiagnostics := plugin.HclDiagnosticsToRPCDiagnostics(diags)
		return &pulumirpc.GenerateProjectResponse{
			Diagnostics: rpcDiagnostics,
		}, nil
	}

	if program == nil {
		return nil, errors.New("internal error: program was nil")
	}

	var project workspace.Project
	if err := json.Unmarshal([]byte(req.Project), &project); err != nil {
		return nil, err
	}

	err = codegen.GenerateProject(
		req.TargetDirectory,
		project,
		program,
		req.LocalDependencies,
	)
	if err != nil {
		return nil, err
	}

	rpcDiagnostics := plugin.HclDiagnosticsToRPCDiagnostics(diags)
	return &pulumirpc.GenerateProjectResponse{
		Diagnostics: rpcDiagnostics,
	}, nil
}

// Implements the `LanguageRuntime.GenerateProgram` RPC method, which generates a Java program for the given request.
// This is just a thin RPC wrapper around the `pkg/codegen` implementation of programgen.
func (host *javaLanguageHost) GenerateProgram(
	_ context.Context,
	req *pulumirpc.GenerateProgramRequest,
) (*pulumirpc.GenerateProgramResponse, error) {
	loader, err := schema.NewLoaderClient(req.LoaderTarget)
	if err != nil {
		return nil, err
	}

	parser := hclsyntax.NewParser()
	// Load all .pp files in the directory
	for path, contents := range req.Source {
		err = parser.ParseFile(strings.NewReader(contents), path)
		if err != nil {
			return nil, err
		}
		diags := parser.Diagnostics
		if diags.HasErrors() {
			return nil, diags
		}
	}

	program, diags, err := pcl.BindProgram(parser.Files, pcl.Loader(loader), pcl.PreferOutputVersionedInvokes)
	if err != nil {
		return nil, err
	}

	rpcDiagnostics := plugin.HclDiagnosticsToRPCDiagnostics(diags)
	if diags.HasErrors() {
		return &pulumirpc.GenerateProgramResponse{
			Diagnostics: rpcDiagnostics,
		}, nil
	}
	if program == nil {
		return nil, errors.New("internal error: program was nil")
	}

	files, diags, err := codegen.GenerateProgram(program)
	if err != nil {
		return nil, err
	}
	rpcDiagnostics = append(rpcDiagnostics, plugin.HclDiagnosticsToRPCDiagnostics(diags)...)

	return &pulumirpc.GenerateProgramResponse{
		Source:      files,
		Diagnostics: rpcDiagnostics,
	}, nil
}

// Implements the `LanguageRuntime.GeneratePackage` RPC method, which generates a Java SDK package from a supplied
// schema.
func (host *javaLanguageHost) GeneratePackage(
	_ context.Context,
	req *pulumirpc.GeneratePackageRequest,
) (*pulumirpc.GeneratePackageResponse, error) {
	loader, err := schema.NewLoaderClient(req.LoaderTarget)
	if err != nil {
		return nil, err
	}

	var spec schema.PackageSpec
	err = json.Unmarshal([]byte(req.Schema), &spec)
	if err != nil {
		return nil, err
	}

	diags := hcl.Diagnostics{}

	// Historically, Java has "deduplicated" PackageSpecs to reduce sets of multiple types whose names differ only in
	// case down to just one type that is then shared (assuming that, apart from name, the types are otherwise
	// identical). We thus perform that deduplication here before we bind the schema and resolve any references.
	dedupedSpec, dedupeDiags, err := codegen.DeduplicateTypes(&spec)
	if err != nil {
		return nil, err
	}
	diags = diags.Extend(dedupeDiags)
	if dedupeDiags.HasErrors() {
		return &pulumirpc.GeneratePackageResponse{
			Diagnostics: plugin.HclDiagnosticsToRPCDiagnostics(diags),
		}, nil
	}

	pkg, bindDiags, err := schema.BindSpec(*dedupedSpec, loader, schema.ValidationOptions{
		AllowDanglingReferences: true,
	})
	if err != nil {
		return nil, err
	}
	diags = diags.Extend(bindDiags)
	if bindDiags.HasErrors() {
		return &pulumirpc.GeneratePackageResponse{
			Diagnostics: plugin.HclDiagnosticsToRPCDiagnostics(diags),
		}, nil
	}

	files, err := codegen.GeneratePackage(
		"pulumi-language-java",
		pkg,
		req.ExtraFiles,
		req.LocalDependencies,
		req.Local,
		false, /*legacyBuildFiles*/
	)
	if err != nil {
		return nil, err
	}

	for filename, data := range files {
		outPath := filepath.Join(req.Directory, filename)
		err := os.MkdirAll(filepath.Dir(outPath), 0o700)
		if err != nil {
			return nil, fmt.Errorf("could not create output directory %s: %w", filepath.Dir(filename), err)
		}

		err = os.WriteFile(outPath, data, 0o600)
		if err != nil {
			return nil, fmt.Errorf("could not write output file %s: %w", filename, err)
		}
	}

	return &pulumirpc.GeneratePackageResponse{
		Diagnostics: plugin.HclDiagnosticsToRPCDiagnostics(diags),
	}, nil
}

// Implements the `LanguageRuntime.Pack` RPC method, which packs a package (SDK) into a language-specific artifact. In
// the case of Java, the artifacts in question are *Java Archives* (or JARs/`.jar` files).
//
// The incoming `PackRequest` specifies the directory to be packaged and where the packed artifact should be produced.
// Since we expect that our packed artifacts will be consumed by other Java libraries and programs, this implementation
// creates a Maven artifact repository at the specified location and publishes a `.jar` there using the
// `publishToMavenLocal` Gradle target (generated SDKs, which is what we expect to be packing, currently use Gradle as
// their build system). We then return in the `PackResponse` the path to the `.jar` we published.
func (host *javaLanguageHost) Pack(_ context.Context, req *pulumirpc.PackRequest) (*pulumirpc.PackResponse, error) {
	buildPath, err := os.MkdirTemp("", "pulumi-java-pack")
	if err != nil {
		return nil, fmt.Errorf("failed to create build directory: %w", err)
	}

	repoPath := filepath.Join(req.DestinationDirectory, "repo")

	err = fsutil.CopyFile(buildPath, req.PackageDirectory, nil)
	if err != nil {
		return nil, fmt.Errorf("copy package: %w", err)
	}

	name, err := getGradleProperty(buildPath, "name")
	if err != nil {
		return nil, fmt.Errorf("get gradle project name: %w", err)
	}

	// Generated SDKs reside at the top level of the generated directory tree. Due to the layout of this repository,
	// however, the core Pulumi SDK resides under a `pulumi` subdirectory. We thus implement a hack here to handle this
	// case and adjust the build path accordingly.
	gradleWd := buildPath

	publication := "mainPublication"
	generatePOMTask := "generatePomFileForMainPublicationPublication"

	if name == "pulumi" {
		gradleWd = filepath.Join(buildPath, "pulumi")

		publication = "gpr"
		generatePOMTask = "generatePomFileForGprPublication"
	}

	gradleGeneratePOMCmd := exec.Command(
		"gradle",
		generatePOMTask,
	)
	gradleGeneratePOMCmd.Dir = gradleWd
	gradleGeneratePOMCmd.Stdout = os.Stdout
	gradleGeneratePOMCmd.Stderr = os.Stderr
	err = gradleGeneratePOMCmd.Run()
	if err != nil {
		return nil, fmt.Errorf("gradle generatePom: %w", err)
	}

	var pomProject POMProject
	pomPath := filepath.Join(gradleWd, "build", "publications", publication, "pom-default.xml")
	pomFile, err := os.ReadFile(pomPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read generated POM file: %w", err)
	}

	err = xml.Unmarshal(pomFile, &pomProject)
	if err != nil {
		return nil, fmt.Errorf("failed to unmarshal generated POM file: %w", err)
	}

	//nolint:gosec
	gradlePublishCmd := exec.Command(
		"gradle",
		"-Dmaven.repo.local="+repoPath, //nolint:gosec
		"publishToMavenLocal",
	)
	gradlePublishCmd.Dir = gradleWd
	gradlePublishCmd.Stdout = os.Stdout
	gradlePublishCmd.Stderr = os.Stderr
	err = gradlePublishCmd.Run()
	if err != nil {
		return nil, errutil.ErrorWithStderr(err, "gradle publish")
	}

	artifactPath := fmt.Sprintf(
		"%s:%s:%s:%s",
		pomProject.Group,
		pomProject.Artifact,
		pomProject.Version,
		repoPath,
	)

	err = os.RemoveAll(buildPath)
	if err != nil {
		return nil, fmt.Errorf("failed to remove build path: %w", err)
	}

	return &pulumirpc.PackResponse{ArtifactPath: artifactPath}, nil
}

func getGradleProperty(projectDir string, property string) (string, error) {
	var stdout bytes.Buffer
	cmd := exec.Command("gradle", "properties")
	cmd.Dir = projectDir
	cmd.Stdout = &stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("gradle properties: %w", err)
	}

	scanner := bufio.NewScanner(&stdout)
	prefix := fmt.Sprintf("%s: ", property)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, prefix) {
			return strings.TrimPrefix(line, prefix), nil
		}
	}

	return "", fmt.Errorf("could not find property %s in gradle properties", property)
}

// POMProject represents a subset of the Maven Project Object Model (POM) file specification. It is used in this module
// for decoding key information from a `pom.xml` file.
type POMProject struct {
	// The top-level `<project>` tag in the POM file.
	XMLName xml.Name `xml:"project"`
	// The ID of the group or company that created the project, encoded in the `<groupId>` tag.
	Group string `xml:"groupId"`
	// The ID of the artifact produced by the project, encoded in the `<artifactId>` tag.
	Artifact string `xml:"artifactId"`
	// The version of the artifact produced by the project, encoded in the `<version>` tag.
	Version string `xml:"version"`
}
