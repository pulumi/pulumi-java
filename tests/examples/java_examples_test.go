// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package examples

import (
	"path/filepath"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

func TestJavaAccMinimal(t *testing.T) {
	test := getBaseOptions().
		With(integration.ProgramTestOptions{
			Dir: filepath.Join(getCwd(t), "minimal"),
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
			PrepareProject: func(*engine.Projinfo) error {
				return nil
			},
		})

	integration.ProgramTest(t, &test)
}
