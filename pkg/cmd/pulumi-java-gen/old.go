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

// Old style main. The style of invocation with -config argument
// pointing to a YAML-encoded Config file will be phased out when all
// provider builds move out of the pulumi/pulumi-java repo.

package main

import (
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"path/filepath"
	"strings"

	"github.com/blang/semver"
	"gopkg.in/yaml.v3"

	javagen "github.com/pulumi/pulumi-java/pkg/codegen/java"
)

type Config struct {
	ArtifactID    string      `yaml:"artifactID"`
	Version       string      `yaml:"version"`
	Schema        string      `yaml:"schema"`
	Out           string      `yaml:"out"`
	VersionFile   string      `yaml:"versionFile"`
	PackageInfo   interface{} `yaml:"packageInfo"`
	PluginFile    string      `yaml:"pluginFile"`
	Overlays      []string    `yaml:"overlays"`
	VersionEnvVar string      `yaml:"versionEnvVar"`
}

func oldStyleMain() {
	config := flag.String("config", "", "path to a YAML-encoded Config file; "+
		"if this option is set others take no effect")
	flag.Parse()

	rootDir, err := filepath.Abs(filepath.Dir(*config))
	if err != nil {
		log.Fatal(err)
	}

	cfg, err := parseConfig(*config)
	if err != nil {
		log.Fatal(err)
	}

	var pkgInfo javagen.PackageInfo
	if cfg.PackageInfo != nil {
		p, err := convertPackageInfo(cfg.PackageInfo)
		if err != nil {
			log.Fatal(err)
		}
		pkgInfo = p
	}

	version := semver.MustParse(cfg.Version)

	opts := generateJavaOptions{
		Schema:      cfg.Schema,
		Version:     &version,
		RootDir:     rootDir,
		OutputDir:   cfg.Out,
		PackageInfo: pkgInfo,
		Overlays:    cfg.Overlays,
		VersionFile: cfg.VersionFile,
		PluginFile:  cfg.PluginFile,
		OverlayTemplateConfig: OverlayTemplateConfig{
			ArtifactID:     cfg.ArtifactID,
			VersionEnvVar:  cfg.VersionEnvVar,
			DefaultVersion: cfg.Version,
		},
	}

	if err := generateJava(opts); err != nil {
		log.Fatal(err)
	}
}

func isOldStyleArgs(args []string) bool {
	oldStyleArgs := false
	for _, arg := range args {
		if arg == "-config" {
			oldStyleArgs = true
		}
	}
	return oldStyleArgs
}

func parseConfig(path string) (*Config, error) {
	bytes, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("Failed to read yaml config from %s: %w", path, err)
	}
	var cfg Config
	if err := yaml.Unmarshal(bytes, &cfg); err != nil {
		return nil, fmt.Errorf("Failed to parse yaml config from %s: %w", path, err)
	}
	if cfg.Schema == "" {
		return nil, fmt.Errorf("Missing required field in config at %s: schema", path)
	}
	if cfg.Out == "" {
		cfg.Out = "sdk/java"
	}
	if cfg.Version == "" {
		return nil, fmt.Errorf("Missing required field in config at %s: version", path)
	}
	if cfg.VersionEnvVar == "" {
		cfg.VersionEnvVar = fmt.Sprintf("PULUMI_%s_PROVIDER_SDK_VERSION",
			strings.ReplaceAll(strings.ToUpper(cfg.ArtifactID),
				"-", "_"))
	}
	return &cfg, nil
}
