// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// DocLanguageHelper is the Java-specific implementation of the
// similarly named codegen interface.
type DocLanguageHelper struct{}

var _ codegen.DocLanguageHelper = DocLanguageHelper{}

func (d DocLanguageHelper) GetLanguageTypeString(
	pkg *schema.Package, moduleName string, t schema.Type, input bool) string {

	modCtx := &modContext{pkg: pkg, mod: moduleName}
	ctx := newPseudoClassFileContext()

	typeShape := modCtx.typeString(
		ctx,
		t,
		noQualifier,
		input,
		false, // state
		false, // requireInitializers
		false, // outerOptional
		false, // inputlessOverload bool
	)

	code := typeShape.ToCodeWithOptions(ctx.imports, TypeShapeStringOptions{
		// Make sure @Nullable is not printed out.
		SkipAnnotations: true,
	})

	return code
}

func (d DocLanguageHelper) GetPropertyName(p *schema.Property) (string, error) {
	return names.Ident(p.Name).AsProperty().Getter(), nil
}

func (d DocLanguageHelper) GetFunctionName(modName string, f *schema.Function) string {
	return tokenToFunctionName(f.Token)
}

func (d DocLanguageHelper) GetResourceFunctionResultName(modName string, f *schema.Function) string {
	return tokenToFunctionResultClassName(f.Token).String()
}

func (d DocLanguageHelper) GetMethodName(m *schema.Method) string {
	// TODO revise when method support is built, revise when output-versioned functions are built
	return tokenToFunctionName(m.Function.Token)
}

func (d DocLanguageHelper) GetMethodResultName(
	pkg *schema.Package, modName string, r *schema.Resource, m *schema.Method) string {
	// TODO revise when method support is built, revise when output-versioned functions are built
	return tokenToFunctionResultClassName(m.Function.Token).String()
}

func (d DocLanguageHelper) GetEnumName(e *schema.Enum, typeName string) (string, error) {
	name := e.Name
	if name == "" {
		name = fmt.Sprintf("%v", e.Value)
	}
	return names.MakeSafeEnumName(name, typeName)
}

// GetDocLinkForPulumiType returns the Java API doc link for a Pulumi type.
func (d DocLanguageHelper) GetDocLinkForPulumiType(pkg *schema.Package, typeName string) string {
	typeName = strings.ReplaceAll(typeName, "?", "")
	result := fmt.Sprintf("/docs/reference/pkg/java/pulumi/pulumi/#%s", typeName)
	return result
}

// GetDocLinkForResourceType returns the Java API doc for a type belonging to a resource provider.
func (d DocLanguageHelper) GetDocLinkForResourceType(pkg *schema.Package, modName, typeName string) string {

	var path string
	switch {
	case pkg.Name != "" && modName != "":
		path = fmt.Sprintf("%s/%s", pkg.Name, modName)
	case pkg.Name == "" && modName != "":
		path = modName
	case pkg.Name != "" && modName == "":
		path = pkg.Name
	}
	typeName = strings.ReplaceAll(typeName, "?", "")
	result := fmt.Sprintf("/docs/reference/pkg/java/pulumi/%s/#%s", path, typeName)
	return result
}

// GetDocLinkForResourceInputOrOutputType returns the doc link for an input or output type of a Resource.
func (d DocLanguageHelper) GetDocLinkForResourceInputOrOutputType(
	pkg *schema.Package, modName, typeName string, input bool) string {
	typeName = strings.TrimSuffix(typeName, "?")
	parts := strings.Split(typeName, ".")
	typeName = parts[len(parts)-1]
	var result string
	if input {
		result = fmt.Sprintf("/docs/reference/pkg/java/pulumi/%s/types/input/#%s", pkg.Name, typeName)
	}
	result = fmt.Sprintf("/docs/reference/pkg/java/pulumi/%s/types/output/#%s", pkg.Name, typeName)
	return result
}

// GetDocLinkForFunctionInputOrOutputType returns the doc link for an
// input or output type of a Function.
func (d DocLanguageHelper) GetDocLinkForFunctionInputOrOutputType(
	pkg *schema.Package, modName, typeName string, input bool) string {

	res := d.GetDocLinkForResourceInputOrOutputType(pkg, modName, typeName, input)
	return res
}

// GetModuleDocLink returns the display name and the link for a
// module.
func (d DocLanguageHelper) GetModuleDocLink(pkg *schema.Package, modName string) (string, string) {

	var displayName string
	var link string
	if modName == "" {
		displayName = fmt.Sprintf("@pulumi/%s", pkg.Name)
	} else {
		displayName = fmt.Sprintf("@pulumi/%s/%s", pkg.Name, strings.ToLower(modName))
	}
	link = d.GetDocLinkForResourceType(pkg, modName, "")

	return displayName, link
}
