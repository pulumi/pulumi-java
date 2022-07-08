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

import (
	"encoding/json"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

const defaultBasePackage = "com.pulumi."

// PropertyInfo represents a Java language-specific info for a property.
type PropertyInfo struct {
	Name string `json:"name,omitempty"`
}

// PackageInfo represents a Java language-specific info for a package.
type PackageInfo struct {
	Packages    map[string]string `json:"packages,omitempty"`
	BasePackage string            `json:"basePackage"`

	// If set to "gradle" generates a basic set of Gradle build files.
	BuildFiles string `json:"buildFiles"`

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

func ensureEndsWithDot(s string) string {
	if strings.HasSuffix(s, ".") {
		return s
	}
	return s + "."
}

// Importer implements schema.Language for Java
var Importer schema.Language = importer(0)

type importer int

// ImportDefaultSpec decodes language-specific metadata associated with a DefaultValue.
func (importer) ImportDefaultSpec(def *schema.DefaultValue, raw json.RawMessage) (interface{}, error) {
	return raw, nil
}

// ImportPropertySpec decodes language-specific metadata associated with a Property.
func (importer) ImportPropertySpec(property *schema.Property, raw json.RawMessage) (interface{}, error) {
	var info PropertyInfo
	if err := json.Unmarshal([]byte(raw), &info); err != nil {
		return nil, err
	}
	return info, nil
}

// ImportObjectTypeSpec decodes language-specific metadata associated with a ObjectType.
func (importer) ImportObjectTypeSpec(object *schema.ObjectType, raw json.RawMessage) (interface{}, error) {
	return raw, nil
}

// ImportResourceSpec decodes language-specific metadata associated with a Resource.
func (importer) ImportResourceSpec(resource *schema.Resource, raw json.RawMessage) (interface{}, error) {
	return raw, nil
}

// ImportFunctionSpec decodes language-specific metadata associated with a Function.
func (importer) ImportFunctionSpec(function *schema.Function, raw json.RawMessage) (interface{}, error) {
	return raw, nil
}

// ImportPackageSpec decodes language-specific metadata associated with a Package.
func (importer) ImportPackageSpec(pkg *schema.Package, raw json.RawMessage) (interface{}, error) {
	var info PackageInfo
	if err := json.Unmarshal([]byte(raw), &info); err != nil {
		return nil, err
	}
	return info, nil
}
