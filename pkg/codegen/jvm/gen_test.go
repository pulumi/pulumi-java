// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"os"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

func TestGeneratePackage(t *testing.T) {
	// pre set-up pulumi/pulumi submodule
	test.RunCommand(t, "git", "../../..",
		"git",
		"submodule",
		"update",
		"--init",
		"--recursive",
	)
	test.RunCommand(t, "git", "../../../pulumi",
		"git",
		"pull",
	)
	compileGeneratedPackage := func(t *testing.T, pwd string) {
		test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
	}

	testGeneratedPackage := func(t *testing.T, pwd string) {
		test.RunCommand(t, "gradle build", pwd, "gradle", "build")
	}

	os.Setenv("PULUMI_ACCEPT", "yes") // skips manifest test

	test.TestSDKCodegen(t, &test.SDKCodegenOptions{
		GenPackage: GeneratePackage,
		Language:   "jvm",
		TestCases:  test.PulumiPulumiSDKTests,
		Checks: map[string]test.CodegenCheck{
			"jvm/compile": compileGeneratedPackage,
			"jvm/test":    testGeneratedPackage,
		},
	})
}
