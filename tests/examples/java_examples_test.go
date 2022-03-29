// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package examples

import (
	"os/exec"
	"path/filepath"
	"strings"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

func TestExamples(t *testing.T) {
	t.Run("random", func(t *testing.T) {
		test := getJvmBase(t, "random").
			With(integration.ProgramTestOptions{
				Quick: true,
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
			})

		integration.ProgramTest(t, &test)

	})

	t.Run("azure-java-static-website", func(t *testing.T) {
		test := getJvmBase(t, "azure-java-static-website").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location": "westus",
				},
				Quick: true,
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					cdnEndpoint := o["cdnEndpoint"].(string)
					staticEndpoint := o["staticEndpoint"].(string)
					assert.True(t, strings.HasPrefix(cdnEndpoint, "https"))
					assert.True(t, strings.HasPrefix(staticEndpoint, "https"))
				},
			})
		integration.ProgramTest(t, &test)
	})

	t.Run("aws-java-webserver", func(t *testing.T) {
		test := getJvmBase(t, "aws-java-webserver").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region": "us-east-1",
				},
				Quick: true,
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					publicIp := o["publicIp"].(string)
					publicHostName := o["publicHostName"].(string)
					assert.True(t, strings.Contains(publicIp, "."))
					assert.True(t, strings.Contains(publicHostName, "."))
				},
			})
		integration.ProgramTest(t, &test)
	})

	t.Run("azure-java-appservice-sql", func(t *testing.T) {
		test := previewOnlyJvmBase(t, "azure-java-appservice-sql").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location":                 "westus",
					"azure-java-appservice-sql:sqlPassword": "not-a-real-password",
				},
			})
		integration.ProgramTest(t, &test)
	})

	t.Run("eks-minimal", func(t *testing.T) {
		test := previewOnlyJvmBase(t, "eks-minimal").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region": "us-west-1",
				},
			})
		integration.ProgramTest(t, &test)
	})

	t.Run("gcp-java-gke-hello-world", func(t *testing.T) {
		test := previewOnlyJvmBase(t, "gcp-java-gke-hello-world").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					// Try `gcloud projects list`
					"gcp:project": "pulumi-development",
					"gcp:zone":    "us-west1-a",
				},
			})

		integration.ProgramTest(t, &test)
	})

	t.Run("minimal", func(t *testing.T) {
		test := getJvmBase(t, "minimal").
			With(integration.ProgramTestOptions{
				PrepareProject: func(info *engine.Projinfo) error {
					cmd := exec.Command(filepath.Join(info.Root, "mvnw"),
						"--no-transfer-progress", "package")
					cmd.Dir = info.Root
					return cmd.Run()
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
			})
		integration.ProgramTest(t, &test)
	})

	t.Run("aws-native-java-s3-folder", func(t *testing.T) {
		test := getJvmBase(t, "aws-native-java-s3-folder").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"aws:region":        "us-west-2",
					"aws-native:region": "us-west-2",
				},

				// TODO failing here, potentially a
				// provider bug. We need to recheck
				// after upgrading to latest.
				SkipRefresh: true,
			})
		integration.ProgramTest(t, &test)
	})
}

func getJvmBase(t *testing.T, dir string) integration.ProgramTestOptions {
	return getBaseOptions().
		With(integration.ProgramTestOptions{
			Dir: filepath.Join(getCwd(t), dir),
			PrepareProject: func(*engine.Projinfo) error {
				return nil
			},
		})
}

// For cloud examples that take a very long time to deploy, we only
// run `pulumi preview` skipping the actual `pulumi up` part. This
// verifies that the example compiles and interacts with the Pulumi
// CLI as expected, but does not verify actual cloud interaction.
func previewOnlyJvmBase(t *testing.T, dir string) integration.ProgramTestOptions {
	return getJvmBase(t, dir).With(integration.ProgramTestOptions{
		SkipRefresh:            true,
		SkipEmptyPreviewUpdate: true,
		SkipExportImport:       true,
		SkipUpdate:             true,
	})
}
