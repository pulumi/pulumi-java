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
