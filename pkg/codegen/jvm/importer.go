// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"encoding/json"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

const defaultBasePackage = "com.pulumi."

// PropertyInfo represents a JVM language-specific info for a property.
type PropertyInfo struct {
	Name string `json:"name,omitempty"`
}

// PackageInfo represents a JVM language-specific info for a package.
type PackageInfo struct {
	PackageReferences      map[string]string `json:"packageReferences,omitempty"`
	Packages               map[string]string `json:"packages,omitempty"`
	DictionaryConstructors bool              `json:"dictionaryConstructors,omitempty"`
	BasePackage            string            `json:"basePackage"`

	// If set to "gradle" generates a basic set of Gradle build files.
	BuildFiles string `json:"buildFiles"`
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

// Importer implements schema.Language for JVM
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
