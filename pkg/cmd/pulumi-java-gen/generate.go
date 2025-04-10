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
	"path/filepath"

	"github.com/blang/semver"
	"github.com/hashicorp/hcl/v2"

	javagen "github.com/pulumi/pulumi-java/pkg/codegen/java"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/diag"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/cmdutil"
)

type generateJavaOptions struct {
	// URL or path to a file containing a Pulumi Package schema
	Schema string

	// Root dir to resolve relative paths against
	RootDir string

	// Output dir to write the generated code to
	OutputDir string

	// If non-nil, overrides PackageInfo parsed from the Schema
	PackageInfo javagen.PackageInfo

	// Paths to folders to mix into the generated code by copying
	Overlays []string

	// Optional version to set on the package.
	Version *semver.Version

	// True if the generator should generate an SDK suitable for local consumption as opposed to a publishable package.
	Local bool
}

func generateJava(cfg generateJavaOptions) error {
	rawPkgSpec, err := readPackageSchema(cfg.Schema)
	if err != nil {
		return fmt.Errorf("failed to read schema from %s: %w", cfg.Schema, err)
	}

	pkgSpec, diags, err := javagen.DeduplicateTypes(rawPkgSpec)
	if err != nil {
		return fmt.Errorf("failed to dedup types in schema from %s: %w", cfg.Schema, err)
	}
	printDiagnostics(diags)

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	pkg.Version = cfg.Version

	var pkgInfo javagen.PackageInfo
	if raw, ok := pkg.Language["java"]; ok {
		p, err := convertPackageInfo(raw)
		if err != nil {
			return fmt.Errorf("failed to read .language.java: %w", err)
		}
		pkgInfo = p
	}
	pkgInfo = pkgInfo.With(cfg.PackageInfo)
	pkg.Language["java"] = pkgInfo

	extraFiles, err := readOverlays(cfg.RootDir, cfg.Overlays)
	if err != nil {
		return err
	}
	files, err := javagen.GeneratePackage(
		"pulumi-java-gen",
		pkg,
		extraFiles,
		nil, /*localDependencies*/
		cfg.Local,
		true,  /*legacyBuildFiles*/
		false, /*generatePolicyPack*/
	)
	if err != nil {
		return err
	}

	outDir := filepath.Join(cfg.RootDir, cfg.OutputDir)

	if err := cleanDir(outDir); err != nil {
		return err
	}

	for f, bytes := range files {
		if err := emitFile(filepath.Join(outDir, f), bytes); err != nil {
			return err
		}
	}

	filesForPolicyPack, err := javagen.GeneratePackage(
		"pulumi-java-gen",
		pkg,
		extraFiles,
		nil, /*localDependencies*/
		cfg.Local,
		true, /*legacyBuildFiles*/
		true, /*generatePolicyPack*/
	)
	if err != nil {
		return err
	}

	outDirForPolicyPack := filepath.Join(cfg.RootDir, cfg.OutputDir+"_policy")

	if err := cleanDir(outDirForPolicyPack); err != nil {
		return err
	}

	for f, bytes := range filesForPolicyPack {
		if err := emitFile(filepath.Join(outDirForPolicyPack, f), bytes); err != nil {
			return err
		}
	}

	return nil
}

// printDiagnostics prints the given diagnostics to stdout and stderr.
func printDiagnostics(diagnostics hcl.Diagnostics) {
	sink := diag.DefaultSink(os.Stdout, os.Stderr, diag.FormatOptions{Color: cmdutil.GetGlobalColorization()})
	for _, diagnostic := range diagnostics {
		if diagnostic.Severity == hcl.DiagError {
			sink.Errorf(diag.Message("", "%s"), diagnostic)
		} else {
			sink.Warningf(diag.Message("", "%s"), diagnostic)
		}
	}
}
