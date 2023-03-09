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
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v3"

	javagen "github.com/pulumi/pulumi-java/pkg/codegen/java"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func readPackageSchema(path string) (*pschema.PackageSpec, error) {
	var stream io.ReadCloser
	if strings.HasPrefix(path, "http") {
		resp, err := http.Get(path) // #nosec G107
		if err != nil {
			return nil, err
		}
		stream = resp.Body
	} else {
		file, err := os.Open(path)
		if err != nil {
			return nil, err
		}
		stream = file
	}

	defer stream.Close()
	var result pschema.PackageSpec
	if strings.HasSuffix(path, ".yaml") {
		dec := yaml.NewDecoder(stream)
		if err := dec.Decode(&result); err != nil {
			return nil, fmt.Errorf("reading YAML schema: %w", err)
		}
	} else {
		dec := json.NewDecoder(stream)
		if err := dec.Decode(&result); err != nil {
			return nil, fmt.Errorf("reading JSON schema: %w", err)
		}
	}
	return &result, nil
}

func convertPackageInfo(mapParsedFromYaml interface{}) (javagen.PackageInfo, error) {
	packageInfoJSON, err := json.Marshal(mapParsedFromYaml)
	if err != nil {
		return javagen.PackageInfo{}, err
	}

	var result javagen.PackageInfo
	if err := json.Unmarshal(packageInfoJSON, &result); err != nil {
		return javagen.PackageInfo{}, err
	}
	return result, nil
}

func parsePackageInfoOverride(path string) (javagen.PackageInfo, error) {
	bytes, err := os.ReadFile(path)
	empty := javagen.PackageInfo{}
	if err != nil {
		return empty, fmt.Errorf("Failed to read language override from %s: %w", path, err)
	}
	var override javagen.PackageInfo
	if err := json.Unmarshal(bytes, &override); err != nil {
		return empty, fmt.Errorf("Failed to parse language override from %s: %w", path, err)
	}
	return override, nil
}

func cleanDir(path string) error {
	return os.RemoveAll(path)
}

func emitFile(path string, bytes []byte) error {
	if err := os.MkdirAll(filepath.Dir(path), os.ModePerm); err != nil {
		return fmt.Errorf("os.MkdirAll failed: %w", err)
	}
	if err := os.WriteFile(path, bytes, 0o600); err != nil {
		return fmt.Errorf("os.WriteFile failed: %w", err)
	}
	return nil
}
