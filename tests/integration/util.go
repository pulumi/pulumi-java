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
	return opts
}
