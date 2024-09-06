// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"os/signal"
	"strings"
	"time"

	pbempty "github.com/golang/protobuf/ptypes/empty"
	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/executable"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/version"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/pulumi/pulumi-java/pkg/internal/executors"
	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

// Launches the language host RPC endpoint, which in turn fires up an RPC server implementing the
// LanguageRuntimeServer RPC endpoint.
func main() {
	var tracing string
	var root string
	var binary string
	flag.StringVar(&tracing, "tracing", "", "Emit tracing to a Zipkin-compatible tracing endpoint")
	flag.StringVar(&root, "root", "", "Project root path to use")
	flag.StringVar(&binary, "binary", "", "JAR or a JBang entry-point file to execute")

	// You can use the below flag to request that the language host load a specific executor instead of probing the
	// PATH.  This can be used during testing to override the default location.
	var useExecutor string
	flag.StringVar(&useExecutor, "use-executor", "",
		"Use the given program as the executor instead of looking for one on PATH")

	flag.Parse()
	var cancelChannel chan bool
	args := flag.Args()
	logging.InitLogging(false, 0, false)
	cmdutil.InitTracing("pulumi-language-java", "pulumi-language-java", tracing)

	wd, err := os.Getwd()
	if err != nil {
		cmdutil.Exit(fmt.Errorf("could not get the working directory: %w", err))
	}

	javaExecOptions := executors.JavaExecutorOptions{
		Binary:      binary,
		UseExecutor: useExecutor,
		WD:          fsys.DirFS(wd),
	}

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
	port, done, err := rpcutil.Serve(0, cancelChannel, []func(*grpc.Server) error{
		func(srv *grpc.Server) error {
			host := newLanguageHost(javaExecOptions, engineAddress, tracing)
			pulumirpc.RegisterLanguageRuntimeServer(srv, host)
			return nil
		},
	}, nil)
	if err != nil {
		cmdutil.Exit(errors.Wrapf(err, "could not start language host RPC server"))
	}

	// Otherwise, print out the port so that the spawner knows how to reach us.
	fmt.Printf("%d\n", port)

	// And finally wait for the server to stop serving.
	if err := <-done; err != nil {
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

	currentExecutor *executors.JavaExecutor
	execOptions     executors.JavaExecutorOptions
	engineAddress   string
	tracing         string
}

func newLanguageHost(execOptions executors.JavaExecutorOptions,
	engineAddress, tracing string,
) pulumirpc.LanguageRuntimeServer {
	return &javaLanguageHost{
		execOptions:   execOptions,
		engineAddress: engineAddress,
		tracing:       tracing,
	}
}

func (host *javaLanguageHost) Executor(attachDebugger bool) (*executors.JavaExecutor, error) {
	if host.currentExecutor == nil || attachDebugger {
		executor, err := executors.NewJavaExecutor(host.execOptions, attachDebugger)
		if err != nil {
			return nil, err
		}
		host.currentExecutor = executor
	}
	return host.currentExecutor, nil
}

// GetRequiredPlugins computes the complete set of anticipated plugins required by a program.
func (host *javaLanguageHost) GetRequiredPlugins(
	ctx context.Context,
	req *pulumirpc.GetRequiredPluginsRequest,
) (*pulumirpc.GetRequiredPluginsResponse, error) {
	logging.V(5).Infof("GetRequiredPlugins: program=%v", req.GetProgram()) //nolint:staticcheck

	// now, introspect the user project to see which pulumi resource packages it references.
	pulumiPackages, err := host.determinePulumiPackages(ctx)
	if err != nil {
		return nil, errors.Wrapf(err, "language host could not determine Pulumi packages")
	}

	// Now that we know the set of pulumi packages referenced, and we know where packages have been restored to,
	// we can examine each package to determine the corresponding resource-plugin for it.

	plugins := []*pulumirpc.PluginDependency{}
	for _, pulumiPackage := range pulumiPackages {
		logging.V(3).Infof(
			"GetRequiredPlugins: Determining plugin dependency: %v, %v",
			pulumiPackage.Name, pulumiPackage.Version,
		)

		if !pulumiPackage.Resource {
			continue // the package has no associated resource plugin
		}

		plugins = append(plugins, &pulumirpc.PluginDependency{
			Name:    pulumiPackage.Name,
			Version: pulumiPackage.Version,
			Server:  pulumiPackage.Server,
			Kind:    "resource",
		})
	}

	logging.V(5).Infof("GetRequiredPlugins: plugins=%v", plugins)

	return &pulumirpc.GetRequiredPluginsResponse{Plugins: plugins}, nil
}

func (host *javaLanguageHost) determinePulumiPackages(
	ctx context.Context,
) ([]plugin.PulumiPluginJSON, error) {
	logging.V(3).Infof("GetRequiredPlugins: Determining Pulumi plugins")

	exec, err := host.Executor(false)
	if err != nil {
		return nil, err
	}

	// Run our classpath introspection from the SDK and parse the resulting JSON
	cmd := exec.Cmd
	args := exec.PluginArgs
	quiet := true
	output, err := host.runJavaCommand(ctx, exec.Dir, cmd, args, quiet)
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
	conn, err := grpc.Dial(
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

	executor, err := host.Executor(req.GetAttachDebugger())
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
	if executor.Dir != "" {
		cmd.Dir = executor.Dir
	}

	var stdoutBuf bytes.Buffer
	var stderrBuf bytes.Buffer

	pr, pw := io.Pipe()

	cmd.Stdout = pw
	cmd.Stderr = &stderrBuf

	tr := io.TeeReader(pr, &stdoutBuf)

	cmd.Env = host.constructEnv(req, config, configSecretKeys)
	go func() {
		WaitForDebuggerReady(ctx, tr)

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
			Message: fmt.Sprintf("on port 8000"),
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
	maybeAppendEnv("project", req.GetProject())
	maybeAppendEnv("stack", req.GetStack())
	maybeAppendEnv("pwd", req.GetPwd())
	maybeAppendEnv("dry_run", fmt.Sprintf("%v", req.GetDryRun()))
	maybeAppendEnv("query_mode", fmt.Sprint(req.GetQueryMode()))
	maybeAppendEnv("parallel", fmt.Sprint(req.GetParallel()))
	maybeAppendEnv("tracing", host.tracing)
	maybeAppendEnv("config", config)
	maybeAppendEnv("config_secret_keys", configSecretKeys)

	return env
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
	executor, err := host.Executor(false)
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
	cmd := exec.Command(executor.Cmd, executor.BuildArgs...) // nolint: gas
	if executor.Dir != "" {
		cmd.Dir = executor.Dir
	}
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
	_ context.Context, _ *pulumirpc.GetProgramDependenciesRequest,
) (*pulumirpc.GetProgramDependenciesResponse, error) {
	// TODO: Implement dependency fetcher for Java
	return &pulumirpc.GetProgramDependenciesResponse{}, nil
}

func WaitForDebuggerReady(ctx context.Context, out io.Reader) error {
	scanner := bufio.NewScanner(out)
	for scanner.Scan() {
		if strings.Contains(scanner.Text(), "Listening for transport dt_socket at address") {
			return nil
		}
	}
	return scanner.Err()
}

func (host *javaLanguageHost) About(_ context.Context, _ *pulumirpc.AboutRequest) (*pulumirpc.AboutResponse, error) {
	getResponse := func(execString string, args ...string) (string, string, error) {
		ex, err := executable.FindExecutable(execString)
		if err != nil {
			return "", "", fmt.Errorf("could not find executable '%s': %w", execString, err)
		}
		cmd := exec.Command(ex, args...)
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

	java, version, err := getResponse("java", "--version")
	if err != nil {
		return nil, err
	}

	metadata := make(map[string]string)
	metadata["java"] = strings.Split(java, "\n")[0]
	_, javac, err := getResponse("javac", "--version")
	if err != nil {
		javac = "unknown"
	}
	metadata["javac"] = strings.TrimPrefix(javac, "javac ")
	if _, maven, err := getResponse("mvn", "--version"); err == nil {
		// We add this only if there are no errors
		metadata["maven"] = strings.Split(maven, "\n")[0]
	}
	if _, gradle, err := getResponse("gradle", "--version"); err == nil {
		for _, line := range strings.Split(gradle, "\n") {
			if strings.HasPrefix(line, "Gradle") {
				metadata["gradle"] = strings.TrimPrefix(line, "Gradle ")
				break
			}
		}
	}

	return &pulumirpc.AboutResponse{
		Executable: java,
		Version:    version,
		Metadata:   metadata,
	}, nil
}
