package jvm

import (
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen"

	"github.com/pulumi/pulumi-java/pkg/codegen/internal/test"
)

var tests []test.SdkTest = []test.SdkTest{
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
}

func TestGeneratePackage(t *testing.T) {
	compileGeneratedPackage := func(t *testing.T, pwd string) {
		test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
	}

	testGeneratedPackage := func(t *testing.T, pwd string) {
		test.RunCommand(t, "gradle build", pwd, "gradle", "build")
	}

	test.TestSDKCodegen(t, &test.SDKCodegenOptions{
		GenPackage: GeneratePackage,
		Language:   "jvm",
		SdkTests:   tests,
		Checks: map[string]test.CodegenCheck{
			"jvm/compile": compileGeneratedPackage,
			"jvm/test":    testGeneratedPackage,
		},
	})
}
