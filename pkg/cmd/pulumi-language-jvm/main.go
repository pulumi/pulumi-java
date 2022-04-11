// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os"
	"os/exec"
	"strings"
	"syscall"

	pbempty "github.com/golang/protobuf/ptypes/empty"
	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/version"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
	"google.golang.org/grpc"
)

// Launches the language host RPC endpoint, which in turn fires up an RPC server implementing the
// LanguageRuntimeServer RPC endpoint.
func main() {
	var tracing string
	var root string
	var jar string
	flag.StringVar(&tracing, "tracing", "", "Emit tracing to a Zipkin-compatible tracing endpoint")
	flag.StringVar(&root, "root", "", "Project root path to use")
	flag.StringVar(&jar, "jar", "", "A relative or an absolute path to a JAR to execute")

	// You can use the below flag to request that the language host load a specific executor instead of probing the
	// PATH.  This can be used during testing to override the default location.
	var givenExecutor string
	flag.StringVar(&givenExecutor, "use-executor", "",
		"Use the given program as the executor instead of looking for one on PATH")

	flag.Parse()
	args := flag.Args()
	logging.InitLogging(false, 0, false)
	cmdutil.InitTracing("pulumi-language-jvm", "pulumi-language-jvm", tracing)

	var jvmExec *jvmExecutor
	switch {
	case givenExecutor != "":
		logging.V(3).Infof("language host asked to use specific executor: `%s`", givenExecutor)
		var err error
		jvmExec, err = resolveExecutor(givenExecutor)
		if err != nil {
			cmdutil.Exit(err)
		}
	case jar != "":
		logging.V(3).Infof("language host asked to use specific JAR: `%s`", jar)
		cmd, err := lookupPath("java")
		if err != nil {
			cmdutil.Exit(err)
		}
		jvmExec, err = newJarExecutor(cmd, jar)
		if err != nil {
			cmdutil.Exit(err)
		}
	default:
		pwd, err := os.Getwd()
		if err != nil {
			cmdutil.Exit(errors.Wrap(err, "could not get the working directory"))
		}
		pathExec, err := probeExecutor(pwd)
		if err != nil {
			cmdutil.Exit(err)
		}
		logging.V(3).Infof("language host identified executor from path: `%s`", pathExec)
		jvmExec, err = resolveExecutor(pathExec)
		if err != nil {
			cmdutil.Exit(err)
		}
	}

	// Optionally pluck out the engine so we can do logging, etc.
	var engineAddress string
	if len(args) > 0 {
		engineAddress = args[0]
	}

	// Fire up a gRPC server, letting the kernel choose a free port.
	port, done, err := rpcutil.Serve(0, nil, []func(*grpc.Server) error{
		func(srv *grpc.Server) error {
			host := newLanguageHost(jvmExec, engineAddress, tracing)
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

// jvmLanguageHost implements the LanguageRuntimeServer interface
// for use as an API endpoint.
type jvmLanguageHost struct {
	exec          *jvmExecutor
	engineAddress string
	tracing       string
}

func newLanguageHost(exec *jvmExecutor, engineAddress, tracing string) pulumirpc.LanguageRuntimeServer {
	return &jvmLanguageHost{
		exec:          exec,
		engineAddress: engineAddress,
		tracing:       tracing,
	}
}

// GetRequiredPlugins computes the complete set of anticipated plugins required by a program.
func (host *jvmLanguageHost) GetRequiredPlugins(
	ctx context.Context,
	req *pulumirpc.GetRequiredPluginsRequest) (*pulumirpc.GetRequiredPluginsResponse, error) {

	logging.V(5).Infof("GetRequiredPlugins: program=%v", req.GetProgram())

	engineClient, err := newEngineClient(host.engineAddress)
	if err != nil {
		return nil, err
	}
	defer engineClient.Close()

	// now, introspect the user project to see which pulumi resource packages it references.
	pulumiPackages, err := host.DeterminePulumiPackages(ctx, engineClient)
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

func (host *jvmLanguageHost) DeterminePulumiPackages(
	ctx context.Context, engineClient pulumirpc.EngineClient) ([]plugin.PulumiPluginJSON, error) {

	logging.V(3).Infof("GetRequiredPlugins: Determining Pulumi plugins")

	// Run our classpath introspection from the SDK and parse the resulting JSON
	cmd := host.exec.cmd
	args := host.exec.pluginArgs
	output, err := host.RunJvmCommand(ctx, engineClient, cmd, args, false /*logToUser*/)
	if err != nil {
		return nil, errors.Wrapf(err, "language host counld not run plugin discovery command successfully")
	}

	logging.V(5).Infof("GetRequiredPlugins: bootstrap raw output=%v", output)

	var plugins []plugin.PulumiPluginJSON
	err = json.Unmarshal([]byte(output), &plugins)
	if err != nil {
		if e, ok := err.(*json.SyntaxError); ok {
			logging.V(5).Infof("JSON syntax error at byte offset %d", e.Offset)
		}
		return nil, errors.Wrapf(err, "language host could not unmarshall plugin package information")
	}

	return plugins, nil
}

func (host *jvmLanguageHost) RunJvmCommand(
	ctx context.Context, engineClient pulumirpc.EngineClient, name string, args []string, logToUser bool) (string, error) {

	commandStr := strings.Join(args, " ")
	if logging.V(5) {
		logging.V(5).Infoln("Language host launching process: ", name, commandStr)
	}

	// Buffer the writes we see from build tool, from its stdout and stderr streams.
	// We will display these ephemerally to the user. If the build does fail though, we will dump
	// messages back to our own stdout/stderr, so they get picked up and displayed to the user.
	streamID := rand.Int31() //nolint:gosec

	infoBuffer := &bytes.Buffer{}
	errorBuffer := &bytes.Buffer{}

	infoWriter := &logWriter{
		ctx:          ctx,
		logToUser:    logToUser,
		engineClient: engineClient,
		streamID:     streamID,
		buffer:       infoBuffer,
		severity:     pulumirpc.LogSeverity_INFO,
	}

	errorWriter := &logWriter{
		ctx:          ctx,
		logToUser:    logToUser,
		engineClient: engineClient,
		streamID:     streamID,
		buffer:       errorBuffer,
		severity:     pulumirpc.LogSeverity_ERROR,
	}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	cmd := exec.Command(name, args...) // nolint: gas // intentionally running dynamic program name.

	cmd.Stdout = infoWriter
	cmd.Stderr = errorWriter

	_, err := infoWriter.LogToUser(fmt.Sprintf("running '%v %v'", name, commandStr))
	if err != nil {
		return "", err
	}

	if err := cmd.Run(); err != nil {
		// The command failed. Dump any data we collected to the actual stdout/stderr streams,
		// so they get displayed to the user.
		os.Stdout.Write(infoBuffer.Bytes())
		os.Stderr.Write(errorBuffer.Bytes())

		if exiterr, ok := err.(*exec.ExitError); ok {
			// If the program ran, but exited with a non-zero error code.
			// This will happen often, since user errors will trigger this.
			// So, the error message should look as nice as possible.
			if status, stok := exiterr.Sys().(syscall.WaitStatus); stok {
				return "", errors.Errorf(
					"'%v %v' exited with non-zero exit code: %d", name, commandStr, status.ExitStatus())
			}

			return "", errors.Wrapf(exiterr, "'%v %v' exited unexpectedly", name, commandStr)
		}

		// Otherwise, we didn't even get to run the program.
		// This ought to never happen unless there's a bug
		// or system condition that prevented us from running the language exec.
		// Issue a scarier error.
		return "", errors.Wrapf(err, "Problem executing '%v %v'", name, commandStr)
	}

	_, err = infoWriter.LogToUser(fmt.Sprintf("'%v %v' completed successfully", name, commandStr))
	return infoBuffer.String(), err
}

type logWriter struct {
	ctx          context.Context
	logToUser    bool
	engineClient pulumirpc.EngineClient
	streamID     int32
	severity     pulumirpc.LogSeverity
	buffer       *bytes.Buffer
}

func (w *logWriter) Write(p []byte) (n int, err error) {
	n, err = w.buffer.Write(p)
	if err != nil {
		return
	}

	return w.LogToUser(string(p))
}

func (w *logWriter) LogToUser(val string) (int, error) {
	if w.logToUser {
		_, err := w.engineClient.Log(w.ctx, &pulumirpc.LogRequest{
			Message:   strings.ToValidUTF8(val, "�"),
			Urn:       "",
			Ephemeral: true,
			StreamId:  w.streamID,
			Severity:  w.severity,
		})

		if err != nil {
			return 0, err
		}
	}

	return len(val), nil
}

// Run is an RPC endpoint for LanguageRuntimeServer::Run
func (host *jvmLanguageHost) Run(ctx context.Context, req *pulumirpc.RunRequest) (*pulumirpc.RunResponse, error) {
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
	executable := host.exec.cmd
	args := host.exec.runArgs

	if logging.V(5) {
		commandStr := strings.Join(args, " ")
		logging.V(5).Infoln("Language host launching process: ", executable, commandStr)
	}

	// Now simply spawn a process to execute the requested program, wiring up stdout/stderr directly.
	var errResult string
	cmd := exec.Command(executable, args...) // nolint: gas // intentionally running dynamic program name.
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Env = host.constructEnv(req, config, configSecretKeys)
	if err := cmd.Run(); err != nil {
		if exiterr, ok := err.(*exec.ExitError); ok {
			// If the program ran, but exited with a non-zero error code.  This will happen often, since user
			// errors will trigger this.  So, the error message should look as nice as possible.
			if status, stok := exiterr.Sys().(syscall.WaitStatus); stok {
				err = errors.Errorf("Program exited with non-zero exit code: %d", status.ExitStatus())
			} else {
				err = errors.Wrapf(exiterr, "Program exited unexpectedly")
			}
		} else {
			// Otherwise, we didn't even get to run the program.  This ought to never happen unless there's
			// a bug or system condition that prevented us from running the language exec.  Issue a scarier error.
			err = errors.Wrapf(err, "Problem executing program (could not run language executor)")
		}

		errResult = err.Error()
	}

	return &pulumirpc.RunResponse{Error: errResult}, nil
}

// constructEnv constructs an environ for `pulumi-language-jvm`
// by enumerating all of the optional and non-optional evn vars present
// in a RunRequest.
func (host *jvmLanguageHost) constructEnv(req *pulumirpc.RunRequest, config, configSecretKeys string) []string {
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
func (host *jvmLanguageHost) constructConfig(req *pulumirpc.RunRequest) (string, error) {
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
func (host *jvmLanguageHost) constructConfigSecretKeys(req *pulumirpc.RunRequest) (string, error) {
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

func (host *jvmLanguageHost) GetPluginInfo(ctx context.Context, req *pbempty.Empty) (*pulumirpc.PluginInfo, error) {
	return &pulumirpc.PluginInfo{
		Version: version.Version,
	}, nil
}

func (host *jvmLanguageHost) InstallDependencies(
	req *pulumirpc.InstallDependenciesRequest,
	srv pulumirpc.LanguageRuntime_InstallDependenciesServer) error {
	return nil
}

func lookupPath(file string) (string, error) {
	pathExec, err := exec.LookPath(file)
	if err != nil {
		return "", errors.Wrapf(err, "could not find `%s` on the $PATH", file)
	}
	return pathExec, nil
}
