// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package examples

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	jtests "github.com/pulumi/pulumi-java/tests/internal"
	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

// For cloud examples that take a very long time to deploy, we only
// run `pulumi preview` skipping the actual `pulumi up` part. This
// verifies that the example compiles and interacts with the Pulumi
// CLI as expected, but does not verify actual cloud interaction.
//
// Later this can be made dynamic to run full version on nightlies.
const previewOnly = true

func TestExamples(t *testing.T) {
	t.Run("random", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/random",
			[]string{"random"},
			integration.ProgramTestOptions{
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					assert.Greater(t, o["randomInteger"].(float64), -0.1)
					assert.Len(t, o["randomString"].(string), 10)
					assert.Len(t, o["randomUuid"].(string), 36)
					assert.Len(t, o["randomIdHex"].(string), 20)

					for _, s := range o["shuffled"].([]interface{}) {
						s := s.(string)
						assert.Contains(t, []string{"A", "B", "C"}, s)
					}

					hasCipherText := false
					for k := range o["randomPassword"].(map[string]interface{}) {
						if k == "ciphertext" {
							hasCipherText = true
						}
					}
					assert.True(t, hasCipherText)
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("azure-java-static-website", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"examples/azure-java-static-website",
			[]string{"azure-native"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location": "westus",
				},
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					cdnEndpoint := o["cdnEndpoint"].(string)
					staticEndpoint := o["staticEndpoint"].(string)
					assert.True(t, strings.HasPrefix(cdnEndpoint, "https"))
					assert.True(t, strings.HasPrefix(staticEndpoint, "https"))
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("aws-java-webserver", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"examples/aws-java-webserver",
			[]string{"aws"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region": "us-east-1",
				},
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					publicIp := o["publicIp"].(string)
					publicHostName := o["publicHostName"].(string)
					assert.True(t, strings.Contains(publicIp, "."))
					assert.True(t, strings.Contains(publicHostName, "."))
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("azure-java-appservice-sql", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"examples/azure-java-appservice-sql",
			[]string{"azure-native"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location":                 "westus",
					"azure-java-appservice-sql:sqlPassword": "not-a-real-password",
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("aws-java-eks-minimal", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"examples/aws-java-eks-minimal",
			[]string{"eks", "aws", "kubernetes"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region": "us-west-1",
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("gcp-java-gke-hello-world", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"examples/gcp-java-gke-hello-world",
			[]string{"gcp"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					// Try `gcloud projects list`
					"gcp:project": "pulumi-ci-gcp-provider",
					"gcp:zone":    "us-west1-a",
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("kubernetes", func(t *testing.T) {
		t.SkipNow()
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/kubernetes",
			[]string{"kubernetes"},
			integration.ProgramTestOptions{
				Config: map[string]string{},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("minimal", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/minimal",
			[]string{}, /*providers*/
			integration.ProgramTestOptions{
				PrepareProject: func(info *engine.Projinfo) error {
					cmd := exec.Command(filepath.Join(info.Root, "mvnw"),
						"--no-transfer-progress", "package")
					cmd.Dir = info.Root
					var buf bytes.Buffer
					cmd.Stdout = &buf
					cmd.Stderr = &buf
					err := cmd.Run()
					if err != nil {
						t.Logf("mvwn --no-transfer-progress package: %v", err)
						t.Log(buf.String())
					}

					return err
				},
				Config: map[string]string{
					"name": "Pulumi",
				},
				Secrets: map[string]string{
					"secret": "this is my secret message",
				},
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					// Simple runtime validation that just ensures the checkpoint was written and read.
					assert.NotNil(t, stackInfo.Deployment)
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("minimalsbt", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/minimalsbt",
			[]string{}, /*providers*/
			integration.ProgramTestOptions{
				Config: map[string]string{
					"name": "Pulumi",
				},
				Secrets: map[string]string{
					"secret": "this is my secret message",
				},
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					// Simple runtime validation that just ensures the checkpoint was written and read.
					assert.NotNil(t, stackInfo.Deployment)
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("aws-native-java-s3-folder", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"tests/examples/aws-native-java-s3-folder",
			[]string{"aws", "aws-native"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region":        "us-west-2",
					"aws-native:region": "us-west-2",
				},

				// TODO failing here, potentially a
				// provider bug. We need to recheck
				// after upgrading to latest.
				SkipRefresh: true,
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("azure-java-function-graal-spring", func(t *testing.T) {
		test := makeJavaProgramTestOptions(t,
			"examples/azure-java-function-graal-spring",
			[]string{"azure-native"},
			integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location": "westus",
				},
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					functionName := o["functionName"].(string)
					endpoint := o["endpoint"].(string)
					assert.True(t, len(functionName) > 0)
					assert.True(t, strings.HasPrefix(endpoint, "https"))
					assertHTTPResult(t, o["endpoint"], nil, func(body string) bool {
						return assert.Contains(t, body, "{\"message\":\"Hello from Spring, Pulumi!\"}")
					})
				},
				PrepareProject: func(info *engine.Projinfo) error {
					cmd := exec.Command("gradle", "app:packageDistribution")
					cmd.Dir = info.Root
					var buf bytes.Buffer
					cmd.Stdout = &buf
					cmd.Stderr = &buf
					err := cmd.Run()
					if err != nil {
						t.Logf("gradle app:packageDistribution: %v", err)
						t.Log(buf.String())
					}
					return err
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("minimal-jbang", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/minimal-jbang",
			[]string{}, /*providers*/
			integration.ProgramTestOptions{
				Config: map[string]string{
					"minimal:name":   "minimal-name",
					"minimal:secret": "minimal-secret",
				},
			},
		)

		integration.ProgramTest(t, &test)
	})

	t.Run("testing-unit-java", func(t *testing.T) {
		test := makeJavaProgramTestOptions(
			t,
			"tests/examples/testing-unit-java",
			[]string{}, /*providers*/
			integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region": "us-east-2",
				},
			},
		)

		integration.ProgramTest(t, &test)
	})
}

// Constructs a set of integration.ProgramTestOptions for running a Java example test. The supplied directory will be
// resolved relative to the repository root, and is typically one of the following:
//
//   - tests/examples/<example> if the example being tested is vendored into this repository.
//   - examples/<example> if the example being tested is in the Pulumi examples repository, which is cloned into this one
//     as part of running this test suite.
func makeJavaProgramTestOptions(
	t *testing.T,
	dir string,
	providers []string,
	overrides integration.ProgramTestOptions,
) integration.ProgramTestOptions {
	repoRoot, err := filepath.Abs(filepath.Join("..", ".."))
	if err != nil {
		panic(err)
	}

	prepareProject := func(info *engine.Projinfo) error {
		if len(providers) == 0 {
			return nil
		}

		deps, err := jtests.ParsePinVersionsFromEnv(t, providers)
		if err != nil {
			return err
		}
		_, err = jtests.Pin(info.Root, deps)
		return err
	}

	opts := integration.ProgramTestOptions{Dir: filepath.Join(repoRoot, dir)}.
		With(getBaseOptions()).
		With(overrides).
		With(integration.ProgramTestOptions{
			PrepareProject: combinePrepareProject(
				prepareProject,
				overrides.PrepareProject,
			),
		})

	if previewOnly {
		opts = opts.With(integration.ProgramTestOptions{
			SkipRefresh:            true,
			SkipEmptyPreviewUpdate: true,
			SkipExportImport:       true,
			SkipUpdate:             true,
		})
		opts.ExtraRuntimeValidation = nil
	}

	return opts
}

func combinePrepareProject(f1, f2 func(info *engine.Projinfo) error) func(info *engine.Projinfo) error {
	return func(info *engine.Projinfo) error {
		if f1 != nil {
			if err := f1(info); err != nil {
				return err
			}
		}
		if f2 != nil {
			return f2(info)
		}
		return nil
	}
}

// Copied from: https://github.com/pulumi/examples/blob/4fb1f146409ace4af1945f84ee9c90c643430e9d/misc/test/examples_test.go

func assertHTTPResult(t *testing.T, output interface{}, headers map[string]string, check func(string) bool) bool {
	return assertHTTPResultWithRetry(t, output, headers, 5*time.Minute, check)
}

func assertHTTPResultWithRetry(t *testing.T, output interface{}, headers map[string]string, maxWait time.Duration, check func(string) bool) bool {
	return assertHTTPResultShapeWithRetry(t, output, headers, maxWait, func(string) bool { return true }, check)
}

func assertHTTPResultShapeWithRetry(t *testing.T, output interface{}, headers map[string]string, maxWait time.Duration,
	ready func(string) bool, check func(string) bool,
) bool {
	hostname, ok := output.(string)
	if !assert.True(t, ok, fmt.Sprintf("expected `%s` output", output)) {
		return false
	}

	if !(strings.HasPrefix(hostname, "http://") || strings.HasPrefix(hostname, "https://")) {
		hostname = fmt.Sprintf("http://%s", hostname)
	}

	startTime := time.Now()
	count, sleep := 0, 0
	for true {
		now := time.Now()
		req, err := http.NewRequest("GET", hostname, nil)
		if !assert.NoError(t, err) {
			return false
		}

		for k, v := range headers {
			// Host header cannot be set via req.Header.Set(), and must be set
			// directly.
			if strings.ToLower(k) == "host" {
				req.Host = v
				continue
			}
			req.Header.Set(k, v)
		}

		client := &http.Client{Timeout: time.Second * 10}
		resp, err := client.Do(req)
		if err == nil && resp.StatusCode == 200 {
			if !assert.NotNil(t, resp.Body, "resp.body was nil") {
				return false
			}

			// Read the body
			defer resp.Body.Close()
			body, err := ioutil.ReadAll(resp.Body)
			if !assert.NoError(t, err) {
				return false
			}

			bodyText := string(body)

			// Even if we got 200 and a response, it may not be ready for assertion yet - that's specific per test.
			if ready(bodyText) {
				// Verify it matches expectations
				return check(bodyText)
			}
		}
		if now.Sub(startTime) >= maxWait {
			fmt.Printf("Timeout after %v. Unable to http.get %v successfully.", maxWait, hostname)
			return false
		}
		count++
		// delay 10s, 20s, then 30s and stay at 30s
		if sleep > 30 {
			sleep = 30
		} else {
			sleep += 10
		}
		time.Sleep(time.Duration(sleep) * time.Second)
		fmt.Printf("Http Error: %v\n", err)
		fmt.Printf("  Retry: %v, elapsed wait: %v, max wait %v\n", count, now.Sub(startTime), maxWait)
	}

	return false
}
