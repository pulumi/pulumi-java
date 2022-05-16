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
	"path/filepath"
	"strings"

	"github.com/blang/semver"

	javagen "github.com/pulumi/pulumi-java/pkg/codegen/java"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
)

type generateJavaOptions struct {
	// URL or path to a file containing a Pulumi Package schema
	Schema string

	// Version for the generated code
	Version semver.Version

	// Root dir to resolve relative paths against
	RootDir string

	// Output dir to write the generated code to
	OutputDir string

	// If non-nil, overrides PackageInfo parsed from the Schema
	PackageInfo javagen.PackageInfo

	// Paths to folders to mix into the generated code by copying
	Overlays []string

	// Deprecated: path to version.txt to emit; in the future will
	// be always computed and not customizable
	VersionFile string

	// Deprecated: path to plugins.json metadata to emit; in the
	// future will be always computed and not customizable
	PluginFile string

	// Deprecated: expanding templates in overlays will be removed
	// once provider SDK builds move out of pulumi/pulumi-java repo.
	OverlayTemplateConfig OverlayTemplateConfig
}

func generateJava(cfg generateJavaOptions) error {
	rawPkgSpec, err := readPackageSchema(cfg.Schema)
	if err != nil {
		return fmt.Errorf("failed to read schema from %s: %w", cfg.Schema, err)
	}

	pkgSpec, err := dedupTypes(rawPkgSpec)
	if err != nil {
		return fmt.Errorf("failed to dedup types in schema from %s: %w", cfg.Schema, err)
	}

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	version := cfg.Version
	pkg.Version = &version

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

	extraFiles, err := readOverlays(cfg.RootDir, cfg.Overlays, cfg.OverlayTemplateConfig)
	if err != nil {
		return err
	}
	files, err := javagen.GeneratePackage("pulumi-java-gen", pkg, extraFiles)
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

	if cfg.VersionFile == "" {
		parts := strings.Split(pkgInfo.BasePackageOrDefault(), ".")
		cfg.VersionFile = filepath.Join(append(
			[]string{"src", "main", "resources"},
			append(parts, pkg.Name, "version.txt")...)...)
	}

	{
		f := filepath.Join(outDir, cfg.VersionFile)
		bytes := []byte(cfg.Version.String())
		if err := emitFile(f, bytes); err != nil {
			return fmt.Errorf("failed to generate version file at %s: %w", f, err)
		}
	}

	if cfg.PluginFile == "" {
		parts := strings.Split(pkgInfo.BasePackageOrDefault(), ".")
		cfg.PluginFile = filepath.Join(append(
			[]string{"src", "main", "resources"},
			append(parts, pkg.Name, "plugin.json")...)...)
	}

	{
		pulumiPlugin := &plugin.PulumiPluginJSON{
			Resource: true,
			Name:     pkg.Name,
			Version:  cfg.Version.String(),
			Server:   pkg.PluginDownloadURL,
		}

		f := filepath.Join(outDir, cfg.PluginFile)
		bytes, err := (pulumiPlugin).JSON()
		if err != nil {
			return fmt.Errorf("failed to serialize plugin file at %s: %w", f, err)
		}
		if err := emitFile(f, bytes); err != nil {
			return fmt.Errorf("failed to generate plugin file at %s: %w", f, err)
		}
	}

	return nil
}
