// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// DocLanguageHelper is the Java-specific implementation of the
// similarly named codegen interface.
type DocLanguageHelper struct{}

var _ codegen.DocLanguageHelper = DocLanguageHelper{}

func (d DocLanguageHelper) GetLanguageTypeString(
	pkg *schema.Package, moduleName string, t schema.Type, input bool,
) string {
	modCtx := &modContext{pkg: pkg.Reference(), mod: moduleName}
	ctx := newPseudoClassFileContext()

	typeShape := modCtx.typeString(
		ctx,
		t,
		noQualifier,
		input,
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

func (d DocLanguageHelper) GetFunctionName(_ string, f *schema.Function) string {
	return tokenToFunctionName(f.Token)
}

func (d DocLanguageHelper) GetResourceFunctionResultName(_ string, f *schema.Function) string {
	return tokenToFunctionResultClassName(nil, f.Token).String()
}

func (d DocLanguageHelper) GetMethodName(m *schema.Method) string {
	// TODO revise when method support is built, revise when output-versioned functions are built
	return tokenToFunctionName(m.Function.Token)
}

func (d DocLanguageHelper) GetMethodResultName(
	_ *schema.Package, _ string, _ *schema.Resource, m *schema.Method,
) string {
	// TODO revise when method support is built, revise when output-versioned functions are built
	return tokenToFunctionResultClassName(nil, m.Function.Token).String()
}

func (d DocLanguageHelper) GetEnumName(e *schema.Enum, typeName string) (string, error) {
	name := e.Name
	if name == "" {
		name = fmt.Sprintf("%v", e.Value)
	}
	return names.MakeSafeEnumName(name, typeName)
}

// TODO revisit once javadocs are published.
//
// Not every language currently supports this. For Python docsgen the
// implementation returns "" also.
//
// For an example of a language that supports this, check out Go.
//
// When visiting
// https://www.pulumi.com/registry/packages/aws/api-docs/s3/bucket/
// search for `func NewBucket`.
//
// Context, BucketArgs and ResourceOption get linked to Go API docs.
//
// For Java we could link to JavaDoc when appropriate.
func (d DocLanguageHelper) GetDocLinkForPulumiType(_ *schema.Package, _ string) string {
	return ""
}

func (d DocLanguageHelper) GetDocLinkForResourceType(_ *schema.Package, _, _ string) string {
	panic("Not implemented")
}

func (d DocLanguageHelper) GetDocLinkForResourceInputOrOutputType(
	_ *schema.Package, _, _ string, _ bool,
) string {
	panic("Not implemented")
}

func (d DocLanguageHelper) GetDocLinkForFunctionInputOrOutputType(
	_ *schema.Package, _, _ string, _ bool,
) string {
	panic("Not implemented")
}

func (d DocLanguageHelper) GetModuleDocLink(_ *schema.Package, _ string) (string, string) {
	panic("Not implemented")
}
