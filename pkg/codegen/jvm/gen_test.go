// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

var tests []test.SDKTest = []test.SDKTest{
	{
		Directory:   "simple-resource-schema",
		Description: "Simple schema with local resource properties",
	},
	{
		Directory:   "simple-enum-schema",
		Description: "Simple schema with enum types",
	},
	{
		Directory:   "external-resource-schema",
		Description: "External resource schema",
		Skip:        codegen.NewStringSet("jvm/any"), // TODO[pulumi/pulumi-java#13]
	},
	{
		Directory:   "simple-plain-schema",
		Description: "Simple schema with plain properties",
	},
	{
		Directory:   "mini-azurenative",
		Description: "Regression tests extracted from trying to codegen azure-natuve",
	},
}

func compileGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
}

func testGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build")
}

func TestGeneratePackage(t *testing.T) {
	test.TestSDKCodegen(t, &test.SDKCodegenOptions{
		GenPackage: GeneratePackage,
		Language:   "jvm",
		TestCases:  tests,
		Checks: map[string]test.CodegenCheck{
			"jvm/compile": compileGeneratedPackage,
			"jvm/test":    testGeneratedPackage,
		},
	})

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
