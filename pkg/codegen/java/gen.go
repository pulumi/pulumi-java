// Copyright 2022, Pulumi Corporation.  All rights reserved.

//nolint:goconst
package java

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"path"
	"path/filepath"
	"reflect"
	"slices"
	"sort"
	"strconv"
	"strings"

	"github.com/blang/semver"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"golang.org/x/exp/maps"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// This should be bumped as required at the point of release.
var DefaultSdkVersion = semver.Version{Major: 1, Minor: 0, Patch: 0}

func packageName(packages map[string]string, name string) string {
	if pkg, ok := packages[name]; ok {
		return pkg
	}

	return name
}

type modContext struct {
	pkg                    schema.PackageReference
	mod                    string
	propertyNames          map[*schema.Property]string
	types                  []*schema.ObjectType
	typesByName            map[string]*schema.ObjectType
	enums                  []*schema.EnumType
	resources              []*schema.Resource
	functions              []*schema.Function
	children               []*modContext
	tool                   string
	packageName            string
	rootPackageName        string
	basePackageName        string
	packages               map[string]string
	configClassPackageName string
	classQueue             *classQueue
}

func (mod *modContext) propertyName(p *schema.Property) string {
	if n, ok := mod.propertyNames[p]; ok {
		return n
	}
	return p.Name
}

func tokenToName(tok string) string {
	// token := pkg : module : member
	// module := path/to/module

	components := strings.Split(tok, ":")
	contract.Assertf(len(components) == 3, "malformed token %v", tok)
	return names.Ident(names.Title(components[2])).String()
}

func resourceName(r *schema.Resource) string {
	if r.IsProvider {
		return "Provider"
	}
	return tokenToName(r.Token)
}

func tokenToFunctionName(tok string) string {
	return names.LowerCamelCase(tokenToName(tok))
}

func tokenToFunctionResultClassName(mod *modContext, tok string) names.Ident {
	suffixes := []string{"Result", "InvokeResult"}
	name := tokenToName(tok)
	for _, suffix := range suffixes {
		conflict := false
		if mod != nil {
			_, conflict = mod.typesByName[name+suffix]
		}
		if !conflict {
			return names.Ident(name + suffix)
		}
	}
	contract.Failf("cannot find an unambigious class name for the %s"+
		"function result: tried suffixing with %v", tok, suffixes)
	return names.Ident("")
}

func (mod *modContext) tokenToPackage(tok string, qualifier qualifier) string {
	components := strings.Split(tok, ":")
	contract.Assertf(len(components) == 3, "malformed token %v", tok)

	pkg := mod.basePackageName

	if qualifier == policiesQualifier {
		pkg += qualifier.String() + "."
	}

	pkg += packageName(mod.packages, components[0])
	pkgName := mod.pkg.TokenToModule(tok)

	if pkgName != "" {
		pkg += "." + packageName(mod.packages, pkgName)
	}

	switch qualifier {
	case noQualifier, policiesQualifier:
		return pkg
	default:
		return fmt.Sprintf("%s.%s", pkg, qualifier.String())
	}
}

func (mod *modContext) typeName(t *schema.ObjectType) string {
	name := tokenToName(t.Token)
	if t.IsInputShape() {
		return name + "Args"
	}
	return name
}

// Computes the TypeShape (Java type representation, possibly
// generic), corresponding to the given type.
//
// Side effects: calls mod.classQueue.ensureGenerated so that it would
// never generate dangling references. The queue is polled later in
// the codegen to make sure every referenced type has a corresponding
// class generated. This method generates the minimal viable set of
// classes closed over referencees, without having to know exactly
// what to visit.
func (mod *modContext) typeString(
	ctx *classFileContext,
	t schema.Type,
	qualifier qualifier,
	input bool,
	// Influences how Map and Array types are generated.
	requireInitializers bool,
	// Allow returning `Optional<T>` directly. Otherwise `@Nullable T` will be returned at the outer scope.
	outerOptional bool,
	// Called in the context of an overload without an `Output<T>` wrapper. We
	// should act like we are inside an Output<T>.
	inputlessOverload bool,
) TypeShape {
	inner := mod.typeStringRecHelper(ctx, t, qualifier, input, requireInitializers, inputlessOverload)
	if inner.Type.Equal(names.Optional) && !outerOptional {
		contract.Assertf(len(inner.Parameters) == 1,
			"Optional must have exactly one parameter, got %v", len(inner.Parameters))
		contract.Assertf(len(inner.Annotations) == 0,
			"Optional must have no annotations, got %v", len(inner.Annotations))
		inner = inner.Parameters[0]
		inner.Annotations = append(inner.Annotations, fmt.Sprintf("@%s", ctx.ref(names.Nullable)))
	}
	return inner
}

// A facilitator function for the inner recursion of `typeString`.
func (mod *modContext) typeStringRecHelper(
	ctx *classFileContext,
	t schema.Type,
	qualifier qualifier,
	input bool,
	requireInitializers bool,
	insideInput bool,
) TypeShape {
	switch t := t.(type) {
	case *schema.InputType:
		elem := t.ElementType
		switch t.ElementType.(type) {
		case *schema.ArrayType, *schema.MapType:
			elem = codegen.PlainType(t.ElementType)
		}
		inner := mod.typeStringRecHelper(ctx, elem, qualifier, true, requireInitializers, true)

		// Simplify Output<Output<T>> to Output<T> here. This is
		// safe to do since:
		//
		//     serialize(Output<Output<T>> x) == serialize(Output<T> x)
		if inner.Type.Equal(names.Output) {
			return inner
		}
		return TypeShape{
			Type:       names.Output,
			Parameters: []TypeShape{inner},
		}

	case *schema.OptionalType:
		inner := mod.typeStringRecHelper(ctx, t.ElementType, qualifier, input, requireInitializers, insideInput)
		if ignoreOptional(t, requireInitializers) {
			inner.Annotations = append(inner.Annotations, fmt.Sprintf("@%s", ctx.ref(names.Nullable)))
			return inner
		}
		return TypeShape{
			Type:       names.Optional,
			Parameters: []TypeShape{inner},
		}

	case *schema.EnumType:
		return mod.typeStringForEnumType(t)

	case *schema.ArrayType:
		listType := names.List
		if requireInitializers {
			listType = names.List
		}

		return TypeShape{
			Type: listType,
			Parameters: []TypeShape{
				mod.typeStringRecHelper(ctx,
					codegen.PlainType(t.ElementType), qualifier, input, false, insideInput,
				),
			},
		}

	case *schema.MapType:
		mapType := names.Map
		if requireInitializers {
			mapType = names.Map
		}

		return TypeShape{
			Type: mapType,
			Parameters: []TypeShape{
				{Type: names.String},
				mod.typeStringRecHelper(ctx,
					codegen.PlainType(t.ElementType), qualifier, input, false, insideInput,
				),
			},
		}

	case *schema.ObjectType:
		return mod.typeStringForObjectType(t, qualifier, input)
	case *schema.ResourceType:
		var resourceType names.FQN
		if strings.HasPrefix(t.Token, "pulumi:providers:") {
			pkgName := strings.TrimPrefix(t.Token, "pulumi:providers:")
			rawPkg := fmt.Sprintf("%s%s", mod.basePackageName, packageName(mod.packages, pkgName))
			pkg, err := parsePackageName(rawPkg)
			if err != nil {
				panic(err)
			}
			resourceType = pkg.Dot(names.Ident("Provider"))
		} else {
			namingCtx := mod
			if t.Resource != nil && !codegen.PkgEquals(t.Resource.PackageReference, mod.pkg) {
				// If resource type belongs to another package, we apply naming conventions from that package,
				// including package naming and compatibility mode.
				extPkg := t.Resource.PackageReference
				var info PackageInfo
				extDef, err := extPkg.Definition()
				contract.AssertNoErrorf(err, "failed to load package definition for %q", extPkg.Name())
				contract.AssertNoErrorf(extDef.ImportLanguages(map[string]schema.Language{"java": Importer}),
					"failed to load java language plugin for %q", extPkg.Name())
				if v, ok := extDef.Language["java"].(PackageInfo); ok {
					info = v
				}
				namingCtx = &modContext{
					pkg:             extPkg,
					packages:        info.Packages,
					basePackageName: info.BasePackageOrDefault(),
				}
			}
			pkg, err := parsePackageName(namingCtx.tokenToPackage(t.Token, noQualifier))
			if err != nil {
				panic(err)
			}
			resourceType = pkg.Dot(names.Ident(tokenToName(t.Token)))
		}
		return TypeShape{Type: resourceType}
	case *schema.TokenType:
		// Use the underlying type for now.
		if t.UnderlyingType != nil {
			return mod.typeStringRecHelper(ctx, t.UnderlyingType, qualifier, input, requireInitializers, insideInput)
		}

		pkg, err := parsePackageName(mod.tokenToPackage(t.Token, qualifier))
		if err != nil {
			panic(err)
		}
		tokenType := pkg.Dot(names.Ident(tokenToName(t.Token)))
		return TypeShape{Type: tokenType}
	case *schema.UnionType:
		elementTypeSet := codegen.NewStringSet()
		var elementTypes []TypeShape
		for _, e := range t.ElementTypes {
			e = codegen.UnwrapType(e)
			// If this is an output and a "relaxed" enum, emit the type as the underlying primitive type rather than the union.
			// Eg. Output<String> rather than Output<Either<EnumType, String>>
			if typ, ok := e.(*schema.EnumType); ok && !input {
				return mod.typeStringRecHelper(ctx, typ.ElementType, qualifier, input, requireInitializers, insideInput)
			}

			et := mod.typeStringRecHelper(ctx, e, qualifier, input, false, insideInput)
			etc := et.ToCode(ctx.imports)
			if !elementTypeSet.Has(etc) {
				elementTypeSet.Add(etc)
				elementTypes = append(elementTypes, et)
			}
		}

		switch len(elementTypes) {
		case 1:
			return mod.typeStringRecHelper(ctx, t.ElementTypes[0], qualifier, input, requireInitializers, insideInput)
		case 2:
			return TypeShape{
				Type:       names.Either,
				Parameters: elementTypes,
			}
		default:
			return TypeShape{Type: names.Object}
		}
	default:
		switch t {
		case schema.BoolType:
			return TypeShape{Type: names.Boolean}
		case schema.IntType:
			return TypeShape{Type: names.Integer}
		case schema.NumberType:
			return TypeShape{Type: names.Double}
		case schema.StringType:
			return TypeShape{Type: names.String}
		case schema.ArchiveType:
			return TypeShape{Type: names.Archive}
		case schema.AssetType:
			return TypeShape{Type: names.AssetOrArchive}
		case schema.JSONType:
			return TypeShape{Type: names.JSONElement}
		case schema.AnyType:
			return TypeShape{Type: names.Object}
		case schema.AnyResourceType:
			return TypeShape{Type: names.ResourceType}
		default:
			panic(fmt.Sprintf("Unknown primitive: %#v", t))
		}
	}
}

func (mod *modContext) typeStringForObjectType(t *schema.ObjectType, qualifier qualifier, input bool) TypeShape {
	foreign := !codegen.PkgEquals(t.PackageReference, mod.pkg)
	namingCtx := mod

	if foreign {
		// If object type belongs to another package, we apply naming conventions from that package,
		// including package naming and compatibility mode.
		extPkg := t.PackageReference
		var info PackageInfo
		extDef, err := extPkg.Definition()
		contract.AssertNoErrorf(err, "failed to load package definition for %q", extPkg.Name())
		contract.AssertNoErrorf(extDef.ImportLanguages(map[string]schema.Language{"java": Importer}),
			"failed to load java language plugin for %q", extPkg.Name())
		if v, ok := extDef.Language["java"].(PackageInfo); ok {
			info = v
		}
		namingCtx = &modContext{
			pkg:             extPkg,
			packages:        info.Packages,
			basePackageName: info.BasePackageOrDefault(),
		}
	}
	packageName, err := parsePackageName(namingCtx.tokenToPackage(t.Token, qualifier))
	if err != nil {
		panic(err)
	}
	className := names.Ident(mod.typeName(t))
	if !foreign && mod.classQueue != nil && qualifier != policiesQualifier {
		mod.classQueue.ensureGenerated(classQueueEntry{
			packageName: packageName,
			className:   className,
			schemaType:  t,
			input:       input,
		})
	}
	fqn := packageName.Dot(className)
	return TypeShape{Type: fqn}
}

func (mod *modContext) typeStringForEnumType(enumType *schema.EnumType) TypeShape {
	pkg, err := parsePackageName(mod.tokenToPackage(enumType.Token, enumsQualifier))
	if err != nil {
		panic(err)
	}
	fqn := pkg.Dot(names.Ident(tokenToName(enumType.Token)))
	return TypeShape{Type: fqn}
}

type plainType struct {
	mod                   *modContext
	res                   *schema.Resource
	name                  string
	comment               string
	baseClass             string
	propertyTypeQualifier qualifier
	properties            []*schema.Property
	args                  bool
}

type propJavadocOptions struct {
	indent    string
	isSetter  bool
	isBuilder bool
	isGetter  bool
	fieldName string
}

func genPropJavadoc(ctx *classFileContext, prop *schema.Property, options propJavadocOptions) {
	w := ctx.writer

	if prop.Comment == "" && prop.DeprecationMessage == "" {
		return
	}
	fprintf(w, "%s/**\n", options.indent)

	preamble := ""
	if options.isBuilder {
		preamble = fmt.Sprintf("@param %s ", options.fieldName)
	}
	if options.isSetter {
		preamble = fmt.Sprintf("@param %s ", options.fieldName)
	}
	if options.isGetter {
		preamble = "@return "
	}

	if prop.Comment != "" {
		fprintf(w, "%s\n", formatForeignBlockCommentFrom(preamble+prop.Comment, len(preamble), options.indent))
	}
	if options.isBuilder {
		fprintf(w, "%s\n", formatBlockComment("@return builder", options.indent))
	}

	if prop.DeprecationMessage != "" {
		fprintf(w, "%s * @deprecated\n", options.indent)
		fprintf(w, "%s\n", formatForeignBlockComment(prop.DeprecationMessage, options.indent))
	}
	fprintf(w, "%s */\n", options.indent)
	printObsoleteAttribute(ctx, prop.DeprecationMessage, options.indent)
}

func (pt *plainType) genInputProperty(ctx *classFileContext, prop *schema.Property, targetType TypeShape) error {
	w := ctx.writer
	wireName := prop.Name

	// First generate the input annotation.
	attributeArgs := ""
	if prop.IsRequired() {
		attributeArgs = ", required=true"
	}
	if pt.res != nil && pt.res.IsProvider {
		pType := codegen.UnwrapType(prop.Type)
		json := true
		if pType == schema.StringType {
			json = false
		} else if t, ok := pType.(*schema.TokenType); ok && t.UnderlyingType == schema.StringType {
			json = false
		}
		if json {
			attributeArgs += ", json=true"
		}
	}

	const indent = "    "
	genPropJavadoc(ctx, prop, propJavadocOptions{
		indent: indent,
	})

	propertyName := names.Ident(pt.mod.propertyName(prop))
	propertyModifiers := []string{}

	propertyModifiers = append(propertyModifiers, "private")
	fprintf(w, "    @%s(name=\"%s\"%s)\n", ctx.ref(names.Import), wireName, attributeArgs)
	fprintf(w, "    %s %s %s;\n\n", strings.Join(propertyModifiers, " "),
		targetType.ToCode(ctx.imports), propertyName)

	getterType, returnStatement := targetType, fmt.Sprintf("this.%s", propertyName)

	// Wrap Nullable as optional when returning from the getter.
	if isNullable, t := targetType.UnNullable(); isNullable {
		getterType = t.Optional()
		returnStatement = fmt.Sprintf("%s.ofNullable(%s)",
			ctx.ref(names.Optional),
			returnStatement)
	}

	genPropJavadoc(ctx, prop, propJavadocOptions{
		indent:   indent,
		isGetter: true,
	})
	if err := getterTemplate.Execute(w, getterTemplateContext{
		Indent:          indent,
		GetterType:      getterType.ToCode(ctx.imports),
		GetterName:      names.Ident(prop.Name).AsProperty().Getter(),
		ReturnStatement: returnStatement,
	}); err != nil {
		return err
	}

	return nil
}

func (pt *plainType) genInputType(ctx *classFileContext) error {
	dg := defaultsGen{pt.mod, ctx}

	// Determine property types
	propTypes := make([]TypeShape, len(pt.properties))
	anyPropertyRequired := false
	for i, prop := range pt.properties {
		requireInitializers := !pt.args || isInputType(prop.Type)

		propTypes[i] = pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			true,                // is input
			requireInitializers, // requires initializers
			false,               // outer optional
			false,               // inputless overload
		)

		if prop.IsRequired() {
			anyPropertyRequired = true
		}
	}

	if anyPropertyRequired {
		ctx.imports.Ref(names.PulumiMissingRequiredPropertyException)
	}

	w := ctx.writer
	fprintf(w, "\n")

	// Open the class.
	if pt.comment != "" {
		fprintf(w, "/**\n")
		fprintf(w, "%s\n", formatForeignBlockComment(pt.comment, ""))
		fprintf(w, " */\n")
	}

	fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	fprintf(w, "\n")
	fprintf(w, "    public static final %s Empty = new %s();\n", pt.name, pt.name)
	fprintf(w, "\n")

	// Declare each input property.
	for propIndex, p := range pt.properties {
		propType := propTypes[propIndex]
		if err := pt.genInputProperty(ctx, p, propType); err != nil {
			return err
		}
		fprintf(w, "\n\n")
	}

	if len(pt.properties) > 0 {
		// Generate the empty constructor.
		fprintf(w, "    private %s() {}\n\n", pt.name)

		// Generate the copying constructor.
		fprintf(w, "    private %[1]s(%[1]s $) {\n", pt.name)
		for _, prop := range pt.properties {
			fieldName := names.Ident(pt.mod.propertyName(prop)).AsProperty().Field()
			fprintf(w, "        this.%[1]s = $.%[1]s;\n", fieldName)
		}
		fprintf(w, "    }\n\n")
	}

	// Generate builder() methods that start a Builder
	fprintf(w, "    public static Builder builder() {\n")
	fprintf(w, "        return new Builder();\n")
	fprintf(w, "    }\n")

	if len(pt.properties) > 0 {
		fprintf(w, "    public static Builder builder(%s defaults) {\n", pt.name)
		fprintf(w, "        return new Builder(defaults);\n")
		fprintf(w, "    }\n")
	}

	// Generate the Builder class

	fprintf(w, `
    public static final class Builder {
        private %[1]s $;

        public Builder() {
            $ = new %[1]s();
        }`+"\n", pt.name)

	if len(pt.properties) > 0 {
		fprintf(w, `
        public Builder(%[1]s defaults) {
            $ = new %[1]s(%[2]s.requireNonNull(defaults));
        }`+"\n\n", pt.name, ctx.ref(names.Objects))
	}

	// Generate builder methods for every property.
	for propIndex, prop := range pt.properties {
		propType := propTypes[propIndex]
		fieldName := names.Ident(pt.mod.propertyName(prop)).AsProperty().Field()

		setterName := names.Ident(prop.Name).AsProperty().Setter()

		const indent = "        "
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:    indent,
			isBuilder: true,
			fieldName: fieldName,
		})

		fprintf(w, "        public Builder %[1]s(%[3]s %[2]s) {\n",
			setterName, fieldName, propType.ToCode(ctx.imports))
		fprintf(w, "            $.%[1]s = %[1]s;\n", fieldName)
		fprintf(w, "            return this;\n")
		fprintf(w, "        }\n\n")

		pt.genBuilderHelpers(ctx, setterName, fieldName, propType, prop)
	}

	// Generate the build() method that does default application
	// and missing value checks for every property.
	fprintf(w, "        public %s build() {\n", pt.name)
	for propIndex, prop := range pt.properties {
		propType := propTypes[propIndex]
		fieldName := names.Ident(pt.mod.propertyName(prop)).AsProperty().Field()

		if prop.IsRequired() && prop.DefaultValue == nil && prop.ConstValue == nil {
			fprintf(w, "            if ($.%s == null) {\n", fieldName)
			fprintf(w, "                throw new %s(\"%s\", \"%s\");\n",
				ctx.ref(names.PulumiMissingRequiredPropertyException), pt.name, fieldName)
			fprintf(w, "            }\n")
			continue
		}

		propRef := fmt.Sprintf("$.%s", fieldName)
		propInit, err := dg.defaultValueExpr(
			fmt.Sprintf("property of class %s", pt.name),
			prop, propType, propRef)
		if err != nil {
			return err
		}

		if propRef != propInit {
			fprintf(w, "            %s = %s;\n", propRef, propInit)
		}
	}
	fprintf(w, "            return $;\n")
	fprintf(w, "        }\n") // finish the build() method

	fprintf(w, "    }\n\n") // finish the Builder class
	// Close the class.
	fprintf(w, "}\n")

	return nil
}

func (pt *plainType) genPolicyType(ctx *classFileContext, token *string, pending map[*schema.ObjectType]bool) error {
	// Determine property types

	type PropAndShape struct {
		prop  *schema.Property
		shape schema.Type
	}

	propTypes := map[string]PropAndShape{}

	for _, prop := range pt.properties {
		shape := pt.flattenPolicyProperty(prop.Type, pending)
		if old, ok := propTypes[prop.Name]; ok {
			if old.shape.String() != shape.String() {
				return fmt.Errorf("Two properties for %v.%v with same name but different type: %v != %v", pt.name, prop.Name, old, shape)
			}
		}
		propTypes[prop.Name] = PropAndShape{
			prop:  prop,
			shape: shape,
		}
	}

	w := ctx.writer
	fprintf(w, "\n")

	// Open the class.
	if pt.comment != "" {
		fprintf(w, "/**\n")
		fprintf(w, "%s\n", formatForeignBlockComment(pt.comment, ""))
		fprintf(w, " */\n")
	}

	if token != nil {
		fprintf(w, "@%s(type=\"%s\")\n", ctx.imports.Ref(names.PolicyResourceTypeAnnotation), *token)
		fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	} else {
		fprintf(w, "public final class %s {\n", pt.name)
	}

	fprintf(w, "\n")

	// Declare each input property.
	for _, propAndShape := range propTypes {
		propType := pt.mod.typeString(
			ctx,
			propAndShape.shape,
			policiesQualifier,
			false, // is input
			false, // requires initializers
			false, // outer optional
			false, // inputless overload
		)
		if err := pt.genPolicyProperty(ctx, propAndShape.prop, propType); err != nil {
			return err
		}
		fprintf(w, "\n\n")
	}

	// Close the class.
	fprintf(w, "}\n")

	return nil
}

func (pt *plainType) flattenPolicyProperty(t schema.Type, pending map[*schema.ObjectType]bool) schema.Type {
	switch t := t.(type) {
	case *schema.InputType:
		return pt.flattenPolicyProperty(t.ElementType, pending)

	case *schema.ArrayType:
		return &schema.ArrayType{ElementType: pt.flattenPolicyProperty(t.ElementType, pending)}

	case *schema.MapType:
		return &schema.MapType{ElementType: pt.flattenPolicyProperty(t.ElementType, pending)}

	case *schema.OptionalType:
		return pt.flattenPolicyProperty(t.ElementType, pending)

	case *schema.ObjectType:
		if t.IsInputShape() {
			return pt.flattenPolicyProperty(t.PlainShape, pending)
		}

		pending[t] = true
		return t

	case *schema.UnionType:
		return pt.flattenPolicyProperty(t.DefaultType, pending)

	default:
		return t
	}
}

func (pt *plainType) genPolicyProperty(ctx *classFileContext, prop *schema.Property, targetType TypeShape) error {
	w := ctx.writer

	const indent = "    "
	genPropJavadoc(ctx, prop, propJavadocOptions{
		indent: indent,
	})

	propertyName := names.Ident(pt.mod.propertyName(prop))

	fprintf(w, "    public %s %s;\n\n", targetType.ToCode(ctx.imports), propertyName)

	return nil
}

// Generates derived builder setters that resolve to the main setter.
// This helps to promote T to Output<T>, accept varargs for a List parameter
// and to unroll Either<L, R> to both of its types.
func (pt *plainType) genBuilderHelpers(ctx *classFileContext, setterName,
	fieldName string, t TypeShape, prop *schema.Property,
) {
	w := ctx.writer
	const indent = "        "

	// Helper for when Output<T> is needed but T is provided.
	isOutput, t1 := t.UnOutput()
	if isOutput {
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:    indent,
			isBuilder: true,
			fieldName: fieldName,
		})
		fprintf(w, "        public Builder %[1]s(%[3]s %[2]s) {\n",
			setterName, fieldName, t1.ToCode(ctx.imports))
		fprintf(w, "            return %[1]s(%[3]s.of(%[2]s));\n",
			setterName, fieldName, ctx.ref(names.Output))
		fprintf(w, "        }\n\n")
	}

	// Further helper for when List<T> is needed but varargs are provided.
	if isList, t2 := t1.UnList(); isList {
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:    indent,
			isBuilder: true,
			fieldName: fieldName,
		})
		fprintf(w, "        public Builder %[1]s(%[3]s... %[2]s) {\n",
			setterName, fieldName, t2.ToCode(ctx.imports))
		fprintf(w, "            return %[1]s(%[3]s.of(%[2]s));\n",
			setterName, fieldName, ctx.ref(names.List))
		fprintf(w, "        }\n\n")
	}

	// Further helpers for when Output<Either<L, R>> is needed but L or R provided.
	isEither, t1, t2 := t1.UnEither()
	// We compare fully qualified raw types (with erased generics)
	// to check if method overloading can be successful
	areAmbiguousTypes := t1.Type.Equal(t2.Type)
	if isEither && areAmbiguousTypes {
		fmt.Printf("WARN: Skipping Either unroll because of ambiguous types: %v vs %v",
			t1.Type, t2.Type)
	}
	if isEither && !areAmbiguousTypes {
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:    indent,
			isBuilder: true,
			fieldName: fieldName,
		})
		fprintf(w, "        public Builder %[1]s(%[3]s %[2]s) {\n",
			setterName, fieldName, t1.ToCode(ctx.imports))
		fprintf(w, "            return %[1]s(%[3]s.ofLeft(%[2]s));\n",
			setterName, fieldName, ctx.ref(names.Either),
		)
		fprintf(w, "        }\n\n")
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:    indent,
			isBuilder: true,
			fieldName: fieldName,
		})
		fprintf(w, "        public Builder %[1]s(%[3]s %[2]s) {\n",
			setterName, fieldName, t2.ToCode(ctx.imports))
		fprintf(w, "            return %[1]s(%[3]s.ofRight(%[2]s));\n",
			setterName, fieldName, ctx.ref(names.Either),
		)
		fprintf(w, "        }\n\n")
	}
}

func (pt *plainType) genOutputType(ctx *classFileContext) error {
	w := ctx.writer
	const indent = "    "

	props := pt.properties

	// Open the class and annotate it appropriately.
	fprintf(w, "@%s\n", ctx.ref(names.CustomType))
	fprintf(w, "public final class %s {\n", pt.name)

	anyPropertyRequired := false
	// Generate each output field.
	for _, prop := range props {
		fieldName := names.Ident(pt.mod.propertyName(prop))
		fieldType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false, // outer optional
			false, // inputless overload
		)
		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:   indent,
			isGetter: true,
		})
		fprintf(w, "    private %s %s;\n", fieldType.ToCode(ctx.imports), fieldName)
		if prop.IsRequired() {
			anyPropertyRequired = true
		}
	}

	if anyPropertyRequired {
		ctx.imports.Ref(names.PulumiMissingRequiredPropertyException)
	}

	if len(props) > 0 {
		fprintf(w, "\n")
	}

	// Generate a private constructor.
	fprintf(w, "    private %s() {}\n", pt.name)

	// Generate getters
	for _, prop := range props {
		paramName := names.Ident(prop.Name)
		getterName := names.Ident(prop.Name).AsProperty().Getter()
		getterType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			true,  // outer optional
			false, // inputless overload
		)
		getterTypeNonOptional := pt.mod.typeString(
			ctx,
			codegen.UnwrapType(prop.Type),
			pt.propertyTypeQualifier,
			false,
			false,
			false, // outer optional (irrelevant)
			false, // inputless overload
		)

		returnStatement := fmt.Sprintf("this.%s", paramName)

		switch propType := prop.Type.(type) {
		case *schema.OptionalType:
			switch propType.ElementType.(type) {
			case *schema.ArrayType:
				getterType = getterTypeNonOptional
				returnStatement = fmt.Sprintf("this.%s == null ? List.of() : this.%s", paramName, paramName)
			case *schema.MapType:
				getterType = getterTypeNonOptional
				returnStatement = fmt.Sprintf("this.%s == null ? Map.of() : this.%s", paramName, paramName)
			default:
				// Option<Output<T>> are stored as @Nullable Output<T>. We don't
				// need to perform the nullable conversion for them.
				if !getterType.Type.Equal(names.Output) {
					returnStatement = fmt.Sprintf("%s.ofNullable(this.%s)", ctx.ref(names.Optional), paramName)
				}
			}
		}

		genPropJavadoc(ctx, prop, propJavadocOptions{
			indent:   indent,
			isGetter: true,
		})
		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          indent,
			GetterType:      getterType.ToCode(ctx.imports),
			GetterName:      getterName,
			ReturnStatement: returnStatement,
		}); err != nil {
			return err
		}

		fprintf(w, "\n")
	}

	// Generate Builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range props {
		propertyName := names.Ident(pt.mod.propertyName(prop))
		propertyType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false, // is input
			false, // requires initializers
			false, // outer optional
			false, // inputless overload
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.ToCode(ctx.imports),
			FieldName: propertyName.AsProperty().Field(),
		})

		setterName := propertyName.AsProperty().Setter()

		// add setter
		var setterAnnotation string
		if setterName != prop.Name {
			setterAnnotation = fmt.Sprintf("@%s.Setter(\"%s\")", ctx.ref(names.CustomType), prop.Name)
		} else {
			setterAnnotation = fmt.Sprintf("@%s.Setter", ctx.ref(names.CustomType))
		}
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName.String(),
			Assignment:   fmt.Sprintf("this.%s = %s", propertyName, propertyName),
			IsRequired:   prop.IsRequired(),
			ListType:     propertyType.ListType(ctx),
			Annotations:  []string{setterAnnotation},
		})
	}

	fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     indent,
		Name:       "Builder",
		IsFinal:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
		Annotations: []string{
			fmt.Sprintf("@%s.Builder", ctx.ref(names.CustomType)),
		},
	}); err != nil {
		return err
	}
	fprintf(w, "\n")

	// Close the class.
	fprintf(w, "}\n")
	return nil
}

func primitiveValue(value interface{}) (string, *TypeShape, error) {
	v := reflect.ValueOf(value)
	if v.Kind() == reflect.Interface {
		v = v.Elem()
	}

	switch v.Kind() {
	case reflect.Bool:
		if v.Bool() {
			return "true", &TypeShape{Type: names.Boolean}, nil
		}
		return "false", &TypeShape{Type: names.Boolean}, nil
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32:
		return strconv.FormatInt(v.Int(), 10), &TypeShape{Type: names.Integer}, nil
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32:
		return strconv.FormatUint(v.Uint(), 10), &TypeShape{Type: names.Integer}, nil
	case reflect.Float32, reflect.Float64:
		return strconv.FormatFloat(v.Float(), 'e', -1, 64), &TypeShape{Type: names.Double}, nil
	case reflect.String:
		return fmt.Sprintf("%q", v.String()), &TypeShape{Type: names.String}, nil
	default:
		return "", nil, fmt.Errorf("unsupported default value of type %T", value)
	}
}

func genAlias(ctx *classFileContext, alias *schema.Alias) {
	w := ctx.writer
	fprintf(w, "%s.of(", ctx.ref(names.Output))
	fprintf(w, "%s.builder()", ctx.ref(names.Alias))
	fprintf(w, ".type(\"%v\")", alias.Type)
	fprintf(w, ".build()")
	fprintf(w, ")")
}

func (mod *modContext) genResource(ctx *classFileContext, r *schema.Resource, argsFQN, stateFQN names.FQN) error {
	w := ctx.writer
	// Create a resource module file into which all of this resource's types will go.
	name := resourceName(r)

	if r.Comment != "" || r.DeprecationMessage != "" {
		fprintf(w, "/**\n")
		if r.Comment != "" {
			fprintf(w, "%s\n", formatForeignBlockComment(r.Comment, ""))
		}

		if r.DeprecationMessage != "" {
			fprintf(w, " * @deprecated\n")
			fprintf(w, "%s\n", formatForeignBlockComment(r.DeprecationMessage, ""))

		}
		fprintf(w, " */\n")
	}

	// Open the class.
	className := name
	var baseType string
	optionsType := "com.pulumi.resources.CustomResourceOptions"
	switch {
	case r.IsProvider:
		baseType = "com.pulumi.resources.ProviderResource"
	case r.IsComponent:
		baseType = "com.pulumi.resources.ComponentResource"
		optionsType = "com.pulumi.resources.ComponentResourceOptions"
	default:
		baseType = "com.pulumi.resources.CustomResource"
	}

	printObsoleteAttribute(ctx, r.DeprecationMessage, "")
	fprintf(w, "@%s(type=\"%s\")\n",
		ctx.imports.Ref(names.ResourceTypeAnnotation),
		r.Token)
	fprintf(w, "public class %s extends %s {\n", className, baseType)

	var secretProps []string
	// Emit all output properties.
	for _, prop := range r.Properties {
		// Write the property attribute
		wireName := prop.Name
		propertyName := names.Ident(mod.propertyName(prop))
		propertyType := mod.typeString(
			ctx,
			prop.Type,
			outputsQualifier,
			false,
			false,
			false, // outer optional
			false, // inputless overload
		)
		// TODO: C# has some kind of workaround here for strings

		if prop.Secret {
			secretProps = append(secretProps, prop.Name)
		}

		if prop.Comment != "" || prop.DeprecationMessage != "" {
			fprintf(w, "    /**\n")
			if prop.Comment != "" {
				fprintf(w, "%s\n", formatForeignBlockComment(prop.Comment, "    "))
			}

			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @deprecated\n")
				fprintf(w, "%s\n", formatForeignBlockComment(prop.DeprecationMessage, "    "))

			}
			fprintf(w, "     */\n")
		}

		outputExportRefs, outputExportTree := propertyType.ToTree(ctx.imports)
		outputParameterType := propertyType.ToCodeCommentedAnnotations(ctx.imports)
		printObsoleteAttribute(ctx, prop.DeprecationMessage, "    ")
		fprintf(w,
			"    @%s(name=\"%s\", refs={%s}, tree=\"%s\")\n",
			ctx.ref(names.Export), wireName, outputExportRefs, outputExportTree)
		fprintf(w,
			"    private %s<%s> %s;\n", ctx.imports.Ref(names.Output), outputParameterType, propertyName)
		fprintf(w, "\n")

		if prop.Comment != "" {
			fprintf(w, "    /**\n")
			fprintf(w, "%s\n", formatForeignBlockCommentFrom("@return "+prop.Comment, 2, "    "))
			fprintf(w, "     */\n")
		}

		// Add getter
		getterType := outputParameterType
		getterName := names.Ident(prop.Name).AsProperty().ResourceGetter()

		// Prefer to surface non-required properties as `Output<Optional<T>>` rather than
		// `Output</* Nullable */ T>` through the getter.
		getterExpr := fmt.Sprintf("this.%s", propertyName)
		if isNullable, t := propertyType.UnNullable(); isNullable {
			getterType = t.Optional().ToCode(ctx.imports)
			getterExpr = fmt.Sprintf("%s.optional(this.%s)", ctx.ref(names.Codegen), propertyName)
		}

		fprintf(w, "    public %s<%s> %s() {\n", ctx.ref(names.Output), getterType, getterName)
		fprintf(w, "        return %s;\n", getterExpr)
		fprintf(w, "    }\n")
	}

	if len(r.Properties) > 0 {
		fprintf(w, "\n")
	}

	// Emit the class constructor

	allOptionalInputs := true
	hasConstInputs := false
	for _, prop := range r.InputProperties {
		allOptionalInputs = allOptionalInputs && !prop.IsRequired()
		hasConstInputs = hasConstInputs || prop.ConstValue != nil
	}

	argsType := ctx.ref(argsFQN)
	if allOptionalInputs {
		// If the number of required input properties was zero, we can make the args object optional.
		argsType = fmt.Sprintf("@%s %s", ctx.ref(names.Nullable), argsType)
	}

	tok := r.Token
	if r.IsProvider {
		tok = mod.pkg.Name()
	}

	// Name only constructor
	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     */\n")
	fprintf(w, "    public %s(java.lang.String name) {\n", className)
	fprintf(w, "        this(name, %s.Empty);\n", ctx.ref(argsFQN))
	fprintf(w, "    }\n")

	// Name+Args constructor

	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     * @param args The arguments to use to populate this resource's properties.\n")
	fprintf(w, "     */\n")
	fprintf(w, "    public %s(java.lang.String name, %s args) {\n", className, argsType)
	fprintf(w, "        this(name, args, null);\n")
	fprintf(w, "    }\n")

	// Constructor
	remoteOrDependency := ""
	if r.IsComponent {
		// Component resources in SDKs will be remote, so we pass `true` to the parent (ComponentResource) for the
		// `remote` argument.
		remoteOrDependency = ", true"
	} else {
		// In order to supply the optional package reference argument, we need to supply a value for the `dependency`
		// parameter, which is true if and only if the resource is a synthetic one for dependency tracking. This is not
		// the case for the resources we are generating, so we can pass `false` to enable us to hit the overload we
		// want.
		remoteOrDependency = ", false"
	}

	pkg, err := mod.pkg.Definition()
	if err != nil {
		return err
	}

	param := ""
	if pkg.Parameterization != nil {
		param = fmt.Sprintf(", %s.getPackageRef()", mod.utilitiesRef(ctx))
	}
	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     * @param args The arguments to use to populate this resource's properties.\n")
	fprintf(w, "     * @param options A bag of options that control this resource's behavior.\n")
	fprintf(w, "     */\n")

	fprintf(w, "    public %s(java.lang.String name, %s args, @%s %s options) {\n",
		className, argsType, ctx.ref(names.Nullable), optionsType)
	fprintf(w, "        super(\"%s\", name, makeArgs(args, options), makeResourceOptions(options, %s.empty())%s%s);\n",
		tok, ctx.imports.Ref(names.Codegen), remoteOrDependency, param)

	fprintf(w, "    }\n")

	// Write a private constructor for the use of `get`.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", "null"
		if r.StateInputs != nil {
			stateParam, stateRef = fmt.Sprintf("@%s %s state, ", ctx.ref(names.Nullable), ctx.ref(stateFQN)), "state"
		}

		fprintf(w, "\n")
		fprintf(w, "    private %s(java.lang.String name, %s<java.lang.String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Output), stateParam, ctx.ref(names.Nullable), optionsType)
		fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, id)%s%s);\n",
			tok, stateRef, remoteOrDependency, param)
		fprintf(w, "    }\n")
	}

	// Write the method that will calculate the resource arguments.
	fprintf(w, "\n")
	fprintf(w, "    private static %s makeArgs(%s args, @%s %s options) {\n",
		ctx.ref(argsFQN), argsType, ctx.ref(names.Nullable), optionsType)
	fprintf(w,
		"        if (options != null && options.getUrn().isPresent()) {\n")
	fprintf(w,
		"            return null;\n")
	fprintf(w,
		"        }\n")
	if hasConstInputs {
		fprintf(w,
			"        var builder = args == null ? %[1]s.builder() : %[1]s.builder(args);\n", ctx.ref(argsFQN))
		fprintf(w, "        return builder\n")
		for _, prop := range r.InputProperties {
			if prop.ConstValue != nil {
				v, _, err := primitiveValue(prop.ConstValue)
				if err != nil {
					return err
				}
				setterName := names.Ident(mod.propertyName(prop)).AsProperty().Setter()
				fprintf(w, "            .%s(%s)\n", setterName, v)
			}
		}
		fprintf(w, "            .build();\n")
	} else {
		fprintf(w,
			"        return args == null ? %s.Empty : args;\n", ctx.ref(argsFQN))
	}
	fprintf(w, "    }\n")

	// Write the method that will calculate the resource options.
	fprintf(w, "\n")
	fprintf(w,
		"    private static %[1]s makeResourceOptions(@%[2]s %[1]s options, @%[2]s %[3]s<java.lang.String> id) {\n",
		optionsType, ctx.ref(names.Nullable), ctx.ref(names.Output))
	fprintf(w, "        var defaultOptions = %s.builder()\n", optionsType)
	fprintf(w, "            .version(%s.getVersion())\n", mod.utilitiesRef(ctx))
	if url := pkg.PluginDownloadURL; url != "" {
		fprintf(w, "            .pluginDownloadURL(%q)\n", url)
	}

	if len(r.Aliases) > 0 {
		fprintf(w, "            .aliases(%s.of(\n", ctx.ref(names.List))
		for i, alias := range r.Aliases {
			fprintf(w, "                ")
			genAlias(ctx, alias)
			isLastElement := i == len(r.Aliases)-1
			if isLastElement {
				fprintf(w, "\n")
			} else {
				fprintf(w, ",\n")
			}
		}
		fprintf(w, "            ))\n")
	}
	if len(secretProps) > 0 {
		fprintf(w, "            .additionalSecretOutputs(%s.of(\n", ctx.ref(names.List))
		for i, sp := range secretProps {
			fprintf(w, "                ")
			fprintf(w, "%q", sp)
			isLastElement := i == len(secretProps)-1
			if isLastElement {
				fprintf(w, "\n")
			} else {
				fprintf(w, ",\n")
			}
		}
		fprintf(w, "            ))\n")
	}

	fprintf(w, "            .build();\n")
	fprintf(w, "        return %s.merge(defaultOptions, options, id);\n", optionsType)
	fprintf(w, "    }\n\n")

	// Write the `get` method for reading instances of this resource unless this
	// is a provider resource or ComponentResource.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", ""

		fprintf(w, "    /**\n")
		fprintf(w, "     * Get an existing Host resource's state with the given name, ID, and optional extra\n")
		fprintf(w, "     * properties used to qualify the lookup.\n")
		fprintf(w, "     *\n")
		fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
		fprintf(w, "     * @param id The _unique_ provider ID of the resource to lookup.\n")
		if r.StateInputs != nil {
			stateParam = fmt.Sprintf("@%s %s state, ", ctx.ref(names.Nullable), ctx.ref(stateFQN))
			stateRef = "state, "
			fprintf(w, "     * @param state\n")
		}
		fprintf(w, "     * @param options Optional settings to control the behavior of the CustomResource.\n")
		fprintf(w, "     */\n")

		fprintf(w, "    public static %s get(java.lang.String name, %s<java.lang.String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Output), stateParam, ctx.ref(names.Nullable), optionsType)
		fprintf(w, "        return new %s(name, id, %soptions);\n", className, stateRef)
		fprintf(w, "    }\n")
	}

	// Close the class.
	fprintf(w, "}\n")

	return nil
}

type addClassMethod = func(names.FQN, names.Ident, func(*classFileContext) error) error

func (mod *modContext) functionsClassName() (names.Ident, error) {
	if mod.mod != "" {
		return names.Ident(names.Title(mod.mod) + "Functions"), nil
	}
	if mod.pkg.Name() != "" {
		return names.Ident(names.Title(mod.pkg.Name()) + "Functions"), nil
	}
	return "", fmt.Errorf("package name empty")
}

func printCommentFunction(ctx *classFileContext, fun *schema.Function, indent string) {
	w := ctx.writer
	if fun.Comment != "" || fun.DeprecationMessage != "" {
		fprintf(w, "    /**\n")
		fprintf(w, "%s\n", formatForeignBlockComment(fun.Comment, indent))
		if fun.DeprecationMessage != "" {
			fprintf(w, "     * @deprecated\n")
			fprintf(w, "%s\n", formatForeignBlockComment(fun.DeprecationMessage, indent))
		}
		fprintf(w, "     */\n")
	}
	printObsoleteAttribute(ctx, fun.DeprecationMessage, "    ")
}

func hasAllOptionalInputs(fun *schema.Function) bool {
	if fun.Inputs == nil {
		return true
	}
	for _, prop := range fun.Inputs.Properties {
		if prop.IsRequired() {
			return false
		}
	}

	return true
}

func sortedFunctions(m []*schema.Function) []*schema.Function {
	idxMap := make(map[string]int, len(m))
	keyList := make([]string, len(m))
	for i, fun := range m {
		idxMap[fun.Token] = i
		keyList[i] = fun.Token
	}
	sort.Strings(keyList)

	sortedList := make([]*schema.Function, len(m))
	for k := range keyList {
		sortedList[k] = m[idxMap[keyList[k]]]
	}
	return sortedList
}

func (mod *modContext) genFunctions(ctx *classFileContext, addClass addClassMethod) error {
	javaPkg, err := parsePackageName(mod.packageName)
	if err != nil {
		return err
	}
	w := ctx.writer

	// Open the config class.
	className, err := mod.functionsClassName()
	if err != nil {
		return err
	}
	fprintf(w, "public final class %s {\n", className)
	for _, fun := range sortedFunctions(mod.functions) {

		const indent = "    "

		// TODO[pulumi/pulumi-java#262]: Support proper codegen for methods
		if fun.IsMethod {
			continue
		}

		if fun.IsOverlay {
			// This function code is generated by the provider, so no further action is required.
			continue
		}

		outputsPkg := javaPkg.Dot(names.Ident("outputs"))
		resultClass := tokenToFunctionResultClassName(mod, fun.Token)
		resultFQN := outputsPkg.Dot(resultClass)
		inputsPkg := javaPkg.Dot(names.Ident("inputs"))

		// Generating "{Function}Args" class for invokes that return Output<T>
		// builders for this class allow outputs as inputs for their properties
		argsClass := names.Ident(tokenToName(fun.Token) + "Args")
		argsFQN := inputsPkg.Dot(argsClass)

		// Generating "{Function}PlainArgs" class for invokes that return CompletableFuture<T>
		// builders for this class only allow non-outputs as inputs for their properties
		plainArgsClass := names.Ident(tokenToName(fun.Token) + "PlainArgs")
		plainArgsFQN := inputsPkg.Dot(plainArgsClass)

		var argsType string
		var plainArgsType string
		if fun.Inputs == nil {
			argsType = ctx.ref(names.InvokeArgs)
			plainArgsType = ctx.ref(names.InvokeArgs)
		} else {
			argsType = ctx.ref(argsFQN)
			plainArgsType = ctx.ref(plainArgsFQN)
		}

		var returnType string
		if fun.Outputs != nil {
			returnType = ctx.imports.Ref(resultFQN)
		} else {
			returnType = ctx.imports.Ref(names.Void)
		}

		// default method name returns Output<ReturnType>
		methodName := tokenToFunctionName(fun.Token)

		// another "plain" overload will return CompletableFuture<ReturnType>
		plainMethodName := methodName + "Plain"

		// Emit datasource inputs method
		invokeOptions := ctx.ref(names.InvokeOptions)

		if hasAllOptionalInputs(fun) {
			// Generate invoke that return Output<T>
			printCommentFunction(ctx, fun, indent)
			// Add no args invoke (returns Output<T>)
			fprintf(w, "    public static %s<%s> %s() {\n",
				ctx.ref(names.Output), returnType, methodName)
			fprintf(w,
				"        return %s(%s.Empty, %s.Empty);\n",
				methodName, argsType, invokeOptions)
			fprintf(w, "    }\n")

			// Generate invoke that return CompletableFuture<T>
			printCommentFunction(ctx, fun, indent)
			fprintf(w, "    public static %s<%s> %s() {\n",
				ctx.ref(names.CompletableFuture), returnType, plainMethodName)
			fprintf(w,
				"        return %s(%s.Empty, %s.Empty);\n",
				plainMethodName, plainArgsType, invokeOptions)
			fprintf(w, "    }\n")
		}

		// Output version: add args only invoke
		printCommentFunction(ctx, fun, indent)
		fprintf(w, "    public static %s<%s> %s(%s args) {\n",
			ctx.ref(names.Output), returnType, methodName, argsType)
		fprintf(w,
			"        return %s(args, %s.Empty);\n",
			methodName, invokeOptions)
		fprintf(w, "    }\n")

		// CompletableFuture version: add args only invoke
		printCommentFunction(ctx, fun, indent)
		fprintf(w, "    public static %s<%s> %s(%s args) {\n",
			ctx.ref(names.CompletableFuture), returnType, plainMethodName, plainArgsType)
		fprintf(w,
			"        return %s(args, %s.Empty);\n",
			plainMethodName, invokeOptions)
		fprintf(w, "    }\n")

		// Output version: add full invoke with InvokeOptions
		printCommentFunction(ctx, fun, indent)
		fprintf(w, "    public static %s<%s> %s(%s args, %s options) {\n",
			ctx.ref(names.Output), returnType, methodName, argsType, invokeOptions)
		fprintf(w,
			"        return %s.getInstance().invoke(\"%s\", %s.of(%s.class), args, %s.withVersion(options)",
			ctx.ref(names.Deployment), fun.Token, ctx.ref(names.TypeShape), returnType, mod.utilitiesRef(ctx))

		pkg, err := mod.pkg.Definition()
		if err != nil {
			return err
		}

		if pkg.Parameterization != nil {
			fprintf(w, ", %s.getPackageRef()", mod.utilitiesRef(ctx))
		}

		fprintf(w, ");\n")
		fprintf(w, "    }\n")

		// Output version: add full invoke with InvokeOutputOptions
		invokeOutputOptions := ctx.ref(names.InvokeOutputOptions)
		printCommentFunction(ctx, fun, indent)
		fprintf(w, "    public static %s<%s> %s(%s args, %s options) {\n",
			ctx.ref(names.Output), returnType, methodName, argsType, invokeOutputOptions)
		fprintf(w,
			"        return %s.getInstance().invoke(\"%s\", %s.of(%s.class), args, %s.withVersion(options)",
			ctx.ref(names.Deployment), fun.Token, ctx.ref(names.TypeShape), returnType, mod.utilitiesRef(ctx))

		if pkg.Parameterization != nil {
			fprintf(w, ", %s.getPackageRef()", mod.utilitiesRef(ctx))
		}

		fprintf(w, ");\n")
		fprintf(w, "    }\n")

		// CompletableFuture version: add full invoke
		// notice how the implementation now uses `invokeAsync` instead of `invoke`
		printCommentFunction(ctx, fun, indent)
		fprintf(w, "    public static %s<%s> %s(%s args, %s options) {\n",
			ctx.ref(names.CompletableFuture), returnType, plainMethodName, plainArgsType, invokeOptions)
		fprintf(w,
			"        return %s.getInstance().invokeAsync(\"%s\", %s.of(%s.class), args, %s.withVersion(options)",
			ctx.ref(names.Deployment), fun.Token, ctx.ref(names.TypeShape), returnType, mod.utilitiesRef(ctx))

		if pkg.Parameterization != nil {
			fprintf(w, ", %s.getPackageRef()", mod.utilitiesRef(ctx))
		}

		fprintf(w, ");\n")
		fprintf(w, "    }\n")

		// Emit the args and result types, if any.
		if fun.Inputs != nil {
			// Generate "{Function}Args" class for invokes that return Output<T>
			// Notice here using fun.Inputs.InputShape.Properties which use the shape which accepting Outputs
			if err := addClass(inputsPkg, argsClass, func(ctx *classFileContext) error {
				args := &plainType{
					mod:                   mod,
					name:                  ctx.className.String(),
					baseClass:             "com.pulumi.resources.InvokeArgs",
					propertyTypeQualifier: inputsQualifier,
					properties:            fun.Inputs.InputShape.Properties,
				}
				return args.genInputType(ctx)
			}); err != nil {
				return err
			}

			// Generate "{Function}PlainArgs" class for invokes that return CompletableFuture<T>
			// Notice here using fun.Inputs.Properties which use the plain shape (not accepting Outputs)
			if err := addClass(inputsPkg, plainArgsClass, func(ctx *classFileContext) error {
				args := &plainType{
					mod:                   mod,
					name:                  ctx.className.String(),
					baseClass:             "com.pulumi.resources.InvokeArgs",
					propertyTypeQualifier: inputsQualifier,
					properties:            fun.Inputs.Properties,
				}
				return args.genInputType(ctx)
			}); err != nil {
				return err
			}
		}

		if fun.Outputs != nil {
			if err := addClass(outputsPkg, resultClass, func(ctx *classFileContext) error {
				res := &plainType{
					mod:                   mod,
					name:                  ctx.className.String(),
					propertyTypeQualifier: outputsQualifier,
					properties:            fun.Outputs.Properties,
				}
				contract.Assertf(resultClass.String() == res.name,
					"expected result class name to be %v, got %v", resultClass, res.name)
				return res.genOutputType(ctx)
			}); err != nil {
				return err
			}
		}
	}
	fprintf(w, "}\n")
	return nil
}

func printObsoleteAttribute(ctx *classFileContext, deprecationMessage, indent string) {
	w := ctx.writer
	if deprecationMessage != "" {
		fprintf(w, "%s@Deprecated /* %s */\n",
			indent,
			strings.Replace(deprecationMessage, `"`, `""`, -1))
	}
}

// what's qualifier?
func (mod *modContext) genEnum(ctx *classFileContext, enum *schema.EnumType) error {
	w := ctx.writer
	indent := "    "
	enumName := tokenToName(enum.Token)

	// Fix up identifiers for each enum value.
	for _, e := range enum.Elements {
		// If the enum doesn't have a name, set the value as the name.
		if e.Name == "" {
			e.Name = fmt.Sprintf("%v", e.Value)
		}

		safeName, err := names.MakeSafeEnumName(e.Name, enumName)
		if err != nil {
			return err
		}
		e.Name = safeName
	}

	if enum.Comment != "" {
		fprintf(w, "%s/**\n", indent)
		fprintf(w, "%s\n", formatForeignBlockComment(enum.Comment, indent))
		fprintf(w, "%s */\n", indent)
	}

	underlyingType := mod.typeString(
		ctx,
		enum.ElementType,
		enumsQualifier,
		false,
		false,
		false, // outer optional
		false, // inputless overload
	)
	switch enum.ElementType {
	case schema.IntType, schema.StringType, schema.NumberType:
		// Open the enum and annotate it appropriately.
		fprintf(w, "%s@%s\n", indent, ctx.ref(names.EnumType))
		fprintf(w, "%spublic enum %s {\n", indent, enumName)
		indent := strings.Repeat(indent, 2)

		// Enum values
		for i, e := range enum.Elements {
			if e.Comment != "" || e.DeprecationMessage != "" {
				fprintf(w, "%s/**\n", indent)
				if e.Comment != "" {
					fprintf(w, "%s\n", formatForeignBlockComment(e.Comment, indent))
				}

				if e.DeprecationMessage != "" {
					fprintf(w, "%s * @deprecated\n", indent)
					fprintf(w, "%s * %s\n", indent, e.DeprecationMessage)
				}
				fprintf(w, "%s */\n", indent)
			}
			printObsoleteAttribute(ctx, e.DeprecationMessage, indent)
			var separator string
			if i == len(enum.Elements)-1 { // last element
				separator = ";"
			} else {
				separator = ","
			}
			if enum.ElementType == schema.StringType {
				fprintf(w, "%s%s(%q)%s\n", indent, e.Name, e.Value, separator)
			} else if enum.ElementType == schema.NumberType {
				fprintf(w, "%s%s(%f)%s\n", indent, e.Name, e.Value, separator)
			} else {
				fprintf(w, "%s%s(%v)%s\n", indent, e.Name, e.Value, separator)
			}
		}
		fprintf(w, "\n")

		fprintf(w, "%sprivate final %s value;\n", indent, underlyingType.ToCode(ctx.imports))
		fprintf(w, "\n")

		// Constructor
		fprintf(w, "%s%s(%s value) {\n", indent, enumName, underlyingType.ToCode(ctx.imports))
		if enum.ElementType == schema.StringType {
			fprintf(w, "%s    this.value = %s.requireNonNull(value);\n", indent, ctx.ref(names.Objects))
		} else {
			fprintf(w, "%s    this.value = value;\n", indent)
		}
		fprintf(w, "%s}\n", indent)
		fprintf(w, "\n")

		// Explicit conversion operator
		fprintf(w, "%[1]s@%s.Converter\n", indent, ctx.ref(names.EnumType))
		fprintf(w, "%[1]spublic %s getValue() {\n", indent, underlyingType.ToCode(ctx.imports))
		fprintf(w, "%s    return this.value;\n", indent)
		fprintf(w, "%s}\n", indent)
		fprintf(w, "\n")

		// toString override
		fprintf(w, "%s@Override\n", indent)
		fprintf(w, "%spublic java.lang.String toString() {\n", indent)
		fprintf(w, "%s    return new %s(\", \", \"%s[\", \"]\")\n", indent, ctx.ref(names.StringJoiner), enumName)
		fprintf(w, "%s        .add(\"value='\" + this.value + \"'\")\n", indent)
		fprintf(w, "%s        .toString();\n", indent)
		fprintf(w, "%s}\n", indent)
	default:
		// TODO: Issue to implement boolean-based enums [in C#]: https://github.com/pulumi/pulumi/issues/5652
		return fmt.Errorf("enums of type %s are not yet implemented for this language", enum.ElementType.String())
	}

	// Close the enum declaration
	fprintf(w, "%s}\n", indent)

	return nil
}

type qualifier int

const (
	noQualifier qualifier = iota
	enumsQualifier
	inputsQualifier
	outputsQualifier
	policiesQualifier
)

func (q qualifier) String() string {
	switch q {
	case noQualifier:
		return ""
	case enumsQualifier:
		return "enums"
	case inputsQualifier:
		return "inputs"
	case outputsQualifier:
		return "outputs"
	case policiesQualifier:
		return "policies"
	}
	panic("invalid qualifier")
}

func (mod *modContext) genType(
	ctx *classFileContext,
	obj *schema.ObjectType,
	input bool,
) error {
	propertyTypeQualifier := outputsQualifier
	if input {
		propertyTypeQualifier = inputsQualifier
	}

	pt := &plainType{
		mod:                   mod,
		name:                  ctx.className.String(),
		comment:               obj.Comment,
		propertyTypeQualifier: propertyTypeQualifier,
		properties:            obj.Properties,
		args:                  obj.IsInputShape(),
	}

	if input {
		pt.baseClass = "com.pulumi.resources.ResourceArgs"
		if !obj.IsInputShape() {
			pt.baseClass = "com.pulumi.resources.InvokeArgs"
		}
		return pt.genInputType(ctx)
	}

	return pt.genOutputType(ctx)
}

func (mod *modContext) genHeader() string {
	var buf bytes.Buffer
	w := &buf
	fprintf(w, "// *** WARNING: this file was generated by %v. ***\n", mod.tool)
	fprintf(w, "// *** Do not edit by hand unless you're certain you know what you are doing! ***\n")
	return buf.String()
}

// Computes the projected type of a Config property and the code to emit.
func (mod *modContext) getConfigProperty(ctx *classFileContext, prop *schema.Property) (TypeShape, string, error) {
	// Unwrap prop.Type TokenType into UnderlyingType.
	schemaType := prop.Type
	for {
		t0 := schemaType
		switch t := schemaType.(type) {
		case *schema.TokenType:
			if t.UnderlyingType != nil {
				schemaType = t.UnderlyingType
			}
		}
		if schemaType == t0 {
			break
		}
	}

	projectedType := mod.typeString(
		ctx,
		schemaType,
		inputsQualifier,
		false,
		true,  // requireInitializers - set to true so we preserve Optional
		true,  // outer optional
		false, // inputless overload
	)

	dg := &defaultsGen{mod, ctx}

	code, err := dg.configExpr("property of config", prop, projectedType)
	if err != nil {
		return TypeShape{}, "", err
	}
	return projectedType, code, nil
}

func (mod *modContext) genConfig(ctx *classFileContext, variables []*schema.Property) error {
	w := ctx.writer

	// Open the config class.
	fprintf(w, "public final class Config {\n")
	fprintf(w, "\n")
	// Create a config bag for the variables to pull from.
	fprintf(w, "    private static final com.pulumi.Config config = com.pulumi.Config.of(%q);", mod.pkg.Name())
	fprintf(w, "\n")

	// Emit an entry for all config variables.
	for _, p := range variables {
		propertyType, returnStatement, err := mod.getConfigProperty(ctx, p)
		if err != nil {
			return err
		}

		getterName := names.Ident(mod.propertyName(p)).AsProperty().Getter()

		if p.Comment != "" {
			fprintf(w, "/**\n")
			fprintf(w, "%s\n", formatForeignBlockComment(p.Comment, ""))
			fprintf(w, " */\n")
		}
		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          "    ",
			GetterType:      propertyType.ToCode(ctx.imports),
			GetterName:      getterName,
			ReturnStatement: returnStatement,
		}); err != nil {
			return err
		}
		fprintf(w, "\n")
	}

	// TODO: finish the config generation, emit any nested types.

	// Close the config class and namespace.
	fprintf(w, "}\n")

	return nil
}

type fs map[string][]byte

func (fs fs) add(path string, contents []byte) {
	old, has := fs[path]
	if string(old) != string(contents) {
		contract.Assertf(!has, "duplicate file: %s", path)
	}
	fs[path] = contents
}

func (mod *modContext) rootPackage() names.FQN {
	pkg, err := parsePackageName(mod.rootPackageName)
	if err != nil {
		panic(err)
	}
	return pkg
}

func (mod *modContext) utilitiesRef(ctx *classFileContext) string {
	return ctx.ref(mod.rootPackage().Dot(names.Ident("Utilities")))
}

func gradleProjectPath() string {
	return filepath.Join("src", "main", "java")
}

func (mod *modContext) gen(fs fs) error {
	pkgComponents := strings.Split(mod.packageName, ".")

	dir := filepath.Join(gradleProjectPath(), filepath.Join(pkgComponents...))

	var files []string
	for p := range fs {
		d := path.Dir(p)
		if d == "." {
			d = ""
		}
		if d == dir {
			files = append(files, p)
		}
	}

	generateClassFilePath := func(pkg names.FQN, className names.Ident) string {
		fqn := pkg.Dot(className)
		relPath := filepath.Join(strings.Split(fqn.String(), ".")...)
		return filepath.Join(gradleProjectPath(), relPath) + ".java"
	}

	addClass := func(javaPkg names.FQN, javaClass names.Ident, gen func(*classFileContext) error) error {
		classPath := generateClassFilePath(javaPkg, javaClass)
		if _, ok := fs[classPath]; ok {
			// Already processed
			return nil
		}

		javaCode, err := genClassFile(javaPkg, javaClass, gen)
		if err != nil {
			return err
		}

		contents := fmt.Sprintf("%s\n%s", mod.genHeader(), javaCode)

		files = append(files, classPath)
		fs.add(classPath, []byte(contents))

		return nil
	}

	javaPkg, err := parsePackageName(mod.packageName)
	if err != nil {
		return err
	}

	// Utilities, config
	switch mod.mod {
	case "":
		if err := addClass(mod.rootPackage(), names.Ident("Utilities"), func(ctx *classFileContext) error {
			pkg, err := mod.pkg.Definition()
			if err != nil {
				return err
			}

			var additionalImports, packageReferenceUtilities string
			if pkg.Parameterization != nil {
				additionalImports = `
import java.util.concurrent.CompletableFuture;
import com.pulumi.deployment.Deployment;

`

				packageReferenceUtilities = fmt.Sprintf(`

	private static final CompletableFuture<java.lang.String> packageRef;
	public static CompletableFuture<java.lang.String> getPackageRef() {
		return packageRef;
	}

	static {
		packageRef = Deployment.getInstance().registerPackage(
			// Base provider name
			"%s",
			// Base provider version
			"%s",
			// Base provider download URL
			"%s",
			// Package name
			"%s",
			// Package version
			getVersion(),
			// Parameter
			"%s"
		);
	}
`,
					pkg.Parameterization.BaseProvider.Name,
					pkg.Parameterization.BaseProvider.Version,
					pkg.PluginDownloadURL,
					pkg.Name,
					base64.StdEncoding.EncodeToString(pkg.Parameterization.Parameter),
				)
			}

			return javaUtilitiesTemplate.Execute(ctx.writer, javaUtilitiesTemplateContext{
				VersionPath:               strings.ReplaceAll(ensureEndsWithDot(mod.basePackageName)+mod.pkg.Name(), ".", "/"),
				ClassName:                 "Utilities",
				Tool:                      mod.tool,
				AdditionalImports:         additionalImports,
				PackageReferenceUtilities: packageReferenceUtilities,
			})
		}); err != nil {
			return err
		}

		// Ensure that the target module directory contains a README.md file.
		readme := mod.pkg.Description()
		if readme != "" && readme[len(readme)-1] != '\n' {
			readme += "\n"
		}
		fs.add("README.md", []byte(readme))
	case "config":
		config, err := mod.pkg.Config()
		if err != nil {
			return err
		}
		if len(config) > 0 {
			configPkg, err := parsePackageName(mod.configClassPackageName)
			if err != nil {
				return err
			}
			if err := addClass(configPkg, names.Ident("Config"), func(ctx *classFileContext) error {
				return mod.genConfig(ctx, config)
			}); err != nil {
				return err
			}
		}
	}

	pending := map[*schema.ObjectType]bool{}

	// Resources
	for _, r := range mod.resources {
		if r.IsOverlay {
			// This resource code is generated by the provider, so no further action is required.
			continue
		}

		inputsPkg := javaPkg.Dot(names.Ident("inputs"))
		argsClassName := names.Ident(resourceName(r) + "Args")
		argsFQN := javaPkg.Dot(argsClassName)

		var stateFQN names.FQN
		stateClassName := names.Ident(resourceName(r) + "State")
		if r.StateInputs != nil {
			stateFQN = inputsPkg.Dot(stateClassName)
		}

		// Generate Resource class
		if err := addClass(javaPkg, names.Ident(resourceName(r)), func(ctx *classFileContext) error {
			return mod.genResource(ctx, r, argsFQN, stateFQN)
		}); err != nil {
			return err
		}

		// Generate ResourceArgs class
		if err := addClass(javaPkg, argsClassName, func(ctx *classFileContext) error {
			args := &plainType{
				mod:                   mod,
				res:                   r,
				name:                  string(ctx.className),
				baseClass:             "com.pulumi.resources.ResourceArgs",
				propertyTypeQualifier: inputsQualifier,
				properties:            r.InputProperties,
				args:                  true,
			}
			return args.genInputType(ctx)
		}); err != nil {
			return err
		}

		policyPkg := javaPkg.Dot(names.Ident("policies"))

		policyPkg, err := parsePackageName(mod.tokenToPackage(r.Token, policiesQualifier))
		if err != nil {
			return err
		}

		policyClassName := names.Ident(tokenToName(r.Token))

		// Generate ResourcePolicy class
		if err := addClass(policyPkg, policyClassName, func(ctx *classFileContext) error {
			props := slices.Concat(r.Properties, r.InputProperties)

			if r.StateInputs != nil {
				props = slices.Concat(props, r.StateInputs.Properties)
			}

			args := &plainType{
				mod:                   mod,
				name:                  string(ctx.className),
				baseClass:             "com.pulumi.resources.PolicyResource",
				propertyTypeQualifier: policiesQualifier,
				properties:            props,
			}
			return args.genPolicyType(ctx, &r.Token, pending)
		}); err != nil {
			return err
		}

		// Generate the `get` args type, if any.
		if r.StateInputs != nil {
			if err := addClass(inputsPkg, stateClassName, func(ctx *classFileContext) error {
				state := &plainType{
					mod:                   mod,
					res:                   r,
					name:                  string(ctx.className),
					baseClass:             "com.pulumi.resources.ResourceArgs",
					propertyTypeQualifier: inputsQualifier,
					properties:            r.StateInputs.Properties,
					args:                  true,
				}
				return state.genInputType(ctx)
			}); err != nil {
				return err
			}
		}
	}

	// Functions
	if len(mod.functions) > 0 {
		className, err := mod.functionsClassName()
		if err != nil {
			return err
		}
		if err := addClass(javaPkg, className, func(ctx *classFileContext) error {
			return mod.genFunctions(ctx, addClass)
		}); err != nil {
			return err
		}
	}

	// Enums
	if len(mod.enums) > 0 {
		for _, enum := range mod.enums {
			enumClassName := names.Ident(tokenToName(enum.Token))
			if err := addClass(javaPkg.Dot(names.Ident("enums")), enumClassName, func(ctx *classFileContext) error {
				return mod.genEnum(ctx, enum)
			}); err != nil {
				return err
			}
		}
	}

	for !mod.classQueue.isEmpty() {
		entry := mod.classQueue.dequeue()
		if err := addClass(entry.packageName, entry.className, func(ctx *classFileContext) error {
			return mod.genType(ctx, entry.schemaType, entry.input)
		}); err != nil {
			return err
		}
	}

	for len(pending) > 0 {
		pending2 := pending
		pending = map[*schema.ObjectType]bool{}
		for t, _ := range pending2 {
			pendingPkg, err := parsePackageName(mod.tokenToPackage(t.Token, policiesQualifier))
			if err != nil {
				return err
			}

			pendingClassName := names.Ident(tokenToName(t.Token))

			// Generate the extra ResourcePolicy class
			if err := addClass(pendingPkg, pendingClassName, func(ctx *classFileContext) error {
				args := &plainType{
					mod:                   mod,
					name:                  string(ctx.className),
					propertyTypeQualifier: policiesQualifier,
					properties:            t.Properties,
				}
				return args.genPolicyType(ctx, nil, pending)
			}); err != nil {
				return err
			}
		}
	}

	return nil
}

// TODO: package metadata if needed

func computePropertyNames(props []*schema.Property, names map[*schema.Property]string) {
	for _, p := range props {
		if info, ok := p.Language["java"].(PropertyInfo); ok && info.Name != "" {
			names[p] = info.Name
		}
	}
}

func generateModuleContextMap(tool string, pkg *schema.Package) (map[string]*modContext, *PackageInfo, error) {
	// Decode Java-specific info for each package as we discover them.
	infos := map[*schema.Package]*PackageInfo{}
	getPackageInfo := func(p schema.PackageReference) *PackageInfo {
		def, err := p.Definition()
		contract.AssertNoErrorf(err, "Failed to get package definition for %q", p.Name())
		info, ok := infos[def]
		if !ok {
			if err := def.ImportLanguages(map[string]schema.Language{"java": Importer}); err != nil {
				panic(err)
			}

			var javaInfo PackageInfo
			if raw, ok := pkg.Language["java"]; ok {
				javaInfo, ok = raw.(PackageInfo)
				if !ok {
					panic(fmt.Sprintf("Failed to cast `pkg.Language[\"java\"]`=%v to `PackageInfo`", raw))
				}
			}

			javaInfo = javaInfo.
				WithDefaultDependencies().
				WithJavaSdkDependencyDefault(DefaultSdkVersion)

			info = &javaInfo
			infos[def] = info
		}
		if info.BasePackage == "" && p.Namespace() != "" {
			info.BasePackage = "com." + sanitizeImport(p.Namespace()) + "."
		}
		return info
	}
	infos[pkg] = getPackageInfo(pkg.Reference())

	propertyNames := map[*schema.Property]string{}
	computePropertyNames(pkg.Config, propertyNames)
	computePropertyNames(pkg.Provider.InputProperties, propertyNames)
	for _, r := range pkg.Resources {
		if r.IsOverlay {
			// This resource code is generated by the provider, so no further action is required.
			continue
		}

		computePropertyNames(r.Properties, propertyNames)
		computePropertyNames(r.InputProperties, propertyNames)
		if r.StateInputs != nil {
			computePropertyNames(r.StateInputs.Properties, propertyNames)
		}
	}
	for _, f := range pkg.Functions {
		if f.IsOverlay {
			// This function code is generated by the provider, so no further action is required.
			continue
		}

		if f.Inputs != nil {
			computePropertyNames(f.Inputs.Properties, propertyNames)
		}
		if f.Outputs != nil {
			computePropertyNames(f.Outputs.Properties, propertyNames)
		}
	}
	for _, t := range pkg.Types {
		if obj, ok := t.(*schema.ObjectType); ok {
			computePropertyNames(obj.Properties, propertyNames)
		}
	}

	// group resources, types, and functions into Java packages
	modules := map[string]*modContext{}

	var getMod func(modName string, p schema.PackageReference) *modContext
	getMod = func(modName string, p schema.PackageReference) *modContext {
		mod, ok := modules[modName]
		if !ok {
			info := getPackageInfo(p)
			basePackage := info.BasePackageOrDefault()
			rootPackage := basePackage + packageName(info.Packages, pkg.Name)
			pkgName := rootPackage
			if modName != "" {
				pkgName += "." + packageName(info.Packages, modName)
			}
			mod = &modContext{
				pkg:             p,
				mod:             modName,
				tool:            tool,
				packageName:     pkgName,
				rootPackageName: rootPackage,
				basePackageName: basePackage,
				packages:        info.Packages,
				propertyNames:   propertyNames,
				classQueue:      newClassQueue(),
			}

			if modName != "" {
				parentName := path.Dir(modName)
				if parentName == "." {
					parentName = ""
				}
				parent := getMod(parentName, p)
				parent.children = append(parent.children, mod)
			}

			// Save the module only if it's for the current package.
			// This way, modules for external packages are not saved.
			if codegen.PkgEquals(p, pkg.Reference()) {
				modules[modName] = mod
			}
		}
		return mod
	}

	getModFromToken := func(token string, p *schema.Package) *modContext {
		return getMod(p.TokenToModule(token), p.Reference())
	}

	// Create the config module if necessary.
	if len(pkg.Config) > 0 {
		cfg := getMod("config", pkg.Reference())
		cfg.configClassPackageName = cfg.basePackageName + packageName(infos[pkg].Packages, pkg.Name)
	}

	// Find input and output types referenced by resources.
	scanResource := func(r *schema.Resource) {
		mod := getModFromToken(r.Token, pkg)
		mod.resources = append(mod.resources, r)
	}

	scanResource(pkg.Provider)
	for _, r := range pkg.Resources {
		scanResource(r)
	}

	// Find input and output types referenced by functions.
	for _, f := range pkg.Functions {
		mod := getModFromToken(f.Token, pkg)
		mod.functions = append(mod.functions, f)
	}

	// Find nested types.
	for _, t := range pkg.Types {
		switch typ := codegen.UnwrapType(t).(type) {
		case *schema.ObjectType:
			mod := getModFromToken(typ.Token, pkg)
			mod.types = append(mod.types, typ)
			if mod.typesByName == nil {
				mod.typesByName = map[string]*schema.ObjectType{}
			}
			mod.typesByName[tokenToName(typ.Token)] = typ
		case *schema.EnumType:
			if !typ.IsOverlay {
				mod := getModFromToken(typ.Token, pkg)
				mod.enums = append(mod.enums, typ)
			}
		default:
			continue
		}
	}

	return modules, infos[pkg], nil
}

// LanguageResource is derived from the schema and can be used by downstream codegen.
type LanguageResource struct {
	*schema.Resource

	Name    string // The resource name (e.g. Deployment)
	Package string // The package name (e.g. apps.v1)
}

// LanguageResources returns a map of resources that can be used by downstream codegen. The map
// key is the resource schema token.
func LanguageResources(tool string, pkg *schema.Package) (map[string]LanguageResource, error) {
	modules, info, err := generateModuleContextMap(tool, pkg)
	if err != nil {
		return nil, err
	}

	resources := map[string]LanguageResource{}
	for modName, mod := range modules {
		if modName == "" {
			continue
		}
		for _, r := range mod.resources {
			if r.IsOverlay {
				// This resource code is generated by the provider, so no further action is required.
				continue
			}
			lr := LanguageResource{
				Resource: r,
				Package:  packageName(info.Packages, modName),
				Name:     tokenToName(r.Token),
			}
			resources[r.Token] = lr
		}
	}

	return resources, nil
}

func GeneratePackage(
	tool string,
	pkg *schema.Package,
	extraFiles map[string][]byte,
	localDependencies map[string]string,
	local bool,
	legacyBuildFiles bool,
) (map[string][]byte, error) {
	// Presently, Gradle is the primary build system we support for generated SDKs. Later on, when we validate the
	// package in order to produce build system artifacts, we'll need a description and repository. To this end, we
	// ensure there are non-empty values for these fields here.
	if pkg.Description == "" {
		pkg.Description = " "
	}
	if pkg.Repository == "" {
		pkg.Repository = "https://example.com"
	}

	modules, info, err := generateModuleContextMap(tool, pkg)
	if err != nil {
		return nil, err
	}

	// We need to ensure that local dependencies are reflected in the lists of dependencies and repositories in the Java
	// PackageInfo.
	pkgOverrides := PackageInfo{}

	dependencies := map[string]string{}
	repositories := map[string]bool{}
	for name, dep := range localDependencies {
		// A local dependency has the form groupId:artifactId:version[:repositoryPath]. We'll parse this and add an
		// entry to the dependency map for groupId:artifactId -> version, and add the repositoryPath to the list of
		// repositories if it's present.
		parts := strings.Split(dep, ":")
		if len(parts) < 3 {
			return nil, fmt.Errorf(
				"invalid dependency for %s %s; must be of the form groupId:artifactId:version[:repositoryPath]",
				name, dep,
			)
		}

		// local dependencies should only be using the pulumi core sdk
		// in the future we will be adding local dependencies to component providers
		// but for non-component providers, we should only be using the pulumi core sdk
		if parts[1] != "pulumi" {
			continue
		}

		k := parts[0] + ":" + parts[1]
		dependencies[k] = parts[2]

		if len(parts) == 4 {
			repositories[parts[3]] = true
		}
	}

	pkgOverrides.Dependencies = dependencies
	pkgOverrides.Repositories = maps.Keys(repositories)

	overriddenInfo := info.With(pkgOverrides)
	info = &overriddenInfo

	pkg.Language["java"] = info

	// Generate each module.
	files := fs{}
	for p, f := range extraFiles {
		files.add(p, f)
	}

	for _, mod := range modules {
		if err := mod.gen(files); err != nil {
			return nil, err
		}
	}

	var useGradle bool
	if local {
		// Local packages do not use gradle.
		useGradle = false
	} else {
		// `legacyBuildFiles is set by `pulumi-java-gen`. When we remove the
		// deprecated `pulumi-java-gen` executable, we can remove the
		// legacyBuildFiles flag.
		if legacyBuildFiles {
			// The default for legacy invocations is "none", so we need to see an explicit "gradle" setting.
			useGradle = info.BuildFiles == "gradle"
		} else {
			// The default for new invocations is to use gradle, unless "none" is specified explicitly.
			useGradle = info.BuildFiles != "none"
		}
	}

	// Currently, packages come bundled with a version.txt resource that is used by generated code to report a version.
	// When a build tool is configured, we defer the generation of this file to the build process so that e.g. CI
	// processes can set the version to be used when releasing or publishing a package, as opposed to when the code for
	// that package is generated. In the case that we are generating a package without a build tool, or a local package
	// to be incorporated into a program with an existing build process, we need to emit the version.txt file explicitly
	// as part of code generation.
	if useGradle {
		if err := genGradleProject(pkg, info, files, legacyBuildFiles); err != nil {
			return nil, err
		}
		return files, nil
	}

	pkgName := fmt.Sprintf("%s%s", info.BasePackageOrDefault(), pkg.Name)
	pkgPath := strings.ReplaceAll(pkgName, ".", "/")

	var version string
	if pkg.Version != nil {
		version = pkg.Version.String()
	} else {
		version = "0.0.1"
	}

	files.add("src/main/resources/"+pkgPath+"/version.txt", []byte(version))
	return files, nil
}

func isInputType(t schema.Type) bool {
	if optional, ok := t.(*schema.OptionalType); ok {
		t = optional.ElementType
	}

	_, stillOption := t.(*schema.OptionalType)
	contract.Assertf(!stillOption, "optional type should have been unwrapped")
	_, isInputType := t.(*schema.InputType)
	return isInputType
}

func ignoreOptional(t *schema.OptionalType, requireInitializers bool) bool {
	switch t.ElementType.(type) {
	case *schema.InputType:
		return true
	case *schema.ArrayType, *schema.MapType:
		return !requireInitializers
	}
	return false
}

// TODO ignores identifier parse errors for the moment.
func parsePackageName(packageName string) (names.FQN, error) {
	parts := strings.Split(packageName, ".")
	if len(parts) < 1 {
		return names.FQN{}, fmt.Errorf("empty package name: %s", packageName)
	}
	result := names.Ident(parts[0]).FQN()
	for _, p := range parts[1:] {
		result = result.Dot(names.Ident(p))
	}
	return result, nil
}

type classQueue struct {
	inputTypes      map[*schema.ObjectType]classQueueEntry
	outputTypes     map[*schema.ObjectType]classQueueEntry
	seenInputTypes  map[*schema.ObjectType]bool
	seenOutputTypes map[*schema.ObjectType]bool
}

type classQueueEntry struct {
	packageName names.FQN
	className   names.Ident
	schemaType  *schema.ObjectType
	input       bool
}

func newClassQueue() *classQueue {
	return &classQueue{
		inputTypes:      map[*schema.ObjectType]classQueueEntry{},
		outputTypes:     map[*schema.ObjectType]classQueueEntry{},
		seenInputTypes:  map[*schema.ObjectType]bool{},
		seenOutputTypes: map[*schema.ObjectType]bool{},
	}
}

func (q *classQueue) isEmpty() bool {
	return len(q.inputTypes) == 0 && len(q.outputTypes) == 0
}

func (q *classQueue) ensureGenerated(entry classQueueEntry) {
	t := entry.schemaType
	typeMap, seenMap := q.outputTypes, q.seenOutputTypes
	if entry.input {
		typeMap, seenMap = q.inputTypes, q.seenInputTypes
	}

	if _, seen := seenMap[t]; !seen {
		seenMap[t] = true
		typeMap[t] = entry
	}
}

func (q *classQueue) dequeue() classQueueEntry {
	queue := q.inputTypes
	if len(q.inputTypes) == 0 {
		queue = q.outputTypes
		if len(q.outputTypes) == 0 {
			panic("deque() on an empty queue")
		}
	}
	var firstEntry classQueueEntry
	for _, entry := range queue {
		firstEntry = entry
		break
	}
	delete(queue, firstEntry.schemaType)
	return firstEntry
}
