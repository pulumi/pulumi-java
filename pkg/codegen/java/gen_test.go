// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

// Test case configuration for a GeneratePackage test.
type generatePackageTestConfig struct {
	packageInfo     PackageInfo
	sdkTest         *test.SDKTest
	hasRuntimeTests bool
}

// Configures tests that are maintained in the pulumi-java repository
// specifially for Java codegen testing.
func javaSpecificTests() []generatePackageTestConfig {
	return []generatePackageTestConfig{
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-azurenative",
			Description: "Regression tests extracted from trying to codegen azure-natuve",
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-awsnative",
			Description: "Regression tests extracted from trying to codegen aws-native",
		}),
		newGeneratePackageTestConfigWithExtras(&test.SDKTest{
			Directory:   "mini-awsclassic",
			Description: "Regression tests extracted from trying to codegen aws",
		}, &PackageInfo{
			Dependencies: map[string]string{
				"com.google.protobuf:protobuf-java":      "3.12.0",
				"com.google.protobuf:protobuf-java-util": "3.12.0",
				"org.assertj:assertj-core":               "3.20.2",
				"org.junit.jupiter:junit-jupiter-api":    "5.7.2",
			},
		}),
		newGeneratePackageTestConfigWithExtras(&test.SDKTest{
			Directory:   "mini-kubernetes",
			Description: "Regression tests extracted from trying to codegen kubernetes",
		}, &PackageInfo{
			Dependencies: map[string]string{
				"com.google.guava:guava":              "30.1-jre",
				"org.mockito:mockito-core":            "3.12.4",
				"org.assertj:assertj-core":            "3.20.2",
				"org.junit.jupiter:junit-jupiter-api": "5.7.2",
			},
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-azuread",
			Description: "Regression tests extracted from trying to codegen azuread",
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-awsx",
			Description: "Regression tests extracted from trying to codegen awsx",
		}),
		newGeneratePackageTestConfigWithExtras(&test.SDKTest{
			Directory:   "jumbo-resources",
			Description: "Testing resources with more than 255 properties",
		}, &PackageInfo{
			Dependencies: map[string]string{
				"org.assertj:assertj-core":            "3.20.2",
				"org.junit.jupiter:junit-jupiter-api": "5.7.2",
				"org.mockito:mockito-core":            "3.12.4",
			},
		}),
	}
}

// Configures how language-agnostic test cases from pulumi/pulumi
// repository are adapted to test Java, which tests are skipped, etc..
func adaptTest(t *test.SDKTest) generatePackageTestConfig {
	hasExtras := false
	pkgInfo := PackageInfo{}
	switch t.Directory {
	case "simple-plain-schema":
		hasExtras = true
		pkgInfo = PackageInfo{
			Dependencies: map[string]string{
				"com.google.guava:guava":              "30.1-jre",
				"org.assertj:assertj-core":            "3.20.2",
				"org.junit.jupiter:junit-jupiter-api": "5.7.2",
				"org.mockito:mockito-core":            "3.12.4",
			},
		}
	case "simple-enum-schema":
		hasExtras = true
		pkgInfo = PackageInfo{
			Dependencies: map[string]string{
				"com.google.guava:guava":              "30.1-jre",
				"org.assertj:assertj-core":            "3.20.2",
				"org.junit.jupiter:junit-jupiter-api": "5.7.2",
				"org.mockito:mockito-core":            "3.12.4",
			},
		}
	case "external-resource-schema":
		// TODO[pulumi/pulumi-java#13]
		t.Skip = codegen.NewStringSet("java/any")
		hasExtras = true
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
	case "enum-reference", "enum-reference-python":
		t.Skip = codegen.NewStringSet("java/any") // python only
	case "go-nested-collections":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	}

	if hasExtras {
		return newGeneratePackageTestConfigWithExtras(t, &pkgInfo)
	}

	return newGeneratePackageTestConfig(t)
}

func TestGeneratePackage(t *testing.T) {
	for _, testCase := range testCases() {
		testCase := testCase
		checks := map[string]test.CodegenCheck{
			"java/compile": compileGeneratedPackage,
		}
		if testCase.hasRuntimeTests {
			checks["java/test"] = testGeneratedPackage
		}
		test.TestSDKCodegen(t, &test.SDKCodegenOptions{
			GenPackage: func(tool string,
				pkg *schema.Package,
				extraFiles map[string][]byte) (map[string][]byte, error) {
				pkg.Language = map[string]interface{}{
					"java": testCase.packageInfo,
				}
				return GeneratePackage(tool, pkg, extraFiles)
			},
			Language:  "java",
			TestCases: []*test.SDKTest{testCase.sdkTest},
			Checks:    checks,
		})
	}
}

// Minimal test config that verifies code generation and compilation.
func newGeneratePackageTestConfig(test *test.SDKTest) generatePackageTestConfig {
	packageInfo := PackageInfo{
		BuildFiles: "gradle",
		Dependencies: map[string]string{
			"com.pulumi:pulumi": "0.0.1",
		},
	}.WithDefaultDependencies()
	return generatePackageTestConfig{
		sdkTest:     test,
		packageInfo: packageInfo,
	}
}

// For codegen tests that populate java-extras dir with JUnit tests to
// run on top of the generated code.
func newGeneratePackageTestConfigWithExtras(test *test.SDKTest, info *PackageInfo) generatePackageTestConfig {
	var packageInfo PackageInfo
	if info != nil {
		packageInfo = *info
	}
	packageInfo = packageInfo.With(PackageInfo{
		BuildFiles: "gradle",
		GradleTest: "JUnitPlatform",
		Dependencies: map[string]string{
			"com.pulumi:pulumi":                      "0.0.1",
			"org.junit.jupiter:junit-jupiter-engine": "5.9.0",
		},
	}).WithDefaultDependencies()
	return generatePackageTestConfig{
		sdkTest:         test,
		packageInfo:     packageInfo,
		hasRuntimeTests: true,
	}
}

func testCases() []generatePackageTestConfig {
	var ts []generatePackageTestConfig
	ts = append(ts, javaSpecificTests()...)
	for _, t := range test.PulumiPulumiSDKTests {
		ts = append(ts, adaptTest(t))
	}
	return ts
}

func compileGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
}

func testGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "test")
}
