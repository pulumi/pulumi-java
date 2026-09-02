// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"sort"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/stretchr/testify/require"
)

// unionTestPackage binds a package with three object variants and one resource per entry of
// unions, each with a unionOf input property that holds the listed variants.
func unionTestPackage(t *testing.T, unions map[string][]int) *schema.Package {
	t.Helper()
	variant := func(n int) schema.ComplexTypeSpec {
		return schema.ComplexTypeSpec{ObjectTypeSpec: schema.ObjectTypeSpec{
			Type: "object",
			Properties: map[string]schema.PropertySpec{
				"kind":  {TypeSpec: schema.TypeSpec{Type: "string"}, Const: fmt.Sprintf("v%d", n)},
				"field": {TypeSpec: schema.TypeSpec{Type: "string"}},
			},
			Required: []string{"kind"},
		}}
	}
	spec := schema.PackageSpec{
		Name:    "test",
		Version: "0.0.1",
		Types: map[string]schema.ComplexTypeSpec{
			"test:index:Variant1": variant(1),
			"test:index:Variant2": variant(2),
			"test:index:Variant3": variant(3),
		},
		Resources: map[string]schema.ResourceSpec{},
		Language:  map[string]schema.RawMessage{"java": schema.RawMessage(`{"fullyTypedUnions": true}`)},
	}
	for name, variants := range unions {
		var oneOf []schema.TypeSpec
		for _, n := range variants {
			oneOf = append(oneOf, schema.TypeSpec{Ref: fmt.Sprintf("#/types/test:index:Variant%d", n)})
		}
		spec.Resources["test:index:"+name] = schema.ResourceSpec{
			InputProperties: map[string]schema.PropertySpec{"unionOf": {TypeSpec: schema.TypeSpec{OneOf: oneOf}}},
			ObjectTypeSpec: schema.ObjectTypeSpec{
				Properties: map[string]schema.PropertySpec{"scalar": {TypeSpec: schema.TypeSpec{
					OneOf: []schema.TypeSpec{{Type: "string"}, {Type: "integer"}},
				}}},
			},
		}
	}
	pkg, err := schema.ImportSpec(spec, map[string]schema.Language{"java": Importer},
		schema.NewNullLoader(), schema.ValidationOptions{})
	require.NoError(t, err)
	return pkg
}

func specNames(specs []*unionSpec) []string {
	names := make([]string, 0, len(specs))
	for _, s := range specs {
		names = append(names, s.name)
	}
	sort.Strings(names)
	return names
}

func TestRegisterUnionsNamesEachLocation(t *testing.T) {
	t.Parallel()

	pkg := unionTestPackage(t, map[string][]int{"A": {1, 2}, "B": {1, 2}, "C": {1, 2, 3}})
	reg := registerUnions(pkg, true)

	byName := map[string]*unionSpec{}
	for _, spec := range reg.specs {
		byName[spec.name] = spec
	}
	require.Equal(t, []string{"AScalar", "AUnionOf", "BScalar", "BUnionOf", "CScalar", "CUnionOf"}, specNames(reg.specs))

	require.Equal(t, []string{"BUnionOf"}, specNames(byName["AUnionOf"].equals))
	require.Equal(t, []string{"CUnionOf"}, specNames(byName["AUnionOf"].supersets))
	require.Empty(t, byName["CUnionOf"].equals)
	require.Empty(t, byName["CUnionOf"].supersets)
	require.Equal(t, []string{"BScalar", "CScalar"}, specNames(byName["AScalar"].equals))

	// The variants implement every union they belong to.
	require.Equal(t, []string{"AUnionOf", "BUnionOf", "CUnionOf"}, specNames(reg.byMember["test:index:Variant1"]))
	require.Equal(t, []string{"CUnionOf"}, specNames(reg.byMember["test:index:Variant3"]))

	// The input shape of a location is bound, the output shape is not.
	require.True(t, byName["AUnionOf"].bound(formKey{inputsQualifier, true}))
	require.False(t, byName["AUnionOf"].bound(formKey{outputsQualifier, false}))
	require.True(t, byName["AScalar"].bound(formKey{outputsQualifier, false}))
}

func TestRegisterUnionsKeepsNamesWhenAMemberIsAdded(t *testing.T) {
	t.Parallel()

	pkg := unionTestPackage(t, map[string][]int{"A": {1, 2}, "B": {1, 2, 3}, "C": {1, 2, 3}})
	reg := registerUnions(pkg, true)

	byName := map[string]*unionSpec{}
	for _, spec := range reg.specs {
		byName[spec.name] = spec
	}
	require.Contains(t, byName, "AUnionOf")
	require.Contains(t, byName, "BUnionOf")
	require.Contains(t, byName, "CUnionOf")
	require.Empty(t, byName["AUnionOf"].equals)
	require.Equal(t, []string{"BUnionOf", "CUnionOf"}, specNames(byName["AUnionOf"].supersets))
	require.Equal(t, []string{"CUnionOf"}, specNames(byName["BUnionOf"].equals))
}
