// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package examples

import (
	"path/filepath"
	"strings"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

func TestJavaAccMinimal(t *testing.T) {
	test := getJvmBase(t, "minimal").
		With(integration.ProgramTestOptions{
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
}

func TestJavaRandomProvider(t *testing.T) {
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
}

func TestCloudExamples(t *testing.T) {
	t.Run("azure-java-static-website", func(t *testing.T) {
		// Skipping as the example uses 340+s; in addition it
		// may require additional CI setup to access an Azure
		// account.
		t.Skip("Too slow")
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
		// Skipping as the example uses 340+s; in addition it
		// may require additional CI setup to access an Azure
		// account.
		t.Skip("Too slow")
		test := getJvmBase(t, "azure-java-appservice-sql").
			With(integration.ProgramTestOptions{
				Config: map[string]string{
					"azure-native:location": "westus",
				},
				Quick: true,
				ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
					o := stackInfo.Outputs
					endpoint := o["endpoint"].(string)
					assert.True(t, strings.HasPrefix(endpoint, "https"))
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
