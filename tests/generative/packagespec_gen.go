// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package generative

import (
	"fmt"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"pgregory.net/rapid"
)

type schemaGenerators struct{}

func (g *schemaGenerators) PackageSpec() *rapid.Generator {
	return rapid.Custom(func(t *rapid.T) pschema.PackageSpec {
		pkgName := g.Identifier().Draw(t, "pkgName").(string)
		tokenGen := g.ResourceToken(pkgName)
		resourcesGen := rapid.MapOf(tokenGen, g.ResourceSpec(pkgName))

		resources := resourcesGen.Draw(t, "resources").(map[string]pschema.ResourceSpec)

		return pschema.PackageSpec{
			Name:      pkgName,
			Resources: resources,
		}
	})
}

func (g *schemaGenerators) Resources(pkgName string) *rapid.Generator {
	return rapid.Custom(func(t *rapid.T) map[string]pschema.ResourceSpec {
		tokensGen := rapid.SliceOfDistinct(g.ResourceToken(pkgName), func(x string) string { return x })
		tokens := tokensGen.Draw(t, "tokens").([]string)
		resourcesGen := rapid.SliceOfN(g.ResourceSpec(pkgName), len(tokens), len(tokens))
		resources := resourcesGen.Draw(t, "resources").([]pschema.ResourceSpec)
		result := map[string]pschema.ResourceSpec{}
		for i, t := range tokens {
			result[t] = resources[i]
		}
		return result
	})
}

func (schemaGenerators) Identifier() *rapid.Generator {
	return rapid.StringMatching(`[a-zA-Z_][a-zA-Z0-9_]*`)
}

func (g *schemaGenerators) ResourceToken(pkgName string) *rapid.Generator {
	modName := "module1" // TODO no module, more than one module, combinations.
	nameGen := g.Identifier()

	return rapid.Custom(func(t *rapid.T) string {
		resourceName := nameGen.Draw(t, "resourceName").(string)
		return fmt.Sprintf("%s:%s:%s", pkgName, modName, resourceName)
	})
}

func (g *schemaGenerators) ResourceSpec(pkgName string) *rapid.Generator {
	objectTypeSpecGen := g.ObjectTypeSpec()
	return rapid.Custom(func(t *rapid.T) pschema.ResourceSpec {
		objectTypeSpec := objectTypeSpecGen.Draw(t, "objectTypeSpec").(pschema.ObjectTypeSpec)
		// TODO IsComponent: true case, etc
		return pschema.ResourceSpec{
			ObjectTypeSpec: objectTypeSpec,
		}
	})
}

func (g *schemaGenerators) ObjectTypeSpec() *rapid.Generator {
	propertyNameGen := g.Identifier()
	propGen := g.PropertySpec()
	propertiesGen := rapid.MapOf(propertyNameGen, propGen)
	return rapid.Custom(func(t *rapid.T) pschema.ObjectTypeSpec {
		properties := propertiesGen.Draw(t, "properties").(map[string]pschema.PropertySpec)
		return pschema.ObjectTypeSpec{
			Properties: properties,
		}
	})
}

func (g schemaGenerators) PropertySpec() *rapid.Generator {
	typeGen := g.TypeSpec()
	return rapid.Custom(func(t *rapid.T) pschema.PropertySpec {
		typeSpec := typeGen.Draw(t, "typeSpec").(pschema.TypeSpec)
		return pschema.PropertySpec{
			TypeSpec: typeSpec,
		}
	})
}

func (schemaGenerators) TypeSpec() *rapid.Generator {
	baseTypes := []pschema.TypeSpec{
		{Type: "boolean"},
		{Type: "integer"},
		{Type: "number"},
		{Type: "string"},
		{Ref: "pulumi.json#/Any"},
		{Ref: "pulumi.json#/Archive"},
		{Ref: "pulumi.json#/Asset"},
		{Ref: "pulumi.json#/Json"},
	}

	// For reference on representations schema.go has a useful function:
	//
	// func (pkg *Package) marshalType(t Type, plain bool) TypeSpec {

	// TODO object
	// TODO input
	// TODO array
	// TODO map

	options := []*rapid.Generator{}
	for _, t := range baseTypes {
		options = append(options, rapid.Just(t))
	}

	return rapid.OneOf(options...)
}
