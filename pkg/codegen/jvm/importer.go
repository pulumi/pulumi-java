package jvm

import (
	"encoding/json"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

const defaultBasePackage = "io.pulumi."

// JVMPropertyInfo represents the a JVM language-specific info for a property.
type JVMPropertyInfo struct {
	Name string `json:"name,omitempty"`
}

// JVMPackageInfo represents the a JVM language-specific info for a package.
type JVMPackageInfo struct {
	PackageReferences      map[string]string `json:"packageReferences,omitempty"`
	Packages               map[string]string `json:"packages,omitempty"`
	Compatibility          string            `json:"compatibility,omitempty"`
	DictionaryConstructors bool              `json:"dictionaryConstructors,omitempty"`
	BasePackage            string            `json:"basePackage"`
}

func (i JVMPackageInfo) BasePackageOrDefault() string {
	if len(i.BasePackage) > 0 {
		return ensureEndsWithDot(i.BasePackage)
	} else {
		return ensureEndsWithDot(defaultBasePackage)
	}
}

func ensureEndsWithDot(s string) string {
	if strings.HasSuffix(s, ".") {
		return s
	} else {
		return s + "."
	}
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
	var info JVMPropertyInfo
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
	var info JVMPackageInfo
	if err := json.Unmarshal([]byte(raw), &info); err != nil {
		return nil, err
	}
	return info, nil
}
