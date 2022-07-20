// Copyright 2016-2022, Pulumi Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package java

const defaultBasePackage = "com.pulumi."

// PackageInfo defines Java-specific extensions to Pulumi Packages
// Schema.
//
// These options affect how GeneratePackage generates and lays out
// Java SDKs for Pulumi Packages.
//
// The options are typically set in the .language.java section of
// the Schema, but can also be overridden during code generation:
//
//     pulumi-java-gen generate --override overrides.json
//
// See https://www.pulumi.com/docs/guides/pulumi-packages/schema/#language-specific-extensions
type PackageInfo struct {

	// Overrides how Schema packages names translate to Java
	// package names.
	//
	// Absent an entry in Packages, Java code generator tries to
	// do something reasonable and produce a Java looking name for
	// a given Schema package. If this undesirable, adding an
	// entry in Packages will specify the desired Java name
	// manually.
	//
	// For example, pulumi-kubernetes specifies Packages names for
	// versioned API packages like this:
	//
	//     packages:
	//       "admissionregistration.k8s.io/v1: "admissionregistration.v1"
	//
	Packages map[string]string `json:"packages,omitempty"`

	// The prefix of the Java package to generate.
	//
	// Defaults to "com.pulumi".
	//
	// The generated Java code will start with:
	//
	//     package ${BasePackage}.${pkg.Name};
	//
	// Note that pkg.Name is coming from the Schema.
	//
	// For example, if the package is named "azuread" then the
	// generated code will be placed under:
	//
	//     package com.pulumi.azuread
	//
	// And if BasePackage is set to "com.myorg" then the generated
	// code will instead be placed under:
	//
	//     package com.myorg.azuread
	//
	BasePackage string `json:"basePackage"`

	// If set to "gradle" enables a generation of a basic set of
	// Gradle build files.
	BuildFiles string `json:"buildFiles"`

	// Specifies Maven-style dependencies for the generated code.
	//
	// The dependency on Java SDK (com.pulumi:pulumi) is special
	// and will be auto-popualted if not specified here.
	//
	// Other dependencies need to be specified if:
	//
	// - the package references types from other packages
	//
	// - the code generator is asked to emit build files, as in
	//   BuildFiles="gradle", and produce a compiling project
	//
	// For example, pulumi-eks referenes pulumi-aws types, and can
	// specify the desired version:
	//
	//     dependencies:
	//       "com.pulumi:aws": "5.4.0"
	Dependencies map[string]string `json:"dependencies,omitempty"`

	// If non-empty and BuildFiles="gradle", enables the use of a
	// given version of io.github.gradle-nexus.publish-plugin in
	// the generated Gradle build files.
	GradleNexusPublishPluginVersion string `json:"gradleNexusPublishPluginVersion"`
}

func (i PackageInfo) With(overrides PackageInfo) PackageInfo {
	result := i
	if overrides.BuildFiles != "" {
		result.BuildFiles = overrides.BuildFiles
	}
	if overrides.BasePackage != "" {
		result.BasePackage = ""
	}
	if overrides.Packages != nil && len(overrides.Packages) > 0 {
		if result.Packages == nil {
			result.Packages = map[string]string{}
		}
		for k, v := range overrides.Packages {
			result.Packages[k] = v
		}
	}
	if overrides.Dependencies != nil && len(overrides.Dependencies) > 0 {
		if result.Dependencies == nil {
			result.Dependencies = map[string]string{}
		}
		for k, v := range overrides.Dependencies {
			result.Dependencies[k] = v
		}
	}
	if overrides.GradleNexusPublishPluginVersion != "" {
		result.GradleNexusPublishPluginVersion = overrides.GradleNexusPublishPluginVersion
	}
	return result
}

func (i PackageInfo) BasePackageOrDefault() string {
	if len(i.BasePackage) > 0 {
		return ensureEndsWithDot(i.BasePackage)
	}
	return ensureEndsWithDot(defaultBasePackage)
}
