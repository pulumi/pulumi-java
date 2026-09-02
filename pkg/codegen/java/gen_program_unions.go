// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"io"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// registryFor returns the union interfaces of pkg, computed once per program.
func (g *generator) registryFor(pkg *schema.Package) *unionRegistry {
	if reg, ok := g.unions[pkg]; ok {
		return reg
	}
	info, _ := pkg.Language["java"].(PackageInfo)
	reg := registerUnions(pkg, info.FullyTypedUnions)
	g.unions[pkg] = reg
	return reg
}

// withUnionLocation sets the property, and the package that declares it, whose value is being
// generated. It returns the function that restores the previous location.
func (g *generator) withUnionLocation(pkg schema.PackageReference, property *schema.Property) func() {
	previousPackage, previousLocation := g.unionPackage, g.unionLocation
	g.unionPackage, g.unionLocation = nil, property
	if pkg != nil {
		if def, err := pkg.Definition(); err == nil {
			g.unionPackage = def
		}
	}
	return func() { g.unionPackage, g.unionLocation = previousPackage, previousLocation }
}

// unionFactory returns the fully qualified static factory of the input interface generated for the
// union elementType at the current location, or "" when the union has no interface.
func (g *generator) unionFactory(elementType schema.Type) string {
	union, ok := codegen.UnwrapType(elementType).(*schema.UnionType)
	if !ok || g.unionPackage == nil {
		return ""
	}
	form, ok := g.registryFor(g.unionPackage).lookup(g.unionLocation, union)
	if !ok {
		return ""
	}
	parts := strings.Split(form.spec.ownerToken, ":")
	ref := g.unionPackage.Reference()
	pkg := extensionPackageName(parts[0], ref)
	className := names.Ident(form.spec.name + "Args").String()
	return pulumiInputImport(pkg, parts[1], className, ref.Namespace()) + ".of"
}

// genUnionElement generates a list or map element of a union, wrapping a non-object value with the
// factory of the union's interface.
func (g *generator) genUnionElement(w io.Writer, factory string, elementType schema.Type, value model.Expression) {
	if call, ok := value.(*model.FunctionCallExpression); ok && call.Name == pcl.IntrinsicConvert {
		if _, isObject := call.Args[0].(*model.ObjectConsExpression); isObject {
			// The binder has resolved the object to its member, and the conversion lowers it as such.
			g.Fgenf(w, "%.v", value)
			return
		}
	}
	if object, ok := value.(*model.ObjectConsExpression); ok {
		if union, isUnion := codegen.UnwrapType(elementType).(*schema.UnionType); isUnion {
			g.genObjectConsExpression(w, object, pickTypeFromUnion(union, object))
			return
		}
	}
	if factory == "" {
		g.Fgenf(w, "%.v", value)
		return
	}
	g.Fgenf(w, "%s(%.v)", factory, value)
}
