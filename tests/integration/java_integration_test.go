// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package integration

import (
	"fmt"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource"
)

func TestIntegrations(t *testing.T) {
	t.Run("stack-reference", func(t *testing.T) {
		dir := filepath.Join(getCwd(t), "stack-reference")
		test := getJvmBase(t, integration.ProgramTestOptions{
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
}

func getJvmBase(t *testing.T, testSpecificOptions integration.ProgramTestOptions) integration.ProgramTestOptions {
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
			return nil // needed because defaultPrepareProject does not know about jvm
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
