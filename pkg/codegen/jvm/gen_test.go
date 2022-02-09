// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"os"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

func TestGeneratePackage(t *testing.T) {
	// pre set-up
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
	os.Setenv("PULUMI_ACCEPT", "yes") // skips
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
