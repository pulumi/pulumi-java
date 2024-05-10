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

package main

import (
	"fmt"
	"os"
	"strings"

	"github.com/blang/semver"
	"github.com/spf13/cobra"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"

	"github.com/pulumi/pulumi-java/pkg/codegen/java"
	"github.com/pulumi/pulumi-java/pkg/version"
)

func newPulumiJavaGenCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "pulumi-java-gen",
		Short: "pulumi-java-gen: Java SDK code generator for Pulumi Packages",
		Long: `pulumi-java-gen: Java SDK code generator for Pulumi Packages

See https://www.pulumi.com/docs/guides/pulumi-packages/schema/
`,
	}
	cmd.AddCommand(newGenerateCommand())
	cmd.AddCommand(newVersionCommand())
	return cmd
}

func newVersionCommand() *cobra.Command {
	return &cobra.Command{
		Use:   "version",
		Short: "Print pulumi-java-gen version number",
		Args:  cmdutil.NoArgs,
		Run: cmdutil.RunFunc(func(_ *cobra.Command, _ []string) error {
			fmt.Printf("%v\n", version.Version)
			return nil
		}),
	}
}

func newGenerateCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "generate",
		Short: "Generate a Java SDK from a Pulumi Package schema",
		Long: `Generate a Java SDK from a Pulumi Package schema

Java-specific language options can be provided as part of the schema
file, under .language.java, or else as a separate JSON file specified
in the --override argument.

Current language options include:

- buildFiles    sets the flavor of build files to generate (default "gradle")
- basePackage   sets the namespace of the generated package
- packages      defines dependencies as a map of Maven package specs to versions

See https://www.pulumi.com/docs/guides/pulumi-packages/schema/#language-specific-extensions
`,
	}

	var versionArg, javaSdkVersionArg, schemaArg, outArg, overrideArg, buildArg string

	cmd.Flags().StringVar(&versionArg, "version", "",
		"default semantic version for the generated package")

	cmd.Flags().StringVar(&schemaArg, "schema", "",
		"URL or local path to a package schema")

	contract.AssertNoErrorf(cmd.MarkFlagRequired("schema"),
		"could not mark flag 'schema' as required")

	cmd.Flags().StringVar(&outArg, "out", "java", "output path")

	cmd.Flags().StringVar(&javaSdkVersionArg, "sdk", version.Version,
		"com.pulumi:pulumi version to depend on in the generated code")

	cmd.Flags().StringVar(&overrideArg, "override", "",
		"path to a JSON file with overrides for .language.java")

	cmd.Flags().StringVar(&buildArg, "build", "gradle",
		`flavor of build files to generate:
 - "", "none":            do not generate any build files
 - "gradle":              generate a Gradle project
 - "gradle-nexus[:$VER]": generate a Gradle project with gradle-nexus/publish-plugin

`)

	cmd.Run = cmdutil.RunFunc(func(_ *cobra.Command, _ []string) error {
		rootDir, err := os.Getwd()
		if err != nil {
			return err
		}

		var version *semver.Version
		if versionArg != "" {
			ver, err := semver.Parse(versionArg)
			if err != nil {
				return err
			}
			version = &ver
		}

		opts := generateJavaOptions{
			Schema:    schemaArg,
			Version:   version,
			RootDir:   rootDir,
			OutputDir: outArg,
		}

		if overrideArg != "" {
			overrides, err := parsePackageInfoOverride(overrideArg)
			if err != nil {
				return err
			}
			opts.PackageInfo = opts.PackageInfo.With(overrides)
		}

		buildArgOverrides, err := parseBuildOption(buildArg)
		if err != nil {
			return err
		}

		opts.PackageInfo = opts.PackageInfo.With(buildArgOverrides).
			WithDefaultDependencies()

		if javaSdkVersionArg != "" {
			parsedVersion, err := semver.ParseTolerant(javaSdkVersionArg)
			if err != nil {
				return err
			}
			opts.PackageInfo = opts.PackageInfo.
				WithJavaSdkDependencyDefault(parsedVersion)
		}

		return generateJava(opts)
	})

	return cmd
}

func parseBuildOption(buildOption string) (java.PackageInfo, error) {
	switch buildOption {
	case "", "none":
		return java.PackageInfo{}, nil
	case "gradle":
		return java.PackageInfo{BuildFiles: "gradle"}, nil
	case "gradle-nexus":
		return java.PackageInfo{
			BuildFiles:                      "gradle",
			GradleNexusPublishPluginVersion: "1.1.0",
		}, nil
	}
	if strings.HasPrefix(buildOption, "gradle-nexus:") {
		v := strings.TrimPrefix(buildOption, "gradle-nexus:")
		return java.PackageInfo{
			BuildFiles:                      "gradle",
			GradleNexusPublishPluginVersion: v,
		}, nil
	}
	return java.PackageInfo{},
		fmt.Errorf(`Unrecognized value %q passed to the --build option.
Supported values are: "", "none", "gradle", "gradle-nexus[:$VER]"`,
			buildOption)
}
