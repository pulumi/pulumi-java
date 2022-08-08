// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package integration

import (
	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	"os"
	"testing"
)

func getCwd(t *testing.T) string {
	cwd, err := os.Getwd()
	if err != nil {
		t.FailNow()
	}
	return cwd
}

func getJavaBase(t *testing.T, testSpecificOptions integration.ProgramTestOptions) integration.ProgramTestOptions {
	opts := integration.ProgramTestOptions{
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
