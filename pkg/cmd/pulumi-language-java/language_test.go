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
	"os/exec"
	"path/filepath"
	"sync"
	"testing"

	"github.com/pulumi/pulumi/sdk/v3"

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
				engineAddress,
				"", /*tracing*/
				"", /*otel*/
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
		LanguageInfo: "{\"use-exectutor\":\"mvn\"}",
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
	"l1-builtin-project-root":                      "TODO: call rootDirectory",
	"l1-builtin-try":                               "#1683 Fix l1-builtin-try/can",
	"l1-builtin-can":                               "#1683 Fix l1-builtin-try/can",
	"l1-config-types-object":                       "https://github.com/pulumi/pulumi-java/issues/2005",
	"l1-config-types-primitive":                    "https://github.com/pulumi/pulumi-java/issues/2004",
	"l1-keyword-overlap":                           "#1681 Fix l1-keyword-overlap",
	"l1-output-null":                               "unhandled error occurred",
	"l1-output-string":                             "#1562 Large string literals are not generated correctly",
	"l1-proxy-index":                               "compilation error",
	"l2-component-call-simple":                     "#1680 Fix l2-component-call-simple",
	"l2-resource-option-deleted-with":              "#1682 Fix l2-resource-option-deleted-with",
	"l2-invoke-secrets":                            "#1563 Invoke argument and result handling",
	"l2-map-keys":                                  "#1561 Map literals are not generated correctly",
	"l2-provider-call":                             "invalid token",
	"l2-provider-call-explicit":                    "invalid token",
	"l2-provider-grpc-config-schema-secret":        "#1568 Don't generate duplicate files",
	"l2-provider-grpc-config-secret":               "#1568 Don't generate duplicate files",
	"l2-provider-grpc-config":                      "#1568 Don't generate duplicate files",
	"l2-proxy-index":                               "compilation error",
	"l2-rtti":                                      "TODO: call pulumiResourceName",
	"l2-component-component-resource-ref":          "components with resources as inputs/outputs not supported",
	"l2-component-program-resource-ref":            "components with resources as inputs/outputs not supported",
	"l2-resource-secret":                           "#1564 Fix l2-resource-secret",
	"l2-resource-parent-inheritance":               "Fix l2-resource-parent-inheritance",
	"l2-namespaced-provider":                       "components with resources as inputs/outputs not supported",
	"l2-component-property-deps":                   "compilation error",
	"l2-parameterized-resource":                    "compilation error",
	"l2-explicit-providers":                        "should have 6 item(s), but has 7",
	"l2-invoke-scalar":                             "exception at runtime",
	"l3-component-simple":                          "compilation error",
	"l1-builtin-cwd":                               "test failing",
	"l1-builtin-stash":                             "test failing",
	"l1-stack-reference":                           "test failing",
	"l2-invoke-options":                            "test failing",
	"l1-output-map":                                "test failing",
	"l2-resource-keyword-overlap":                  "test failing",
	"policy-simple":                                "test failing",
	"l2-invoke-variants":                           "test failing",
	"l2-union":                                     "test failing",
	"l2-resource-option-import":                    "test failing",
	"l2-resource-option-retain-on-delete":          "test failing",
	"l2-explicit-provider":                         "test failing",
	"l2-resource-option-protect":                   "test failing",
	"l2-engine-update-options":                     "test failing",
	"l2-target-up-with-new-dependency":             "test failing",
	"l1-output-array":                              "test failing",
	"l1-main":                                      "test failing",
	"l1-builtin-project-root-main":                 "test failing",
	"l2-failed-create-continue-on-error":           "test failing",
	"l2-parameterized-resource-twice":              "test failing",
	"policy-dryrun":                                "test failing",
	"policy-config-schema":                         "test failing",
	"l2-resource-asset-archive":                    "test failing",
	"l2-parallel-resources":                        "test failing",
	"l1-builtin-info":                              "test failing",
	"l2-camel-names":                               "test failing",
	"policy-config":                                "test failing",
	"l2-explicit-parameterized-provider":           "test failing",
	"l2-invoke-options-depends-on":                 "test failing",
	"l2-resource-primitives":                       "test failing",
	"l2-resource-simple":                           "test failing",
	"policy-stack-config":                          "test failing",
	"policy-enforcement-config":                    "test failing",
	"policy-remediate":                             "test failing",
	"policy-invalid":                               "https://github.com/pulumi/pulumi-java/issues/1998",
	"l2-primitive-ref":                             "test failing",
	"l2-plain":                                     "test failing",
	"l2-resource-alpha":                            "test failing",
	"l2-failed-create":                             "test failing",
	"l2-resource-config":                           "test failing",
	"l2-keywords":                                  "test failing",
	"l2-large-string":                              "test failing",
	"l2-ref-ref":                                   "test failing",
	"l2-invoke-dependencies":                       "test failing",
	"l2-destroy":                                   "test failing",
	"l2-invoke-scalars":                            "not implemented",
	"l2-invoke-output-only":                        "not implemented",
	"l2-parameterized-invoke":                      "not implemented",
	"l2-resource-option-delete-before-replace":     "https://github.com/pulumi/pulumi-java/issues/1990",
	"l2-resource-option-additional-secret-outputs": "https://github.com/pulumi/pulumi-java/issues/1991",
	"l2-resource-option-replace-on-changes":        "https://github.com/pulumi/pulumi-java/issues/1992",
	"l2-resource-option-custom-timeouts":           "https://github.com/pulumi/pulumi-java/issues/1994",
	"l2-resource-option-version":                   "https://github.com/pulumi/pulumi-java/issues/1995",
	"l2-resource-option-version-sdk":               "https://github.com/pulumi/pulumi-java/issues/1996",
	"l3-range-resource-output-traversal":           "https://github.com/pulumi/pulumi-java/issues/1997",
	"l2-resource-option-plugin-download-url":       "https://github.com/pulumi/pulumi-java/issues/1999",
	"l2-resource-option-env-var-mappings":          "https://github.com/pulumi/pulumi-java/issues/2006",
	"l1-elide-index":                               "https://github.com/pulumi/pulumi-java/issues/2017",
	"l2-elide-index":                               "https://github.com/pulumi/pulumi-java/issues/2016",
	"l2-enum":                                      "https://github.com/pulumi/pulumi-java/issues/2018",
	"l2-module-format":                             "https://github.com/pulumi/pulumi-java/issues/2019",
	"l2-resource-names":                            "https://github.com/pulumi/pulumi-java/issues/2020",
	"l2-discriminated-union":                       "https://github.com/pulumi/pulumi-java/issues/2021",
	"policy-stack-tags":                            "https://github.com/pulumi/pulumi-java/issues/2022",
	"provider-resource-component":                  "not implemented",
	"provider-alias-component":                     "not implemented",
	"provider-replacement-trigger-component":       "https://github.com/pulumi/pulumi-java/issues/2007",
	"provider-depends-on-component":                "https://github.com/pulumi/pulumi-java/issues/2023",
	"provider-ignore-changes-component":            "https://github.com/pulumi/pulumi-java/issues/2024",
	"l1-builtin-base64":                            "TODO: call fromBase64/toBase64 not implemented",
	"l1-config-secret":                             "compilation error: applyValue called on plain String",
	"l1-builtin-object":                            "compilation error: illegal start of expression",
	"l2-builtin-object":                            "compilation error: illegal start of expression",
	"l3-range":                                     "compilation error: KeyedValue.of() expects Iterable, got String",
	"l2-resource-option-replacement-trigger":       "replacementTrigger method not found on CustomResourceOptions.Builder",
	"l1-builtin-list":                              "compilation error: array indexing on String",
	"l2-resource-elide-unknowns":                   "compilation error: array indexing on List and missing .x() method on Map", //nolint:lll
	"l2-external-enum":                             "compilation error: package com.pulumi.enum_.enums does not exist",
	"l1-builtin-secret":                            "TODO: call unsecret not implemented",
	"l1-builtin-require-pulumi-version":            "requirePulumiVersion method not found on DeploymentInstance",
	"l2-resource-name-type":                        "TODO: call pulumiResourceName/pulumiResourceType not implemented",
	"l2-resource-option-replace-with":              "replaceWith method not found on CustomResourceOptions.Builder",
	"l2-resource-option-alias":                     "noParent method not found on Alias.Builder",
}

// runTestingHost boots up a new instance of the language conformance test runner, `pulumi-test-language`, as well as a
// fake Pulumi engine for collecting logs. It returns the address of the fake engine and a connection to the test runner
// that can be used to manage a test suite run.
func runTestingHost(t *testing.T) (string, testingrpc.LanguageTestClient) {
	// We can't just go run the pulumi-test-language package because of
	// https://github.com/golang/go/issues/39172, so we build it to a temp file then run that.
	binary := t.TempDir() + "/pulumi-test-language"
	cmd := exec.Command("go", "build", "-o", binary, "github.com/pulumi/pulumi/pkg/v3/testing/pulumi-test-language") //nolint:gosec,lll
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

	return address, client
}
