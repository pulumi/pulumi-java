// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	"encoding/json"
	"os/exec"
	"path/filepath"
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
// specifically for Java codegen testing.
func javaSpecificTests(keyDeps map[string]string) []generatePackageTestConfig {
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
			Dependencies: makeDeps(keyDeps,
				"com.google.protobuf:protobuf-java",
				"com.google.protobuf:protobuf-java-util",
				"org.assertj:assertj-core",
				"org.junit.jupiter:junit-jupiter-api",
			),
		}),
		newGeneratePackageTestConfigWithExtras(&test.SDKTest{
			Directory:   "mini-kubernetes",
			Description: "Regression tests extracted from trying to codegen kubernetes",
		}, &PackageInfo{
			Dependencies: makeDeps(keyDeps,
				"com.google.guava:guava",
				"org.mockito:mockito-core",
				"org.assertj:assertj-core",
				"org.junit.jupiter:junit-jupiter-api",
			),
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-azuread",
			Description: "Regression tests extracted from trying to codegen azuread",
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "mini-awsx",
			Description: "Regression tests extracted from trying to codegen awsx",
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "akamai",
			Description: "Regression tests extracted from trying to codegen akamai",
		}),
		newGeneratePackageTestConfigWithExtras(&test.SDKTest{
			Directory:   "jumbo-resources",
			Description: "Testing resources with more than 255 properties",
		}, &PackageInfo{
			Dependencies: makeDeps(keyDeps,
				"org.assertj:assertj-core",
				"org.junit.jupiter:junit-jupiter-api",
				"org.mockito:mockito-core",
			),
		}),
		newGeneratePackageTestConfig(&test.SDKTest{
			Directory:   "parameterized",
			Description: "Tests for parameterized providers",
		}),
	}
}

// Configures how language-agnostic test cases from pulumi/pulumi
// repository are adapted to test Java, which tests are skipped, etc..
func adaptTest(t *test.SDKTest, keyDeps map[string]string) generatePackageTestConfig {
	hasExtras := false
	pkgInfo := PackageInfo{}
	switch t.Directory {
	case "simple-plain-schema":
		hasExtras = true
		pkgInfo = PackageInfo{
			Dependencies: makeDeps(keyDeps,
				"com.google.guava:guava",
				"org.assertj:assertj-core",
				"org.junit.jupiter:junit-jupiter-api",
				"org.mockito:mockito-core",
			),
		}
	case "simple-enum-schema":
		hasExtras = true
		pkgInfo = PackageInfo{
			Dependencies: makeDeps(keyDeps,
				"com.google.guava:guava",
				"org.assertj:assertj-core",
				"org.junit.jupiter:junit-jupiter-api",
				"org.mockito:mockito-core",
			),
		}
	case "external-resource-schema":
		// TODO[pulumi/pulumi-java#13]
		t.Skip = codegen.NewStringSet("java/any")
		hasExtras = true
	case "plain-schema-gh6957":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "simple-methods-schema":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "provider-type-schema":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "provider-config-schema":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "simple-methods-schema-single-value-returns":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "simplified-invokes":
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
	case "enum-reference", "enum-reference-python", "embedded-crd-types":
		t.Skip = codegen.NewStringSet("java/any") // python only
	case "go-nested-collections":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "external-enum":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "naming-collisions":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "assets-and-archives-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "simple-plain-schema-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "simple-enum-schema-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "secrets-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "plain-and-default-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "output-funcs-go-generics-only":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "regress-py-14539":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "regress-py-14012":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "methods-return-plain-resource":
		t.Skip = codegen.NewStringSet("java/any") // TODO
	case "go-overridden-internal-module-name":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "regress-py-12546":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "array-of-enum-map":
		t.Skip = codegen.NewStringSet("java/any") // go-only
	case "python-typed-dict-setuppy":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "python-typed-dict-pyproject":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "overlay-supported-languages":
		t.Skip = codegen.NewStringSet("java/any") // docs-only
	case "python-typed-dict-disabled-setuppy":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	case "regress-py-17219":
		t.Skip = codegen.NewStringSet("java/any") // python-only
	}

	if hasExtras {
		return newGeneratePackageTestConfigWithExtras(t, &pkgInfo)
	}

	return newGeneratePackageTestConfig(t)
}

func TestGeneratePackage(t *testing.T) {
	tcs, err := testCases()
	if err != nil {
		t.Error(err)
		t.FailNow()
	}
	for _, testCase := range tcs {
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
				extraFiles map[string][]byte,
			) (map[string][]byte, error) {
				pkg.Description = "test description"
				pkg.Repository = "https://github.com/pulumi/pulumi-java"
				pkg.Language = map[string]interface{}{
					"java": testCase.packageInfo,
				}
				return GeneratePackage(tool, pkg, extraFiles, nil, false)
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

func testCases() ([]generatePackageTestConfig, error) {
	keyDeps, err := keyDependencies()
	if err != nil {
		return nil, err
	}
	var ts []generatePackageTestConfig
	ts = append(ts, javaSpecificTests(keyDeps)...)
	for _, t := range test.PulumiPulumiSDKTests {
		ts = append(ts, adaptTest(t, keyDeps))
	}
	return ts, nil
}

func compileGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle build", pwd, "gradle", "build", "-x", "test")
}

func testGeneratedPackage(t *testing.T, pwd string) {
	test.RunCommand(t, "gradle test", pwd, "gradle", "test")
}

func keyDependencies() (map[string]string, error) {
	var buf bytes.Buffer
	cmd := exec.Command("gradle", "-q", "pulumi:exportKeyDependencies")
	cmd.Dir = filepath.Join("..", "..", "..", "sdk", "java")
	cmd.Stdout = &buf
	if err := cmd.Run(); err != nil {
		return nil, err
	}
	result := map[string]string{}
	if err := json.Unmarshal(buf.Bytes(), &result); err != nil {
		return nil, err
	}
	return result, nil
}

func makeDeps(deps map[string]string, packages ...string) map[string]string {
	spec := map[string]string{}
	for _, p := range packages {
		spec[p] = deps[p]
	}
	return spec
}
