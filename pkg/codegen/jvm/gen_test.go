// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

var javaSpecificTests []test.SDKTest = []test.SDKTest{
	{
		Directory:   "mini-azurenative",
		Description: "Regression tests extracted from trying to codegen azure-natuve",
	},
}

func adaptTest(t test.SDKTest) test.SDKTest {
	switch t.Directory {
	case "external-resource-schema":
		// TODO[pulumi/pulumi-java#13]
		t.Skip = codegen.NewStringSet("jvm/any")
	case "plain-schema-gh6957":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "simple-plain-schema":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "simple-methods-schema":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "provider-config-schema":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "simple-methods-schema-single-value-returns":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "hyphen-url":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "plain-object-defaults":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "output-funcs":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "regress-8403":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "plain-object-disable-defaults":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "regress-node-8110":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "different-package-name-conflict":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "different-enum":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "regress-go-8664":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "external-node-compatibility":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "external-go-import-aliases":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	case "external-python-same-module-name":
		t.Skip = codegen.NewStringSet("jvm/any") // TODO
	}
	return t
}

func testCases() []test.SDKTest {
	var ts []test.SDKTest
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
		GenPackage: GeneratePackage,
		Language:   "jvm",
		TestCases:  testCases(),
		Checks: map[string]test.CodegenCheck{
			"jvm/compile": compileGeneratedPackage,
			"jvm/test":    testGeneratedPackage,
		},
	})
}
