// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

var javaSpecificTests = []*test.SDKTest{
	{
		Directory:   "mini-azurenative",
		Description: "Regression tests extracted from trying to codegen azure-natuve",
	},
	{
		Directory:   "mini-awsnative",
		Description: "Regression tests extracted from trying to codegen aws-native",
	},
	{
		Directory:   "mini-awsclassic",
		Description: "Regression tests extracted from trying to codegen aws",
	},
	{
		Directory:   "mini-kubernetes",
		Description: "Regression tests extracted from trying to codegen kubernetes",
	},
	{
		Directory:   "mini-azuread",
		Description: "Regression tests extracted from trying to codegen azuread",
	},
	{
		Directory:   "mini-awsx",
		Description: "Regression tests extracted from trying to codegen awsx",
	},
	{
		Directory:   "output-funcs-edgeorder",
		Description: "Testing EdgeOrder functions which return Output<T>",
	},
}

func adaptTest(t *test.SDKTest) *test.SDKTest {
	switch t.Directory {
	case "external-resource-schema":
		// TODO[pulumi/pulumi-java#13]
		t.Skip = codegen.NewStringSet("java/any")
	case "plain-schema-gh6957":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "simple-methods-schema":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "provider-config-schema":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "simple-methods-schema-single-value-returns":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "hyphen-url":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "plain-object-defaults":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "regress-8403":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "plain-object-disable-defaults":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "regress-node-8110":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "different-package-name-conflict":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "different-enum":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "regress-go-8664":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "external-node-compatibility":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "external-go-import-aliases":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "external-python-same-module-name":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "internal-dependencies-go":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "go-plain-ref-repro":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	}
	return t
}

func testCases() []*test.SDKTest {
	var ts []*test.SDKTest
	ts = append(ts, javaSpecificTests...)
	for _, t := range test.PulumiPulumiSDKTests {
		ts = append(ts, adaptTest(t))
	}
	return ts
}

func compileGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
}

func testGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build")
}

func TestGeneratePackage(t *testing.T) {
	test.TestSDKCodegen(t, &test.SDKCodegenOptions{
		GenPackage: generatePackage,
		Language:   "java",
		TestCases:  testCases(),
		Checks: map[string]test.CodegenCheck{
			"java/compile": compileGeneratedPackage,
			"java/test":    testGeneratedPackage,
		},
	})
}

func generatePackage(tool string, pkg *schema.Package, extraFiles map[string][]byte) (map[string][]byte, error) {
	pkgInfo := PackageInfo{
		BuildFiles: "gradle",
		Packages: map[string]string{
			"com.pulumi:pulumi": "0.0.1",

			// There are some sprinkled unit tests that
			// complement testing the generated code at
			// runtime, and this is the union of all their
			// dependencies. It would be more precise to
			// move the dependencies into the schema for
			// the individual projects that happen to need
			// them.
			"com.google.guava:guava":                 "30.1-jre",
			"com.google.protobuf:protobuf-java":      "3.12.0",
			"com.google.protobuf:protobuf-java-util": "3.12.0",
			"org.assertj:assertj-core":               "3.20.2",
			"org.junit.jupiter:junit-jupiter-api":    "5.7.2",
			"org.mockito:mockito-core":               "3.12.4",
		},
	}
	pkg.Language = map[string]interface{}{
		"java": pkgInfo,
	}
	return GeneratePackage(tool, pkg, extraFiles)
}
