// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"context"
	"errors"
	"fmt"
	"maps"
	"slices"
	"testing"

	"github.com/blang/semver"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

// unionTestLoader keeps these tests independent of the pulumi submodule and of any package that is
// not defined inline in the test.
type unionTestLoader struct{}

func (unionTestLoader) LoadPackage(string, *semver.Version) (*schema.Package, error) {
	return nil, errors.New("external packages are not available in this test")
}

func (unionTestLoader) LoadPackageV2(context.Context, *schema.PackageDescriptor) (*schema.Package, error) {
	return nil, errors.New("external packages are not available in this test")
}

func (unionTestLoader) LoadPackageReference(string, *semver.Version) (schema.PackageReference, error) {
	return nil, errors.New("external packages are not available in this test")
}

func (unionTestLoader) LoadPackageReferenceV2(
	context.Context, *schema.PackageDescriptor,
) (schema.PackageReference, error) {
	return nil, errors.New("external packages are not available in this test")
}

const javaSrcRoot = "src/main/java/com/pulumi/union/"

func generateUnionTestPackage(t *testing.T, spec schema.PackageSpec) map[string]string {
	t.Helper()

	pkg, diags, err := schema.BindSpec(spec, unionTestLoader{}, schema.ValidationOptions{})
	require.NoError(t, err)
	require.False(t, diags.HasErrors(), "%v", diags)

	generated, err := GeneratePackage("test", pkg, nil, nil, false, false)
	require.NoError(t, err)

	files := map[string]string{}
	for name, contents := range generated {
		files[name] = string(contents)
	}
	return files
}

func requireFile(t *testing.T, files map[string]string, name string) string {
	t.Helper()
	contents, ok := files[javaSrcRoot+name]
	require.True(t, ok, "%s was not generated, got %v", name, slices.Sorted(maps.Keys(files)))
	return contents
}

// unionTestSpec builds a package with a discriminated union over the first variantCount variants,
// plus, when subsetCount is non-zero, a second resource holding a union over the first subsetCount
// of the same variants.
func unionTestSpec(variantCount, subsetCount int, discriminator string) schema.PackageSpec {
	stringT := schema.TypeSpec{Type: "string"}

	types := map[string]schema.ComplexTypeSpec{}
	oneOf := make([]schema.TypeSpec, 0, variantCount)
	mapping := map[string]string{}
	for i := 1; i <= variantCount; i++ {
		token := fmt.Sprintf("union:index:Variant%d", i)
		tag := fmt.Sprintf("variant%d", i)
		properties := map[string]schema.PropertySpec{
			"payload": {TypeSpec: stringT},
		}
		var required []string
		if discriminator != "" {
			properties[discriminator] = schema.PropertySpec{TypeSpec: stringT, Const: tag}
			required = append(required, discriminator)
		}
		types[token] = schema.ComplexTypeSpec{ObjectTypeSpec: schema.ObjectTypeSpec{
			Type:       "object",
			Properties: properties,
			Required:   required,
		}}
		oneOf = append(oneOf, schema.TypeSpec{Ref: "#/types/" + token})
		mapping[tag] = "#/types/" + token
	}

	union := schema.TypeSpec{OneOf: oneOf}
	if discriminator != "" {
		union.Discriminator = &schema.DiscriminatorSpec{PropertyName: discriminator, Mapping: mapping}
	}

	properties := map[string]schema.PropertySpec{
		"unionOf":        {TypeSpec: union},
		"arrayOfUnionOf": {TypeSpec: schema.TypeSpec{Type: "array", Items: &union}},
	}
	resources := map[string]schema.ResourceSpec{
		"union:index:Example": {
			ObjectTypeSpec:  schema.ObjectTypeSpec{Type: "object", Properties: properties},
			InputProperties: properties,
		},
	}

	if subsetCount > 0 {
		subsetMapping := map[string]string{}
		for i := 1; i <= subsetCount; i++ {
			tag := fmt.Sprintf("variant%d", i)
			subsetMapping[tag] = mapping[tag]
		}
		subset := schema.TypeSpec{
			OneOf: oneOf[:subsetCount],
			Discriminator: &schema.DiscriminatorSpec{
				PropertyName: discriminator,
				Mapping:      subsetMapping,
			},
		}
		subsetProperties := map[string]schema.PropertySpec{"unionOf": {TypeSpec: subset}}
		resources["union:index:SubsetExample"] = schema.ResourceSpec{
			ObjectTypeSpec:  schema.ObjectTypeSpec{Type: "object", Properties: subsetProperties},
			InputProperties: subsetProperties,
		}
	}

	return schema.PackageSpec{
		Name:      "union",
		Version:   "1.0.0",
		Types:     types,
		Resources: resources,
	}
}

// withFullyTypedUnions opts the package in to generated union interfaces.
func withFullyTypedUnions(spec schema.PackageSpec) schema.PackageSpec {
	spec.Language = map[string]schema.RawMessage{
		"java": schema.RawMessage(`{"fullyTypedUnions": true}`),
	}
	return spec
}

func TestGenerateDiscriminatedUnionInterface(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, withFullyTypedUnions(unionTestSpec(4, 0, "discriminantKind")))

	outputs := requireFile(t, files, "outputs/IExampleUnionOf.java")
	assert.Contains(t, outputs, `import com.pulumi.core.annotations.DiscriminatedUnion;`)
	assert.Contains(t, outputs, `@DiscriminatedUnion("discriminantKind")`)
	assert.Contains(t, outputs, `@DiscriminatedUnion.Case(tag = "variant1", type = Variant1.class)`)
	assert.Contains(t, outputs, `@DiscriminatedUnion.Case(tag = "variant4", type = Variant4.class)`)
	assert.Contains(t, outputs, "public interface IExampleUnionOf {")

	inputs := requireFile(t, files, "inputs/IExampleUnionOfArgs.java")
	assert.Contains(t, inputs, `@DiscriminatedUnion("discriminantKind")`)
	assert.Contains(t, inputs, `@DiscriminatedUnion.Case(tag = "variant1", type = Variant1Args.class)`)
	assert.Contains(t, inputs, "public interface IExampleUnionOfArgs {")

	assert.Contains(t, requireFile(t, files, "outputs/Variant1.java"),
		"public final class Variant1 implements IExampleUnionOf {")
	assert.Contains(t, requireFile(t, files, "inputs/Variant1Args.java"),
		"public final class Variant1Args extends com.pulumi.resources.ResourceArgs implements IExampleUnionOfArgs {")

	// The union-typed properties are typed as the interface, not as java.lang.Object.
	example := requireFile(t, files, "Example.java")
	assert.Contains(t, example, "private Output</* @Nullable */ IExampleUnionOf> unionOf;")
	assert.Contains(t, example, "private Output</* @Nullable */ List<IExampleUnionOf>> arrayOfUnionOf;")
	assert.NotContains(t, example, "Object")

	args := requireFile(t, files, "ExampleArgs.java")
	assert.Contains(t, args, "private @Nullable Output<IExampleUnionOfArgs> unionOf;")
	assert.Contains(t, args, "private @Nullable Output<List<IExampleUnionOfArgs>> arrayOfUnionOf;")
	assert.NotContains(t, args, "Output<Object>")
}

func TestGenerateConstValuedProperty(t *testing.T) {
	t.Parallel()

	// Const initialization does not depend on the union opt-in.
	files := generateUnionTestPackage(t, unionTestSpec(4, 0, "discriminantKind"))

	// The discriminator tag is a constant, so the generated builder fills it in and the caller never
	// has to write it by hand. A value the caller did pass still wins, through .arg.
	variant := requireFile(t, files, "inputs/Variant1Args.java")
	assert.Contains(t, variant,
		`$.discriminantKind = Codegen.stringProp("discriminantKind").output()`+
			`.arg($.discriminantKind).def("variant1").require();`)
	assert.Contains(t, requireFile(t, files, "inputs/Variant4Args.java"), `.def("variant4")`)

	// A const property must not be rejected for being absent.
	assert.NotContains(t, variant, "MissingRequiredPropertyException(\"Variant1Args\", \"discriminantKind\")")
}

func TestGenerateDiscriminatedUnionSubsetInterface(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, withFullyTypedUnions(unionTestSpec(4, 3, "discriminantKind")))

	// A value of the narrower union must assign into a slot typed as the wider one.
	assert.Contains(t, requireFile(t, files, "outputs/ISubsetExampleUnionOf.java"),
		"public interface ISubsetExampleUnionOf extends IExampleUnionOf {")
	assert.Contains(t, requireFile(t, files, "inputs/ISubsetExampleUnionOfArgs.java"),
		"public interface ISubsetExampleUnionOfArgs extends IExampleUnionOfArgs {")

	// Members of both unions name only the narrower interface, which already extends the wider one.
	assert.Contains(t, requireFile(t, files, "outputs/Variant1.java"),
		"public final class Variant1 implements ISubsetExampleUnionOf {")
	assert.Contains(t, requireFile(t, files, "outputs/Variant4.java"),
		"public final class Variant4 implements IExampleUnionOf {")

	assert.Contains(t, requireFile(t, files, "SubsetExample.java"),
		"private Output</* @Nullable */ ISubsetExampleUnionOf> unionOf;")
	assert.Contains(t, requireFile(t, files, "SubsetExampleArgs.java"),
		"private @Nullable Output<ISubsetExampleUnionOfArgs> unionOf;")
}

// Without the opt-in every union keeps the shape it has today, however many members it has.
func TestGenerateDiscriminatedUnionRequiresOptIn(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, unionTestSpec(4, 0, "discriminantKind"))

	for name := range files {
		assert.NotContains(t, name, "IExample", "no interface should be generated without the opt-in")
	}
	assert.Contains(t, requireFile(t, files, "Example.java"),
		"private Output</* @Nullable */ Object> unionOf;")
	assert.Contains(t, requireFile(t, files, "ExampleArgs.java"),
		"private @Nullable Output<Object> unionOf;")
}

// Member count never decides the shape: the opt-in covers a two-member union as well, so adding a
// third member to one is not a breaking change.
func TestGenerateTwoMemberDiscriminatedUnionInterface(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, withFullyTypedUnions(unionTestSpec(2, 0, "discriminantKind")))

	outputs := requireFile(t, files, "outputs/IExampleUnionOf.java")
	assert.Contains(t, outputs, `@DiscriminatedUnion("discriminantKind")`)
	assert.Contains(t, outputs, `@DiscriminatedUnion.Case(tag = "variant1", type = Variant1.class)`)
	assert.Contains(t, outputs, `@DiscriminatedUnion.Case(tag = "variant2", type = Variant2.class)`)

	example := requireFile(t, files, "Example.java")
	assert.Contains(t, example, "private Output</* @Nullable */ IExampleUnionOf> unionOf;")
	assert.NotContains(t, example, "Either")
	assert.Contains(t, requireFile(t, files, "ExampleArgs.java"),
		"private @Nullable Output<IExampleUnionOfArgs> unionOf;")
}

// Without the opt-in a two-member union keeps Either<L, R>.
func TestGenerateTwoMemberDiscriminatedUnionIsUnchanged(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, unionTestSpec(2, 0, "discriminantKind"))

	for name := range files {
		assert.NotContains(t, name, "IExample", "no interface should be generated for a two-member union")
	}

	assert.Contains(t, requireFile(t, files, "Example.java"),
		"private Output</* @Nullable */ Either<Variant1,Variant2>> unionOf;")
	assert.Contains(t, requireFile(t, files, "ExampleArgs.java"),
		"private @Nullable Output<Either<Variant1Args,Variant2Args>> unionOf;")
	assert.Contains(t, requireFile(t, files, "outputs/Variant1.java"), "public final class Variant1 {")
}

func TestGenerateUndiscriminatedUnionIsUnchanged(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, withFullyTypedUnions(unionTestSpec(4, 0, "")))

	for name := range files {
		assert.NotContains(t, name, "IExample", "no interface should be generated without a discriminator")
	}
	assert.Contains(t, requireFile(t, files, "Example.java"),
		"private Output</* @Nullable */ Object> unionOf;")
	assert.Contains(t, requireFile(t, files, "Example.java"),
		"private Output</* @Nullable */ List<Object>> arrayOfUnionOf;")
}

// A schema may name the discriminator with a trailing double underscore, as the
// l2-discriminated-union-internal conformance fixture does. Only a leading double underscore marks a
// property as engine-internal, so this one round-trips.
func TestGenerateTrailingUnderscoreDiscriminator(t *testing.T) {
	t.Parallel()

	files := generateUnionTestPackage(t, withFullyTypedUnions(unionTestSpec(3, 0, "type__")))

	outputs := requireFile(t, files, "outputs/IExampleUnionOf.java")
	assert.Contains(t, outputs, `@DiscriminatedUnion("type__")`)
	assert.Contains(t, outputs, `@DiscriminatedUnion.Case(tag = "variant1", type = Variant1.class)`)
	assert.Contains(t, requireFile(t, files, "inputs/Variant1Args.java"), `.def("variant1")`)
}

// A union whose members do not all appear in the discriminator mapping cannot be dispatched on, so
// it keeps the untyped shape it has always had.
func TestGenerateIncompleteMappingIsUnchanged(t *testing.T) {
	t.Parallel()

	spec := withFullyTypedUnions(unionTestSpec(4, 0, "discriminantKind"))
	union := spec.Resources["union:index:Example"].InputProperties["unionOf"]
	delete(union.Discriminator.Mapping, "variant4")

	files := generateUnionTestPackage(t, spec)

	for name := range files {
		assert.NotContains(t, name, "IExample", "no interface should be generated for an incomplete mapping")
	}
	assert.Contains(t, requireFile(t, files, "Example.java"),
		"private Output</* @Nullable */ Object> unionOf;")
}

func TestGenerateDiscriminatedUnionRejectsUncoveredMember(t *testing.T) {
	t.Parallel()

	// Two tags naming the same member leaves another member uncovered while keeping the tag and
	// member counts equal. Such a union must not generate an interface, because tag dispatch
	// could never reach the uncovered member.
	spec := withFullyTypedUnions(unionTestSpec(3, 0, "discriminantKind"))
	union := spec.Resources["union:index:Example"].InputProperties["unionOf"]
	union.Discriminator.Mapping["variant2"] = "#/types/union:index:Variant1"
	spec.Resources["union:index:Example"].InputProperties["unionOf"] = union

	files := generateUnionTestPackage(t, spec)

	_, ok := files[javaSrcRoot+"outputs/IExampleUnionOf.java"]
	assert.False(t, ok, "a union whose mapping leaves a member uncovered must not generate an interface")
}

func TestGenerateDiscriminatedUnionEscapesTags(t *testing.T) {
	t.Parallel()

	// Discriminator names and tags come from the schema and land inside Java string literals.
	spec := withFullyTypedUnions(unionTestSpec(3, 0, `kind"x`))
	files := generateUnionTestPackage(t, spec)

	outputs := requireFile(t, files, "outputs/IExampleUnionOf.java")
	assert.Contains(t, outputs, `@DiscriminatedUnion("kind\"x")`,
		"the discriminator name must be escaped, not terminate the literal")
	assert.NotContains(t, outputs, `@DiscriminatedUnion("kind"x")`)
}
