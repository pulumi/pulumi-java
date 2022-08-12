// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package integration

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource"
	"github.com/pulumi/pulumi/sdk/v3/go/common/tokens"
)

func TestIntegrations(t *testing.T) {
	t.Run("stack-reference", func(t *testing.T) {
		dir := filepath.Join(getCwd(t), "stack-reference")
		test := getJavaBase(t, integration.ProgramTestOptions{
			Dir:           dir,
			Quick:         true,
			DebugUpdates:  false,
			DebugLogLevel: 0,
			Env: []string{
				"PULUMI_EXCESSIVE_DEBUG_OUTPUT=false",
			},
			ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
				rawVal := stackInfo.Outputs["val"]
				val, isArray := rawVal.([]interface{})
				assert.Truef(t, isArray, "output 'val' was not serialized as an array, got %T", rawVal)
				assert.Equal(t, 2, len(val))
				assert.Equal(t, "a", val[0])
				assert.Equal(t, "b", val[1])
			},
			EditDirs: []integration.EditDir{
				{
					Dir:      filepath.Join(dir, "step1"),
					Additive: true,
					Verbose:  true,
					ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
						rawVal2 := stackInfo.Outputs["val2"]
						val2, isMap := rawVal2.(map[string]interface{}) // it is a secret
						assert.Truef(t, isMap, "secret output 'val2' was not serialized as a map, got %T", rawVal2)
						assert.Equal(t, resource.SecretSig, val2[resource.SigKey].(string))
						_, ok := val2["ciphertext"]
						assert.Truef(t, ok, "secret output value 'val2[\"ciphertext\"]' is missing, got: %v", val2)
					},
				},
				{
					Dir:      filepath.Join(dir, "step2"),
					Additive: true,
					Verbose:  true,
					ExtraRuntimeValidation: func(t *testing.T, stackInfo integration.RuntimeValidationStackInfo) {
						rawVal := stackInfo.Outputs["gotExpectedError"]
						gotExpectedError, isBool := rawVal.(bool)
						assert.Truef(t, isBool, "output 'gotExpectedError' was not serialized as a bool, got %T", rawVal)
						assert.True(t, gotExpectedError)
					},
				},
			},
		})
		integration.ProgramTest(t, &test)
	})
	t.Run("stack-transformation", func(t *testing.T) {
		dir := filepath.Join(getCwd(t), "stack-transformation")
		test := getJavaBase(t, integration.ProgramTestOptions{
			Dir:                    dir,
			Quick:                  true,
			DebugUpdates:           false,
			DebugLogLevel:          0,
			ExtraRuntimeValidation: stackTransformationValidator(),
		})
		integration.ProgramTest(t, &test)
	})
	t.Run("convert", func(t *testing.T) {
		t.Skip("TODO re-enable once pulumi CLI ships needed GenerateProject changes")

		convertedDir := t.TempDir()
		dir := filepath.Join(getCwd(t), "convert")
		pulumi, err := exec.LookPath("pulumi")
		require.NoError(t, err)
		err = integration.RunCommand(t, "pulumi convert ...", []string{
			pulumi, "convert", "--language", "java",
			"--out", convertedDir,
		}, dir, &integration.ProgramTestOptions{})
		require.NoError(t, err)
		test := getJavaBase(t, integration.ProgramTestOptions{
			Dir: convertedDir,
			Config: map[string]string{
				"azure:location": "westus2",
			},
			SkipRefresh:            true,
			SkipEmptyPreviewUpdate: true,
			SkipExportImport:       true,
			SkipUpdate:             true,
		})
		integration.ProgramTest(t, &test)
	})
}

func getJavaBase(t *testing.T, testSpecificOptions integration.ProgramTestOptions) integration.ProgramTestOptions {
	repoRoot, err := filepath.Abs(filepath.Join("..", ".."))
	if err != nil {
		panic(err)
	}
	opts := integration.ProgramTestOptions{
		Env: []string{fmt.Sprintf("PULUMI_REPO_ROOT=%s", repoRoot)},
		Config: map[string]string{
			"org": "pulumi-bot",
		},
		PrepareProject: func(*engine.Projinfo) error {
			return nil // needed because defaultPrepareProject does not know about java
		},
	}
	opts = opts.With(testSpecificOptions)

	// local environment, to run locally offline, make sure you set:
	// export PULUMI_BACKEND_URL=file://~
	// export PULUMI_API=file://~
	// pulumi login --local
	pulumiAPI, ok := os.LookupEnv("PULUMI_API")
	var isAPILocal = ok && pulumiAPI == "file://~"
	pulumiBackend, ok := os.LookupEnv("PULUMI_BACKEND_URL")
	var isBackendLocal = ok && pulumiBackend == "file://~"

	var isLocal = isAPILocal || isBackendLocal
	if isLocal {
		t.Log("Running test locally")
		opts = opts.With(integration.ProgramTestOptions{
			Config: map[string]string{
				"local": "true",
			},
			CloudURL: "file://~",
		})
	}
	t.Logf("Running test with opts.CloudURL: %s", opts.CloudURL)
	return opts
}

func stackTransformationValidator() func(t *testing.T, stack integration.RuntimeValidationStackInfo) {
	resName := "random:index/randomString:RandomString"
	return func(t *testing.T, stack integration.RuntimeValidationStackInfo) {
		foundRes1 := false
		foundRes2Child := false
		foundRes3 := false
		foundRes4Child := false
		foundRes5Child := false
		for _, res := range stack.Deployment.Resources {
			// "res1" has a transformation which adds additionalSecretOutputs
			if res.URN.Name() == "res1" {
				foundRes1 = true
				assert.Equal(t, res.Type, tokens.Type(resName))
				assert.Contains(t, res.AdditionalSecretOutputs, resource.PropertyKey("length"))
			}
			// "res2" has a transformation which adds additionalSecretOutputs to it's
			// "child" and sets minUpper to 2
			if res.URN.Name() == "res2-child" {
				foundRes2Child = true
				assert.Equal(t, res.Type, tokens.Type(resName))
				assert.Equal(t, res.Parent.Type(), tokens.Type("my:component:MyComponent"))
				assert.Contains(t, res.AdditionalSecretOutputs, resource.PropertyKey("length"))
				assert.Contains(t, res.AdditionalSecretOutputs, resource.PropertyKey("special"))
				minUpper := res.Inputs["minUpper"]
				assert.NotNil(t, minUpper)
				assert.Equal(t, 2.0, minUpper.(float64))
			}
			// "res3" is impacted by a global stack transformation which sets
			// overrideSpecial to "stackvalue"
			if res.URN.Name() == "res3" {
				foundRes3 = true
				assert.Equal(t, res.Type, tokens.Type(resName))
				overrideSpecial := res.Inputs["overrideSpecial"]
				assert.NotNil(t, overrideSpecial)
				assert.Equal(t, "stackvalue", overrideSpecial.(string))
			}
			// "res4" is impacted by two component parent transformations which appends
			// to overrideSpecial "value1" and then "value2" and also a global stack
			// transformation which appends "stackvalue" to overrideSpecial.  The end
			// result should be "value1value2stackvalue".
			if res.URN.Name() == "res4-child" {
				foundRes4Child = true
				assert.Equal(t, res.Type, tokens.Type(resName))
				assert.Equal(t, res.Parent.Type(), tokens.Type("my:component:MyComponent"))
				overrideSpecial := res.Inputs["overrideSpecial"]
				assert.NotNil(t, overrideSpecial)
				assert.Equal(t, "value1value2stackvalue", overrideSpecial.(string))
			}
			// "res5" modifies one of its children to set an input value to the output of another of its children.
			if res.URN.Name() == "res5-child1" {
				foundRes5Child = true
				assert.Equal(t, res.Type, tokens.Type(resName))
				assert.Equal(t, res.Parent.Type(), tokens.Type("my:component:MyComponent"))
				length := res.Inputs["length"]
				assert.NotNil(t, length)
				assert.Equal(t, 6.0, length.(float64))
			}
		}
		assert.True(t, foundRes1)
		assert.True(t, foundRes2Child)
		assert.True(t, foundRes3)
		assert.True(t, foundRes4Child)
		assert.True(t, foundRes5Child)
	}
}
