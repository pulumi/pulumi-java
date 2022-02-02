// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package generative

import (
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"pgregory.net/rapid"

	jvmgen "github.com/pulumi/pulumi-java/pkg/codegen/jvm"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func generatesCompilingJavaCode(rootDir string, pkgSpec pschema.PackageSpec) error {
	pkg, err := pschema.ImportSpec(pkgSpec, nil /*languages*/)
	if err != nil {
		return fmt.Errorf("pchema.ImportSpec failed: %w", err)
	}

	// TODO overlays
	tfgen := "the Pulumi Terraform Bridge (tfgen) Tool"
	files, err := jvmgen.GeneratePackage(tfgen, pkg, nil)
	if err != nil {
		return fmt.Errorf("jvmgen.GeneratePackage failed: %w", err)
	}

	for f, bytes := range files {
		path := filepath.Join(rootDir, f)

		if err := os.MkdirAll(filepath.Dir(path), os.ModePerm); err != nil {
			return fmt.Errorf("os.MkdirAll failed: %w", err)
		}

		if err := ioutil.WriteFile(path, bytes, 0600); err != nil {
			return fmt.Errorf("ioutil.WriteFile failed: %w", err)
		}
	}

	cmd := exec.Command("gradle", "build")
	cmd.Dir = rootDir

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("gradle build failed: %w", err)
	}

	// for f := range files {
	// 	fmt.Printf("Generated file %s", f)
	// }
	return nil
}

func TestRandomSchemasGenerateCompilingJavaCode(t *testing.T) {
	tempdir := "tmp"

	gen := &schemaGenerators{}
	packageSpecGen := gen.PackageSpec()
	n := 0
	rapid.Check(t, func(t *rapid.T) {
		n = n + 1
		dir := filepath.Join(tempdir, fmt.Sprintf("%d", n))
		t.Logf("dir = %s", dir)
		packageSpec := packageSpecGen.Draw(t, "packageSpec").(pschema.PackageSpec)
		err := generatesCompilingJavaCode(dir, packageSpec)
		if err != nil {
			t.Fatalf("failed to compile java code: %v", err)
		}
	})
}

// // PackageSpec is the serializable description of a Pulumi package.
// type PackageSpec struct {
// 	// Name is the unqualified name of the package (e.g. "aws", "azure", "gcp", "kubernetes", "random")
// 	Name string `json:"name" yaml:"name"`
// 	// DisplayName is the human-friendly name of the package.
// 	DisplayName string `json:"displayName,omitempty" yaml:"displayName,omitempty"`
// 	// Version is the version of the package. The version must be valid semver.
// 	Version string `json:"version,omitempty" yaml:"version,omitempty"`
// 	// Description is the description of the package.
// 	Description string `json:"description,omitempty" yaml:"description,omitempty"`
// 	// Keywords is the list of keywords that are associated with the package, if any.
// 	// Some reserved keywords can be specified as well that help with categorizing the
// 	// package in the Pulumi registry. `category/<name>` and `kind/<type>` are the only
// 	// reserved keywords at this time, where `<name>` can be one of:
// 	// `cloud`, `database`, `infrastructure`, `monitoring`, `network`, `utility`, `vcs`
// 	// and `<type>` is either `native` or `component`. If the package is a bridged Terraform
// 	// provider, then don't include the `kind/` label.
// 	Keywords []string `json:"keywords,omitempty" yaml:"keywords,omitempty"`
// 	// Homepage is the package's homepage.
// 	Homepage string `json:"homepage,omitempty" yaml:"homepage,omitempty"`
// 	// License indicates which license is used for the package's contents.
// 	License string `json:"license,omitempty" yaml:"license,omitempty"`
// 	// Attribution allows freeform text attribution of derived work, if needed.
// 	Attribution string `json:"attribution,omitempty" yaml:"attribution,omitempty"`
// 	// Repository is the URL at which the source for the package can be found.
// 	Repository string `json:"repository,omitempty" yaml:"repository,omitempty"`
// 	// LogoURL is the URL for the package's logo, if any.
// 	LogoURL string `json:"logoUrl,omitempty" yaml:"logoUrl,omitempty"`
// 	// PluginDownloadURL is the URL to use to acquire the provider plugin binary, if any.
// 	PluginDownloadURL string `json:"pluginDownloadURL,omitempty" yaml:"pluginDownloadURL,omitempty"`
// 	// Publisher is the name of the person or organization that authored and published the package.
// 	Publisher string `json:"publisher,omitempty" yaml:"publisher,omitempty"`

// 	// Meta contains information for the importer about this package.
// 	Meta *MetadataSpec `json:"meta,omitempty" yaml:"meta,omitempty"`

// 	// A list of allowed package name in addition to the Name property.
// 	AllowedPackageNames []string `json:"allowedPackageNames,omitempty" yaml:"allowedPackageNames,omitempty"`

// 	// Config describes the set of configuration variables defined by this package.
// 	Config ConfigSpec `json:"config" yaml:"config"`
// 	// Types is a map from type token to ComplexTypeSpec that describes the set of complex types (ie. object, enum)
// 	// defined by this package.
// 	Types map[string]ComplexTypeSpec `json:"types,omitempty" yaml:"types,omitempty"`
// 	// Provider describes the provider type for this package.
// 	Provider ResourceSpec `json:"provider" yaml:"provider"`
// 	// Resources is a map from type token to ResourceSpec that describes the set of resources defined by this package.
// 	Resources map[string]ResourceSpec `json:"resources,omitempty" yaml:"resources,omitempty"`
// 	// Functions is a map from token to FunctionSpec that describes the set of functions defined by this package.
// 	Functions map[string]FunctionSpec `json:"functions,omitempty" yaml:"functions,omitempty"`
// 	// Language specifies additional language-specific data about the package.
// 	Language map[string]RawMessage `json:"language,omitempty" yaml:"language,omitempty"`
// }

// * Should we generate PackageSpec using Go grammar
//   or should we generate it straight off th e JSON grammar? Erm

// Key attributes here:
//
// .Resources
// .Functions
// .Types

//
// ResourceSpec is the serializable form of a resource description.

// type ResourceSpec struct {
// 	ObjectTypeSpec `yaml:",inline"`

// 	// InputProperties is a map from property name to PropertySpec that describes the resource's input properties.
// 	InputProperties map[string]PropertySpec `json:"inputProperties,omitempty" yaml:"inputProperties,omitempty"`
// 	// RequiredInputs is a list of the names of the resource's required input properties.
// 	RequiredInputs []string `json:"requiredInputs,omitempty" yaml:"requiredInputs,omitempty"`
// 	// PlainInputs was a list of the names of the resource's plain input properties. This property is ignored:
// 	// instead, property types should be marked as plain where necessary.
// 	PlainInputs []string `json:"plainInputs,omitempty" yaml:"plainInputs,omitempty"`
// 	// StateInputs is an optional ObjectTypeSpec that describes additional inputs that mau be necessary to get an
// 	// existing resource. If this is unset, only an ID is necessary.
// 	StateInputs *ObjectTypeSpec `json:"stateInputs,omitempty" yaml:"stateInputs,omitempty"`
// 	// Aliases is the list of aliases for the resource.
// 	Aliases []AliasSpec `json:"aliases,omitempty" yaml:"aliases,omitempty"`
// 	// DeprecationMessage indicates whether or not the resource is deprecated.
// 	DeprecationMessage string `json:"deprecationMessage,omitempty" yaml:"deprecationMessage,omitempty"`
// 	// IsComponent indicates whether the resource is a ComponentResource.
// 	IsComponent bool `json:"isComponent,omitempty" yaml:"isComponent,omitempty"`
// 	// Methods maps method names to functions in this schema.
// 	Methods map[string]string `json:"methods,omitempty" yaml:"methods,omitempty"`
// }

// // FunctionSpec is the serializable form of a function description.
// type FunctionSpec struct {
// 	// Description is the description of the function, if any.
// 	Description string `json:"description,omitempty" yaml:"description,omitempty"`
// 	// Inputs is the bag of input values for the function, if any.
// 	Inputs *ObjectTypeSpec `json:"inputs,omitempty" yaml:"inputs,omitempty"`
// 	// Outputs is the bag of output values for the function, if any.
// 	Outputs *ObjectTypeSpec `json:"outputs,omitempty" yaml:"outputs,omitempty"`
// 	// DeprecationMessage indicates whether or not the function is deprecated.
// 	DeprecationMessage string `json:"deprecationMessage,omitempty" yaml:"deprecationMessage,omitempty"`
// 	// Language specifies additional language-specific data about the function.
// 	Language map[string]RawMessage `json:"language,omitempty" yaml:"language,omitempty"`
// 	// IsOverlay indicates whether the function is an overlay provided by the package. Overlay code is generated by the
// 	// package rather than using the core Pulumi codegen libraries.
// 	IsOverlay bool `json:"isOverlay,omitempty" yaml:"isOverlay,omitempty"`
// }
