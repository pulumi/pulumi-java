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

// unionFactory returns the fully qualified static factory that wraps value as a member of the union
// elementType at the current location, or "" when the union has no interface.
func (g *generator) unionFactory(elementType schema.Type, value model.Expression) string {
	union, ok := codegen.UnwrapType(elementType).(*schema.UnionType)
	if !ok || g.unionPackage == nil {
		return ""
	}
	form, ok := g.registryFor(g.unionPackage).lookup(g.unionLocation, union)
	if !ok {
		return ""
	}
	factory := "of"
	for _, m := range form.spec.members {
		if m.object == nil && m.factory != "of" && admits(flattenUnion(union)[memberIndex(union, m.key)], value.Type()) {
			factory = m.factory
			break
		}
	}
	parts := strings.Split(form.spec.ownerToken, ":")
	ref := g.unionPackage.Reference()
	pkg := extensionPackageName(parts[0], ref)
	className := names.Ident(form.spec.name + "Args").String()
	return pulumiInputImport(pkg, parts[1], className, ref.Namespace()) + "." + factory
}

// admits reports whether a value of the PCL type v can be a value of the schema type t. It tells
// apart members that share an erased Java type.
func admits(t schema.Type, v model.Type) bool {
	v = unwrapOptional(model.ResolveOutputs(v))
	switch t := codegen.UnwrapType(t).(type) {
	case *schema.ArrayType:
		switch v := v.(type) {
		case *model.ListType:
			return admits(t.ElementType, v.ElementType)
		case *model.TupleType:
			for _, e := range v.ElementTypes {
				if !admits(t.ElementType, e) {
					return false
				}
			}
			return true
		}
		return false
	case *schema.MapType:
		switch v := v.(type) {
		case *model.MapType:
			return admits(t.ElementType, v.ElementType)
		case *model.ObjectType:
			for _, e := range v.Properties {
				if !admits(t.ElementType, e) {
					return false
				}
			}
			return true
		}
		return false
	case *schema.EnumType:
		return admits(t.ElementType, v)
	}
	switch codegen.UnwrapType(t) {
	case schema.StringType:
		return v == model.StringType
	case schema.IntType:
		return v == model.IntType
	case schema.NumberType:
		return v == model.NumberType || v == model.IntType
	case schema.BoolType:
		return v == model.BoolType
	}
	return true
}

// genUnionElement generates a list or map element of a union interface, wrapping a non-object
// value with the interface factory. It reports false when the element is not part of a union
// interface.
func (g *generator) genUnionElement(w io.Writer, elementType schema.Type, value model.Expression) bool {
	if call, ok := value.(*model.FunctionCallExpression); ok && call.Name == pcl.IntrinsicConvert {
		if _, isObject := call.Args[0].(*model.ObjectConsExpression); isObject {
			// The binder has resolved the object to its member, and the conversion lowers it as such.
			g.Fgenf(w, "%.v", value)
			return true
		}
	}
	if object, ok := value.(*model.ObjectConsExpression); ok {
		if union, isUnion := codegen.UnwrapType(elementType).(*schema.UnionType); isUnion {
			g.genObjectConsExpression(w, object, pickTypeFromUnion(union, object))
			return true
		}
	}
	factory := g.unionFactory(elementType, value)
	if factory == "" {
		return false
	}
	g.Fgenf(w, "%s(%.v)", factory, value)
	return true
}

// genUnionArgument generates the value of a property. An output whose element is a wrapped member
// of the property's union interface is lifted into the interface with the interface factory.
func (g *generator) genUnionArgument(w io.Writer, propertyType schema.Type, value model.Expression) {
	factory := g.unionFactory(propertyType)
	source := value
	for {
		call, ok := source.(*model.FunctionCallExpression)
		if !ok || call.Name != pcl.IntrinsicConvert {
			break
		}
		source = call.Args[0]
	}
	_, isObject := unwrapOptional(model.ResolveOutputs(source.Type())).(*model.ObjectType)
	if factory == "" || !model.ContainsOutputs(source.Type()) || isObject {
		g.Fgenf(w, "%.v", value)
		return
	}
	g.Fgenf(w, "%.v.applyValue(%s)", value, strings.TrimSuffix(factory, ".of")+"::of")
}
