// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

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
	var binary string
	flag.StringVar(&tracing, "tracing", "", "Emit tracing to a Zipkin-compatible tracing endpoint")
	flag.StringVar(&root, "root", "", "Project root path to use")
	flag.StringVar(&binary, "binary", "", "A relative or an absolute path to a JAR to execute")

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
	case binary != "":
		logging.V(3).Infof("language host asked to use specific JAR: `%s`", binary)
		cmd, err := lookupPath("java")
		if err != nil {
			cmdutil.Exit(err)
		}
		jvmExec, err = newJarExecutor(cmd, binary)
		if err != nil {
			cmdutil.Exit(err)
		}
	default:
		pathExec, err := probeExecutor()
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

type jvmExecutor struct {
	cmd        string
	buildArgs  []string
	runArgs    []string
	pluginArgs []string
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

func (host *jvmLanguageHost) determinePulumiPackages(
	ctx context.Context) ([]plugin.PulumiPluginJSON, error) {

	logging.V(3).Infof("GetRequiredPlugins: Determining Pulumi plugins")

	// Run our classpath introspection from the SDK and parse the resulting JSON
	cmd := host.exec.cmd
	args := host.exec.pluginArgs
	output, err := host.runJvmCommand(ctx, cmd, args)
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

func (host *jvmLanguageHost) runJvmCommand(
	ctx context.Context, name string, args []string) (string, error) {

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

func (host *jvmLanguageHost) InstallDependencies(req *pulumirpc.InstallDependenciesRequest,
	server pulumirpc.LanguageRuntime_InstallDependenciesServer) error {

	// Executor may not support the build command (for example, jar executor).
	if host.exec.buildArgs == nil {
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
	cmd := exec.Command(host.exec.cmd, host.exec.buildArgs...) // nolint: gas
	cmd.Stdout = stdout
	cmd.Stderr = stderr

	if err := runCommand(cmd); err != nil {
		logging.V(5).Infof("InstallDependencies(Directory=%s): failed", req.Directory)
		return err
	}

	logging.V(5).Infof("InstallDependencies(Directory=%s): done", req.Directory)
	return nil
}

func probeExecutor() (string, error) {
	pwd, err := os.Getwd()
	if err != nil {
		return "", errors.Wrap(err, "could not get the working directory")
	}
	files, err := ioutil.ReadDir(pwd)
	if err != nil {
		return "", errors.Wrap(err, "could not read the working directory")
	}
	mvn := "mvn"
	// detect mvn wrapper
	for _, file := range files {
		if !file.IsDir() && file.Name() == "mvnw" {
			mvn = "./mvnw"
		}
	}
	gradle := "gradle"
	// detect gradle wrapper
	for _, file := range files {
		if !file.IsDir() && file.Name() == "gradlew" {
			gradle = "./gradlew"
		}
	}
	// detect maven or gradle
	for _, file := range files {
		if !file.IsDir() {
			switch file.Name() {
			case "pom.xml":
				return mvn, nil
			case "settings.gradle", "settings.gradle.kts":
				return gradle, nil
			}
		}
	}
	return "", errors.New("did not found an executor, expected one of: gradle (settings.gradle), maven (pom.xml)")
}

func resolveExecutor(exec string) (*jvmExecutor, error) {
	switch exec {
	case "gradle", "./gradlew":
		cmd, err := lookupPath(exec)
		if err != nil {
			return nil, err
		}
		return newGradleExecutor(cmd)
	case "mvn", "./mvnw":
		cmd, err := lookupPath(exec)
		if err != nil {
			return nil, err
		}
		return newMavenExecutor(cmd)
	default:
		return nil, errors.Errorf("did not recognize executor '%s', "+
			"expected one of: gradle, mvn, gradlew, mvnw", exec)
	}
}

func newGradleExecutor(cmd string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:       cmd,
		buildArgs: []string{"build", "--console=plain"},
		runArgs:   []string{"run", "--console=plain"},
		pluginArgs: []string{
			"-q", // must first due to a bug https://github.com/gradle/gradle/issues/5098
			"run", "--console=plain",
			"-PmainClass=com.pulumi.bootstrap.internal.Main",
			"--args=packages",
		},
	}, nil
}

func newMavenExecutor(cmd string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:       cmd,
		buildArgs: []string{"--no-transfer-progress", "compile"},
		runArgs:   []string{"--no-transfer-progress", "compile", "exec:java"},
		pluginArgs: []string{
			"--quiet", "--no-transfer-progress", "compile", "exec:java",
			"-DmainClass=com.pulumi.bootstrap.internal.Main",
			"-DmainArgs=packages",
		},
	}, nil
}

func newJarExecutor(cmd string, path string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:        cmd,
		buildArgs:  nil, // not supported
		runArgs:    []string{"-jar", filepath.Clean(path)},
		pluginArgs: []string{"-cp", filepath.Clean(path), "com.pulumi.bootstrap.internal.Main", "packages"},
	}, nil
}

func lookupPath(file string) (string, error) {
	pathExec, err := exec.LookPath(file)
	if err != nil {
		return "", errors.Wrapf(err, "could not find `%s` on the $PATH", file)
	}
	return pathExec, nil
}
