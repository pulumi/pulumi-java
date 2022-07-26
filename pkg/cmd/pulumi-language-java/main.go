// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"

	pbempty "github.com/golang/protobuf/ptypes/empty"
	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/executable"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/version"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
	"google.golang.org/grpc"

	"github.com/pulumi/pulumi-java/pkg/internal/executors"
	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
	"google.golang.org/protobuf/types/known/emptypb"
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

	javaExec, err := executors.NewJavaExecutor(executors.JavaExecutorOptions{
		Binary:      binary,
		UseExecutor: useExecutor,
		WD:          fsys.DirFS(wd),
	})
	if err != nil {
		cmdutil.Exit(err)
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
			host := newLanguageHost(javaExec, engineAddress, tracing)
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
	// If we have a host cancel our cancellation context if it fails the healthcheck
	ctx, cancel := context.WithCancel(context.Background())

	// map the context Done channel to the rpcutil boolean cancel channel
	cancelChannel := make(chan bool)
	go func() {
		<-ctx.Done()
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
	exec          *executors.JavaExecutor
	engineAddress string
	tracing       string
}

func newLanguageHost(exec *executors.JavaExecutor, engineAddress, tracing string) pulumirpc.LanguageRuntimeServer {
	return &javaLanguageHost{
		exec:          exec,
		engineAddress: engineAddress,
		tracing:       tracing,
	}
}

// GetRequiredPlugins computes the complete set of anticipated plugins required by a program.
func (host *javaLanguageHost) GetRequiredPlugins(
	ctx context.Context,
	req *pulumirpc.GetRequiredPluginsRequest) (*pulumirpc.GetRequiredPluginsResponse, error) {

	logging.V(5).Infof("GetRequiredPlugins: program=%v", req.GetProgram())

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
	ctx context.Context) ([]plugin.PulumiPluginJSON, error) {

	logging.V(3).Infof("GetRequiredPlugins: Determining Pulumi plugins")

	// Run our classpath introspection from the SDK and parse the resulting JSON
	cmd := host.exec.Cmd
	args := host.exec.PluginArgs
	output, err := host.runJavaCommand(ctx, host.exec.Dir, cmd, args)
	if err != nil {
		// Plugin determination is an advisory feature so it does not need to escalate to an error.
		logging.V(3).Infof("language host could not run plugin discovery command successfully, "+
			"returning empty plugins; cause: %s", err)
		return []plugin.PulumiPluginJSON{}, nil
	}

	logging.V(5).Infof("GetRequiredPlugins: bootstrap raw output=%v", output)

	var plugins []plugin.PulumiPluginJSON
	err = json.Unmarshal([]byte(output), &plugins)
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

func (host *javaLanguageHost) runJavaCommand(
	ctx context.Context, dir, name string, args []string) (string, error) {

	commandStr := strings.Join(args, " ")
	if logging.V(5) {
		logging.V(5).Infoln("Language host launching process: ", name, commandStr)
	}

	// Buffer the writes we see from build tool, from its stdout and stderr streams.
	// We will display these ephemerally to the user. If the build does fail though, we will dump
	// messages back to our own stdout/stderr, so they get picked up and displayed to the user.

	infoWriter := &bytes.Buffer{}
	errorWriter := &bytes.Buffer{}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	cmd := exec.Command(name, args...) // nolint: gas // intentionally running dynamic program name.
	if dir != "" {
		cmd.Dir = dir
	}

	cmd.Stdout = infoWriter
	cmd.Stderr = errorWriter

	if err := runCommand(cmd); err != nil {
		// The command failed. Dump any data we collected to
		// the actual stdout/stderr streams, so they get
		// displayed to the user.
		os.Stdout.Write(infoWriter.Bytes())
		os.Stderr.Write(errorWriter.Bytes())
		return "", err
	}

	if logging.V(5) {
		logging.V(5).Infof("'%v %v' completed successfully\n", name, commandStr)
	}

	return infoWriter.String(), nil
}

// Run is an RPC endpoint for LanguageRuntimeServer::Run
func (host *javaLanguageHost) Run(ctx context.Context, req *pulumirpc.RunRequest) (*pulumirpc.RunResponse, error) {
	logging.V(5).Infof("Run: program=%v", req.GetProgram())

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

	// Run from source.
	executable := host.exec.Cmd
	args := host.exec.RunArgs

	if logging.V(5) {
		commandStr := strings.Join(args, " ")
		logging.V(5).Infoln("Language host launching process: ", executable, commandStr)
	}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	var errResult string
	cmd := exec.Command(executable, args...) // nolint: gas // intentionally running dynamic program name.
	if host.exec.Dir != "" {
		cmd.Dir = host.exec.Dir
	}

	var stdoutBuf bytes.Buffer
	var stderrBuf bytes.Buffer

	cmd.Stdout = &stdoutBuf
	cmd.Stderr = &stderrBuf
	cmd.Env = host.constructEnv(req, config, configSecretKeys)
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

func (host *javaLanguageHost) GetPluginInfo(ctx context.Context, req *pbempty.Empty) (*pulumirpc.PluginInfo, error) {
	return &pulumirpc.PluginInfo{
		Version: version.Version,
	}, nil
}

func (host *javaLanguageHost) InstallDependencies(req *pulumirpc.InstallDependenciesRequest,
	server pulumirpc.LanguageRuntime_InstallDependenciesServer) error {

	// Executor may not support the build command (for example, jar executor).
	if host.exec.BuildArgs == nil {
		logging.V(5).Infof("InstallDependencies(Directory=%s): skipping", req.Directory)
		return nil
	}

	logging.V(5).Infof("InstallDependencies(Directory=%s): starting", req.Directory)

	closer, stdout, stderr, err := rpcutil.MakeStreams(server, req.IsTerminal)
	if err != nil {
		return err
	}
	defer closer.Close()

	// intentionally running dynamic program name.
	cmd := exec.Command(host.exec.Cmd, host.exec.BuildArgs...) // nolint: gas
	if host.exec.Dir != "" {
		cmd.Dir = host.exec.Dir
	}
	cmd.Stdout = stdout
	cmd.Stderr = stderr

	if err := runCommand(cmd); err != nil {
		logging.V(5).Infof("InstallDependencies(Directory=%s): failed", req.Directory)
		return err
	}

	logging.V(5).Infof("InstallDependencies(Directory=%s): done", req.Directory)
	return nil
}

func (host *javaLanguageHost) GetProgramDependencies(
	ctx context.Context, req *pulumirpc.GetProgramDependenciesRequest) (*pulumirpc.GetProgramDependenciesResponse, error) {
	// TODO: Implement dependency fetcher for Java
	return &pulumirpc.GetProgramDependenciesResponse{}, nil
}

func (host *javaLanguageHost) About(ctx context.Context, req *emptypb.Empty) (*pulumirpc.AboutResponse, error) {
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
