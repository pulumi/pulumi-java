// Copyright 2024, Pulumi Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"sync"
	"testing"

	"github.com/pulumi/pulumi-java/pkg/internal/executors"
	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
	"github.com/pulumi/pulumi/sdk/v3"
	"github.com/pulumi/pulumi/sdk/v3/go/common/diag"
	pbempty "google.golang.org/protobuf/types/known/emptypb"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/fsutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/rpcutil"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
	testingrpc "github.com/pulumi/pulumi/sdk/v3/proto/go/testing"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// TestLanguage runs the language conformance test suite against the Java language host.
func TestLanguage(t *testing.T) {
	t.Parallel()

	engineAddress, engine := runTestingHost(t)

	tests, err := engine.GetLanguageTests(context.Background(), &testingrpc.GetLanguageTestsRequest{})
	require.NoError(t, err)

	cancel := make(chan bool)

	rootDir := t.TempDir()

	// Boot up the Java language host.
	handle, err := rpcutil.ServeWithOptions(rpcutil.ServeOptions{
		Init: func(srv *grpc.Server) error {
			host := newLanguageHost(
				executors.JavaExecutorOptions{
					WD:          fsys.DirFS(rootDir),
					UseExecutor: "mvn",
				},
				engineAddress,
				"", /*tracing*/
			)

			pulumirpc.RegisterLanguageRuntimeServer(srv, host)
			return nil
		},
		Cancel: cancel,
	})
	require.NoError(t, err)

	snapshotDir := "./testdata/"
	coreSDKDirectory := t.TempDir()

	// When building the core SDK normally, the Protobuf Gradle plugin takes care of generating code from .proto files
	// via a relative reference to the pulumi submodule that looks something like $rootDir/../../pulumi/proto. For this
	// build process to work when Gradle is run as part of the LanguageRuntime.Pack RPC method, which the conformance
	// tests use, we need to set up the directory structure just right before we pass a path to Pack. To this end we
	// explicitly copy the core SDK source and appropriate .proto files over so that the structure lines up just so.
	// This is a bit of a hack but works fine for now.
	err = fsutil.CopyFile(coreSDKDirectory, "../../../sdk/java", nil)
	require.NoError(t, err)
	err = fsutil.CopyFile(filepath.Join(coreSDKDirectory, "pulumi", "src", "main", "proto"), "../../../pulumi/proto", nil)
	require.NoError(t, err)

	prepare, err := engine.PrepareLanguageTests(context.Background(), &testingrpc.PrepareLanguageTestsRequest{
		LanguagePluginName:   "java",
		LanguagePluginTarget: fmt.Sprintf("127.0.0.1:%d", handle.Port),
		TemporaryDirectory:   rootDir,
		SnapshotDirectory:    snapshotDir,
		CoreSdkDirectory:     coreSDKDirectory,
		CoreSdkVersion:       sdk.Version.String(),
		SnapshotEdits: []*testingrpc.PrepareLanguageTestsRequest_Replacement{
			// build.gradle and pom.xml files generated as part of conformance tests will reference local/alpha versions
			// of the core Pulumi SDK. We'll rewrite these since they'll change as the version of this repository moves
			// forward.
			{
				Path:        "build.gradle",
				Pattern:     `implementation\("com.pulumi:pulumi:.*"\)`,
				Replacement: `implementation("com.pulumi:pulumi:CORE.VERSION")`,
			},
			{
				Path:        "pom.xml",
				Pattern:     `(?m)(<artifactId>pulumi</artifactId>[\s]*)<version>.*</version>`,
				Replacement: `$1<version>CORE.VERSION</version>`,
			},

			// build.gradle and pom.xml files generated as part of conformance tests will reference local Maven
			// repositories containing built artifacts, such as the core SDK and provider SDKs used in the test. We'll
			// rewrite these paths out since they'll change every time we run the tests.
			{
				Path:        "build.gradle",
				Pattern:     `url\("([^h]|h[^t]|ht[^t]|htt[^p]).*"\)`,
				Replacement: `url("REPOSITORY")`,
			},
			{
				Path:        "pom.xml",
				Pattern:     "<url>file://.*</url>",
				Replacement: "<url>REPOSITORY</url>",
			},
		},
	})
	require.NoError(t, err)

	for _, tt := range tests.Tests {
		tt := tt
		t.Run(tt, func(t *testing.T) {
			t.Parallel()

			if expected, ok := expectedFailures[tt]; ok {
				t.Skipf("Skipping known failure: %s", expected)
			}

			result, err := engine.RunLanguageTest(context.Background(), &testingrpc.RunLanguageTestRequest{
				Token: prepare.Token,
				Test:  tt,
			})

			require.NoError(t, err)
			for _, msg := range result.Messages {
				t.Log(msg)
			}
			t.Logf("stdout: %s", result.Stdout)
			t.Logf("stderr: %s", result.Stderr)
			assert.True(t, result.Success)
		})
	}

	t.Cleanup(func() {
		close(cancel)
		assert.NoError(t, <-handle.Done)
	})
}

// expectedFailures maps the set of conformance tests we expect to fail to reasons they currently do so, so that we may
// skip them with an informative message until they are fixed.
var expectedFailures = map[string]string{
	"l1-output-array":                       "#1560 Empty array literals are not generated correctly",
	"l1-output-map":                         "#1561 Map literals are not generated correctly",
	"l1-output-string":                      "#1562 Large string literals are not generated correctly",
	"l2-invoke-dependencies":                "#1563 Invoke argument and result handling",
	"l2-invoke-options":                     "#1563 Invoke argument and result handling",
	"l2-invoke-options-depends-on":          "#1563 Invoke argument and result handling",
	"l2-invoke-secrets":                     "#1563 Invoke argument and result handling",
	"l2-map-keys":                           "#1569 Spot and qualify duplicate identifiers",
	"l2-provider-grpc-config-schema-secret": "#1568 Don't generate duplicate files",
	"l2-provider-grpc-config-secret":        "#1568 Don't generate duplicate files",
	"l2-provider-grpc-config":               "#1568 Don't generate duplicate files",
	"l2-resource-asset-archive":             "#1567 Implement remoteAsset and assetArchive",
	"l2-resource-config":                    "#1566 Fix l2-resource-config / plugin download URL code generation",
	"l2-resource-primitives":                "#1565 Better generation of numeric literals",
	"l2-resource-secret":                    "#1564 Fix l2-resource-secret",
}

// runTestingHost boots up a new instance of the language conformance test runner, `pulumi-test-language`, as well as a
// fake Pulumi engine for collecting logs. It returns the address of the fake engine and a connection to the test runner
// that can be used to manage a test suite run.
func runTestingHost(t *testing.T) (string, testingrpc.LanguageTestClient) {
	// We can't just go run the pulumi-test-language package because of
	// https://github.com/golang/go/issues/39172, so we build it to a temp file then run that.
	binary := t.TempDir() + "/pulumi-test-language"
	cmd := exec.Command("go", "build", "-C", "../../../pulumi/cmd/pulumi-test-language", "-o", binary)
	output, err := cmd.CombinedOutput()
	t.Logf("build output: %s", output)
	require.NoError(t, err)

	cmd = exec.Command(binary)
	stdout, err := cmd.StdoutPipe()
	require.NoError(t, err)
	stderr, err := cmd.StderrPipe()
	require.NoError(t, err)
	stderrReader := bufio.NewReader(stderr)

	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		for {
			text, err := stderrReader.ReadString('\n')
			if err != nil {
				wg.Done()
				return
			}
			t.Logf("engine: %s", text)
		}
	}()

	err = cmd.Start()
	require.NoError(t, err)

	stdoutBytes, err := io.ReadAll(stdout)
	require.NoError(t, err)

	address := string(stdoutBytes)

	conn, err := grpc.NewClient(
		address,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithUnaryInterceptor(rpcutil.OpenTracingClientInterceptor()),
		grpc.WithStreamInterceptor(rpcutil.OpenTracingStreamClientInterceptor()),
		rpcutil.GrpcChannelOptions(),
	)
	require.NoError(t, err)

	client := testingrpc.NewLanguageTestClient(conn)

	t.Cleanup(func() {
		assert.NoError(t, cmd.Process.Kill())
		wg.Wait()
		// We expect this to error because we just killed it.
		contract.IgnoreError(cmd.Wait())
	})

	engineAddress := runEngine(t)
	return engineAddress, client
}

// runEngine boots up a hostEngine for receiving logs from the language runtime under test so that they can be
// incorporated into test log output.
func runEngine(t *testing.T) string {
	engine := &hostEngine{t: t}
	stop := make(chan bool)
	t.Cleanup(func() {
		close(stop)
	})
	handle, err := rpcutil.ServeWithOptions(rpcutil.ServeOptions{
		Cancel: stop,
		Init: func(srv *grpc.Server) error {
			pulumirpc.RegisterEngineServer(srv, engine)
			return nil
		},
		Options: rpcutil.OpenTracingServerInterceptorOptions(nil),
	})
	require.NoError(t, err)
	return fmt.Sprintf("127.0.0.1:%v", handle.Port)
}

// hostEngine is a fake implementation of the Engine gRPC interface which accepts log messages (in this case, from the
// language host) and forwards on to the supplied T's Log method.
type hostEngine struct {
	pulumirpc.UnimplementedEngineServer
	t *testing.T

	logLock         sync.Mutex
	logRepeat       int
	previousMessage string
}

// Implements the Engine.Log RPC method. Forwards received log messages on to this hostEngine's T.Log.
func (e *hostEngine) Log(_ context.Context, req *pulumirpc.LogRequest) (*pbempty.Empty, error) {
	e.logLock.Lock()
	defer e.logLock.Unlock()

	var sev diag.Severity
	switch req.Severity {
	case pulumirpc.LogSeverity_DEBUG:
		sev = diag.Debug
	case pulumirpc.LogSeverity_INFO:
		sev = diag.Info
	case pulumirpc.LogSeverity_WARNING:
		sev = diag.Warning
	case pulumirpc.LogSeverity_ERROR:
		sev = diag.Error
	default:
		return nil, fmt.Errorf("Unrecognized logging severity: %v", req.Severity)
	}

	message := req.Message
	if os.Getenv("PULUMI_LANGUAGE_TEST_SHOW_FULL_OUTPUT") != "true" {
		// Cut down logs so they don't overwhelm the test output
		if len(message) > 1024 {
			message = message[:1024] + "... (truncated, run with PULUMI_LANGUAGE_TEST_SHOW_FULL_OUTPUT=true to see full logs))"
		}
	}

	if e.previousMessage == message {
		e.logRepeat++
		return &pbempty.Empty{}, nil
	}

	if e.logRepeat > 1 {
		e.t.Logf("Last message repeated %d times", e.logRepeat)
	}
	e.logRepeat = 1
	e.previousMessage = message

	if req.StreamId != 0 {
		e.t.Logf("(%d) %s[%s]: %s", req.StreamId, sev, req.Urn, message)
	} else {
		e.t.Logf("%s[%s]: %s", sev, req.Urn, message)
	}
	return &pbempty.Empty{}, nil
}
