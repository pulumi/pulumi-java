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

func (d DocLanguageHelper) GetDocLinkForResourceType(pkg *schema.Package, modName, typeName string) string {
	panic("Not implemented")
}

func (d DocLanguageHelper) GetDocLinkForResourceInputOrOutputType(
	pkg *schema.Package, modName, typeName string, input bool) string {
	panic("Not implemented")
}

func (d DocLanguageHelper) GetDocLinkForFunctionInputOrOutputType(
	pkg *schema.Package, modName, typeName string, input bool) string {

	panic("Not implemented")
}

func (d DocLanguageHelper) GetModuleDocLink(pkg *schema.Package, modName string) (string, string) {
	panic("Not implemented")
}
