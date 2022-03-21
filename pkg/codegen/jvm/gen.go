// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"bytes"
	"fmt"
	"path"
	"reflect"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"

	"github.com/pulumi/pulumi-java/pkg/codegen/jvm/names"
)

type typeDetails struct {
	outputType bool
	inputType  bool
	stateType  bool
	plainType  bool
}

func packageName(packages map[string]string, name string) string {
	if pkg, ok := packages[name]; ok {
		return pkg
	}

	return name
}

type modContext struct {
	pkg                    *schema.Package
	mod                    string
	propertyNames          map[*schema.Property]string
	types                  []*schema.ObjectType
	enums                  []*schema.EnumType
	resources              []*schema.Resource
	functions              []*schema.Function
	typeDetails            map[*schema.ObjectType]*typeDetails
	children               []*modContext
	tool                   string
	packageName            string
	rootPackageName        string
	basePackageName        string
	packages               map[string]string
	dictionaryConstructors bool
	configClassPackageName string
}

func (mod *modContext) propertyName(p *schema.Property) string {
	if n, ok := mod.propertyNames[p]; ok {
		return n
	}
	return p.Name
}

func getListType(ctx *classFileContext, propertyType TypeShape) string {
	if propertyType.Type.String() == "java.util.List" {
		for _, a := range propertyType.Parameters {
			if strings.HasPrefix(a.Type.String(), "java.util.") {
				continue
			}
			return a.ToCode(ctx.imports)
		}
	}
	return ""
}

func (mod *modContext) details(t *schema.ObjectType) *typeDetails {
	details, ok := mod.typeDetails[t]
	if !ok {
		details = &typeDetails{}
		mod.typeDetails[t] = details
	}
	return details
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
	return tokenToName(tok)
}

func (mod *modContext) tokenToPackage(tok string, qualifier string) string {
	components := strings.Split(tok, ":")
	contract.Assertf(len(components) == 3, "malformed token %v", tok)

	pkg := mod.basePackageName + packageName(mod.packages, components[0])
	pkgName := mod.pkg.TokenToModule(tok)

	typ := pkg
	if pkgName != "" {
		typ += "." + packageName(mod.packages, pkgName)
	}
	if qualifier != "" {
		typ += "." + qualifier
	}

	return typ
}

func (mod *modContext) typeName(t *schema.ObjectType, state, args bool) string {
	name := tokenToName(t.Token)
	if state {
		return name + "GetArgs"
	}
	if args {
		return name + "Args"
	}
	return name
}

func (mod *modContext) typeString(
	ctx *classFileContext,
	t schema.Type,
	qualifier string,
	input bool,
	state bool,
	// Influences how Map and Array types are generated.
	requireInitializers bool,
	// Allow returning `Optional<T>` directly. Otherwise `@Nullable T` will be returned at the outer scope.
	outerOptional bool,
	// Called in the context of an overload without an `Output<T>` wrapper. We
	// should act like we are inside an Output<T>.
	inputlessOverload bool,
) TypeShape {
	inner := mod.typeStringRecHelper(ctx, t, qualifier, input, state, requireInitializers, inputlessOverload)
	if inner.Type.Equal(names.Optional) && !outerOptional {
		contract.Assert(len(inner.Parameters) == 1)
		contract.Assert(len(inner.Annotations) == 0)
		inner = inner.Parameters[0]
		inner.Annotations = append(inner.Annotations, fmt.Sprintf("@%s", ctx.ref(names.Nullable)))
	}
	return inner
}

// A facilitator function for the inner recursion of `typeString`.
func (mod *modContext) typeStringRecHelper(
	ctx *classFileContext,
	t schema.Type,
	qualifier string,
	input bool,
	state bool,
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
		inner := mod.typeStringRecHelper(ctx, elem, qualifier, true, state, requireInitializers, true)

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
		inner := mod.typeStringRecHelper(ctx, t.ElementType, qualifier, input, state, requireInitializers, insideInput)
		if ignoreOptional(t, requireInitializers) {
			inner.Annotations = append(inner.Annotations, fmt.Sprintf("@%s", ctx.ref(names.Nullable)))
			return inner
		}
		return TypeShape{
			Type:       names.Optional,
			Parameters: []TypeShape{inner},
		}

	case *schema.EnumType:
		// TODO: try to replace with 'qualifier'
		pkg, err := parsePackageName(mod.tokenToPackage(t.Token, ""))
		if err != nil {
			panic(err)
		}
		return TypeShape{
			Type: pkg.Dot("enums").Dot(names.Ident(tokenToName(t.Token))),
		}

	case *schema.ArrayType:
		listType := names.List // TODO: decide weather or not to use ImmutableList
		if requireInitializers {
			listType = names.List
		}

		return TypeShape{
			Type: listType,
			Parameters: []TypeShape{
				mod.typeStringRecHelper(ctx,
					codegen.PlainType(t.ElementType), qualifier, input, state, false, insideInput,
				),
			},
		}

	case *schema.MapType:
		mapType := names.Map // TODO: decide weather or not to use ImmutableMap
		if requireInitializers {
			mapType = names.Map
		}

		return TypeShape{
			Type: mapType,
			Parameters: []TypeShape{
				{Type: names.String},
				mod.typeStringRecHelper(ctx,
					codegen.PlainType(t.ElementType), qualifier, input, state, false, insideInput,
				),
			},
		}

	case *schema.ObjectType:
		namingCtx := mod
		if t.Package != mod.pkg {
			// If object type belongs to another package, we apply naming conventions from that package,
			// including package naming and compatibility mode.
			extPkg := t.Package
			var info PackageInfo
			contract.AssertNoError(extPkg.ImportLanguages(map[string]schema.Language{"jvm": Importer}))
			if v, ok := t.Package.Language["jvm"].(PackageInfo); ok {
				info = v
			}
			namingCtx = &modContext{
				pkg:             extPkg,
				packages:        info.Packages,
				basePackageName: info.BasePackageOrDefault(),
			}
		}
		pkg, err := parsePackageName(namingCtx.tokenToPackage(t.Token, qualifier))
		if err != nil {
			panic(err)
		}
		typ := pkg.Dot(names.Ident(mod.typeName(t, state, insideInput)))
		return TypeShape{Type: typ}
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
			if t.Resource != nil && t.Resource.Package != mod.pkg {
				// If resource type belongs to another package, we apply naming conventions from that package,
				// including package naming and compatibility mode.
				extPkg := t.Resource.Package
				var info PackageInfo
				contract.AssertNoError(extPkg.ImportLanguages(map[string]schema.Language{"jvm": Importer}))
				if v, ok := t.Resource.Package.Language["jvm"].(PackageInfo); ok {
					info = v
				}
				namingCtx = &modContext{
					pkg:             extPkg,
					packages:        info.Packages,
					basePackageName: info.BasePackageOrDefault(),
				}
			}
			pkg, err := parsePackageName(namingCtx.tokenToPackage(t.Token, ""))
			if err != nil {
				panic(err)
			}
			resourceType = pkg.Dot(names.Ident(tokenToName(t.Token)))
		}
		return TypeShape{Type: resourceType}
	case *schema.TokenType:
		// Use the underlying type for now.
		if t.UnderlyingType != nil {
			return mod.typeStringRecHelper(ctx, t.UnderlyingType, qualifier, input, state, requireInitializers, insideInput)
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
				return mod.typeStringRecHelper(ctx, typ.ElementType, qualifier, input, state, requireInitializers, insideInput)
			}

			et := mod.typeStringRecHelper(ctx, e, qualifier, input, state, false, insideInput)
			etc := et.ToCode(ctx.imports)
			if !elementTypeSet.Has(etc) {
				elementTypeSet.Add(etc)
				elementTypes = append(elementTypes, et)
			}
		}

		switch len(elementTypes) {
		case 1:
			return mod.typeStringRecHelper(ctx, t.ElementTypes[0], qualifier, input, state, requireInitializers, insideInput)
		case 2:
			return TypeShape{
				Type:       names.Either,
				Parameters: elementTypes,
			}
		default:
			//return TypeShape{Type: names.Object, Annotations: elementTypeSet.SortedValues()}
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
		default:
			panic(fmt.Sprintf("Unknown primitive: %#v", t))
		}
	}
}

// Returns a constructor for an empty instance of type `t`.
//
// optionalAsNull is used in conjunction with the `typeString` parameter
// `outerOptional` to ensure types line up.
// In general, `outerOptional` <=> `!optionalAsNull`.
func emptyTypeInitializer(ctx *classFileContext, t schema.Type, optionalAsNull bool) string {
	if isInputType(t) {
		return fmt.Sprintf("%s.empty()", ctx.ref(names.Output))
	}
	if _, ok := t.(*schema.OptionalType); ok && !optionalAsNull {
		return fmt.Sprintf("%s.empty()", ctx.ref(names.Optional))
	}
	switch codegen.UnwrapType(t).(type) {
	case *schema.ArrayType:
		return fmt.Sprintf("%s.of()", ctx.ref(names.List))
	case *schema.MapType:
		return fmt.Sprintf("%s.of()", ctx.ref(names.Map))
	// TODO: should we return an "empty Either" (not sure what that means exactly)
	// case *schema.UnionType:
	// 	return "null"
	default:
		return "null"
	}
}

func typeInitializer(ctx *classFileContext, t schema.Type, nested string, nestedType string) string {
	handleUnion := func(t *schema.UnionType, left, right string) string {
		var m string
		switch nestedType {
		case codegen.UnwrapType(t.ElementTypes[0]).String():
			m = left
		case codegen.UnwrapType(t.ElementTypes[1]).String():
			m = right
		default:
			panic(fmt.Sprintf("this should never happen: '%s' does not match either '%s' or '%s', for nested of: '%s'",
				nestedType, t.ElementTypes[0], t.ElementTypes[1], nested,
			))
		}
		return fmt.Sprintf("%s(%s)", m, nested)
	}

	switch t := t.(type) {
	case *schema.OptionalType:
		inner := typeInitializer(ctx, t.ElementType, nested, nestedType)
		if ignoreOptional(t, true) {
			return inner
		}
		var method string
		switch t.ElementType.(type) {
		case *schema.ArrayType, *schema.MapType, *schema.UnionType:
			method = "of"
		default:
			method = "ofNullable"
		}
		return fmt.Sprintf("Optional.%s(%s)", method, inner)

	case *schema.InputType:
		switch t := t.ElementType.(type) {
		case *schema.ArrayType:
			return fmt.Sprintf("%s.ofList(%s)", ctx.ref(names.Output), nested)
		case *schema.MapType:
			return fmt.Sprintf("%s.ofMap(%s)", ctx.ref(names.Output), nested)
		case *schema.UnionType:
			return handleUnion(t,
				fmt.Sprintf("%s.ofLeft", ctx.ref(names.Output)),
				fmt.Sprintf("%s.ofRight", ctx.ref(names.Output)))
		default:
			return fmt.Sprintf("%s.ofNullable(%s)", ctx.ref(names.Output), nested)
		}

	case *schema.ArrayType:
		return fmt.Sprintf("%s.of(%s)", ctx.ref(names.List), nested)

	case *schema.MapType:
		return fmt.Sprintf("%s.of(%s)", ctx.ref(names.Map), nested)

	case *schema.UnionType:
		return handleUnion(t,
			fmt.Sprintf("%s.leftOf", ctx.ref(names.Either)),
			fmt.Sprintf("%s.rightOf", ctx.ref(names.Either)))

	default:
		return nested
	}
}

type plainType struct {
	mod                   *modContext
	res                   *schema.Resource
	name                  string
	comment               string
	baseClass             string
	propertyTypeQualifier string
	properties            []*schema.Property
	args                  bool
	state                 bool
}

func (pt *plainType) genInputProperty(ctx *classFileContext, prop *schema.Property, isFinal bool) error {
	w := ctx.writer
	requireInitializers := !pt.args || isInputType(prop.Type)
	wireName := prop.Name
	typ := prop.Type
	if !prop.IsRequired() {
		typ = codegen.OptionalType(prop)
	}
	propertyType := pt.mod.typeString(
		ctx,
		typ,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		requireInitializers, // requires initializers
		false,               // outer optional
		false,               // inputless overload
	)

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

	if prop.Comment != "" || prop.DeprecationMessage != "" {
		fprintf(w, "    /**\n")
		if prop.Comment != "" {
			fprintf(w, "%s\n", formatBlockComment(prop.Comment, indent))
		}

		if prop.DeprecationMessage != "" {
			fprintf(w, "     * @Deprecated\n")
			fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, indent))

		}
		fprintf(w, "     */\n")
	}

	propertyName := names.Ident(pt.mod.propertyName(prop))
	propertyModifiers := make([]string, 2)

	propertyModifiers = append(propertyModifiers, "private")
	if isFinal {
		propertyModifiers = append(propertyModifiers, "final")
	}
	printObsoleteAttribute(ctx, prop.DeprecationMessage, indent)
	fprintf(w, "    @%s(name=\"%s\"%s)\n", ctx.ref(names.Import), wireName, attributeArgs)
	fprintf(w, "    %s %s %s;\n\n",
		strings.Join(propertyModifiers, " "), propertyType.ToCode(ctx.imports), propertyName)

	// Add getter
	getterType := pt.mod.typeString(
		ctx,
		prop.Type,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		requireInitializers, // FIXME: should not require initializers, make it immutable
		true,                // outer optional
		false,               // inputless overload
	)

	printObsoleteAttribute(ctx, prop.DeprecationMessage, indent)
	returnStatement := fmt.Sprintf("this.%s", propertyName)
	if opt, ok := prop.Type.(*schema.OptionalType); ok {
		req := opt.ElementType
		emptyStatement := emptyTypeInitializer(ctx, prop.Type, false)
		switch req.(type) {
		case *schema.ArrayType, *schema.MapType, *schema.UnionType, *schema.InputType: // the most common case actually
			getterTypeNonOptional := pt.mod.typeString(
				ctx,
				req,
				pt.propertyTypeQualifier,
				true,                // is input
				pt.state,            // is state
				requireInitializers, // FIXME: should not require initializers, make it immutable
				false,               // outer optional
				false,               // inputless overload
			)
			getterType = getterTypeNonOptional
			emptyStatement = emptyTypeInitializer(ctx, req, true)
		default:
			// nested type is only used when prop.Type is a union
			returnStatement = typeInitializer(ctx, prop.Type, returnStatement, "")
		}
		returnStatement = fmt.Sprintf("this.%s == null ? %s : %s", propertyName, emptyStatement, returnStatement)
	}

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
	if len(pt.properties) > 250 {
		return pt.genJumboInputType(ctx)
	}
	return pt.genNormalInputType(ctx)
}

func (pt *plainType) genJumboInputType(ctx *classFileContext) error {
	// generates a class for Outputs where pt.properties >= 250
	w := ctx.writer
	fprintf(w, "\n")

	props := pt.properties

	// Open the class.
	if pt.comment != "" {
		fprintf(w, "/**\n")
		fprintf(w, "%s\n", formatBlockComment(pt.comment, ""))
		fprintf(w, " */\n")
	}

	fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	fprintf(w, "\n")
	fprintf(w, "    public static final %s Empty = new %s();\n", pt.name, pt.name)
	fprintf(w, "\n")

	// Declare each input property.
	for _, p := range props {
		if err := pt.genInputProperty(ctx, p, false); err != nil {
			return err
		}
		fprintf(w, "\n")
		fprintf(w, "\n")
	}

	// Generate empty constructor, not that the instance created
	// with this constructor may not be valid if there are 'required' fields.
	if len(props) > 0 {
		fprintf(w, "\n")
		fprintf(w, "    private %s() {\n", pt.name)
		for _, prop := range props {
			fieldName := names.Ident(pt.mod.propertyName(prop))
			emptyValue := emptyTypeInitializer(ctx, prop.Type, true)
			fprintf(w, "        this.%s = %s;\n", fieldName, emptyValue)
		}
		fprintf(w, "    }\n")
	}

	// Generate the builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range props {
		requireInitializers := !pt.args || isInputType(prop.Type)
		propertyName := names.Ident(pt.mod.propertyName(prop))
		propertyType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			true,                // is input
			pt.state,            // is state
			requireInitializers, // requires initializers
			false,               // outer optional
			false,               // inputless overload
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.ToCode(ctx.imports),
			FieldName: propertyName.String(),
		})

		setterName := names.Ident(prop.Name).AsProperty().Setter()
		assignment := func(propertyName names.Ident) string {
			if prop.Secret {
				return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Output), propertyName)
			}
			if prop.IsRequired() {
				return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
			}
			return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
		}

		// add main setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName.String(),
			Assignment:   assignment(propertyName),
			ListType:     getListType(ctx, propertyType),
		})

		if isInputType(prop.Type) { // we have a wrapped field so we add an unwrapped helper setter
			var typ schema.Type = &schema.OptionalType{ElementType: codegen.UnwrapType(prop.Type)}
			if prop.IsRequired() {
				typ = codegen.UnwrapType(typ)
			}
			propertyTypeUnwrapped := pt.mod.typeString(
				ctx,
				typ,
				pt.propertyTypeQualifier,
				true,                // is input
				pt.state,            // is state
				requireInitializers, // requires initializers
				false,               // outer optional
				true,                // inputless overload
			)

			assignmentUnwrapped := func(propertyName names.Ident) string {
				if prop.Secret {
					return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Output), propertyName)
				}
				if prop.IsRequired() {
					return fmt.Sprintf("this.%s = %s.of(%s.requireNonNull(%s))",
						propertyName, ctx.ref(names.Output), ctx.ref(names.Objects), propertyName)
				}
				return fmt.Sprintf("this.%s = %s.ofNullable(%s)", propertyName, ctx.ref(names.Output), propertyName)
			}

			if !propertyTypeUnwrapped.Equal(propertyType) {
				// add overloaded setter

				builderSetters = append(builderSetters, builderSetterTemplateContext{
					SetterName:   setterName,
					PropertyType: propertyTypeUnwrapped.ToCode(ctx.imports),
					PropertyName: propertyName.String(),
					Assignment:   assignmentUnwrapped(propertyName),
					ListType:     getListType(ctx, propertyTypeUnwrapped),
				})
			}
		}
	}

	fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     "    ",
		Name:       "Builder",
		IsFinal:    true,
		IsJumbo:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
	}); err != nil {
		return err
	}
	fprintf(w, "\n")

	// Close the class.
	fprintf(w, "}\n")

	return nil
}

func (pt *plainType) genNormalInputType(ctx *classFileContext) error {
	w := ctx.writer
	fprintf(w, "\n")

	// TODO: proper support for large constructors
	props := pt.properties

	// Open the class.
	if pt.comment != "" {
		fprintf(w, "/**\n")
		fprintf(w, "%s\n", formatBlockComment(pt.comment, ""))
		fprintf(w, " */\n")
	}

	fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	fprintf(w, "\n")
	fprintf(w, "    public static final %s Empty = new %s();\n", pt.name, pt.name)
	fprintf(w, "\n")

	// Declare each input property.
	for _, p := range props {
		if err := pt.genInputProperty(ctx, p, true); err != nil {
			return err
		}
		fprintf(w, "\n")
		fprintf(w, "\n")
	}

	// Generate the constructor.
	fprintf(w, "    public %s(", pt.name)

	// Generate the constructor parameters.
	for i, prop := range props {

		// TODO: factor this out (with similar code in genOutputType)
		paramName := names.Ident(prop.Name)
		paramType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			true,
			pt.state,
			false, // requireInitializers
			false, // outer optional
			false, // inputless overload
		).ToCode(ctx.imports)

		if i == 0 && len(props) > 1 { // first param
			fprintf(w, "\n")
		}

		terminator := ""
		if i != len(props)-1 { // not last param
			terminator = ",\n"
		}

		paramDef := fmt.Sprintf("%s %s%s", paramType, paramName, terminator)
		if len(props) > 1 {
			paramDef = fmt.Sprintf("        %s", paramDef)
		}
		fprintf(w, "%s", paramDef)
	}

	fprintf(w, ") {\n")

	// Generate the constructor body
	for _, prop := range props {
		paramName := names.Ident(prop.Name)
		fieldName := names.Ident(pt.mod.propertyName(prop))
		// set default values or assign given values
		var defaultValueCode string
		if prop.DefaultValue != nil {
			defaultValueString, defType, err := pt.mod.getDefaultValue(ctx, prop.DefaultValue, codegen.UnwrapType(prop.Type))
			if err != nil {
				return err
			}
			defaultValueInitializer := typeInitializer(ctx, unOptional(prop.Type), defaultValueString, defType)
			defaultValueCode = fmt.Sprintf("%s == null ? %s : ", paramName, defaultValueInitializer)
		}
		if prop.IsRequired() {
			fprintf(w, "        this.%s = %s%s.requireNonNull(%s, \"expected parameter '%s' to be non-null\");\n",
				fieldName, defaultValueCode, ctx.ref(names.Objects), paramName, paramName)
		} else {
			fprintf(w, "        this.%s = %s%s;\n", fieldName, defaultValueCode, paramName)
		}
	}
	fprintf(w, "    }\n")

	// Generate empty constructor, not that the instance created
	// with this constructor may not be valid if there are 'required' fields.
	if len(props) > 0 {
		fprintf(w, "\n")
		fprintf(w, "    private %s() {\n", pt.name)
		for _, prop := range props {
			fieldName := names.Ident(pt.mod.propertyName(prop))
			emptyValue := emptyTypeInitializer(ctx, prop.Type, true)
			fprintf(w, "        this.%s = %s;\n", fieldName, emptyValue)
		}
		fprintf(w, "    }\n")
	}

	// Generate the builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range props {
		requireInitializers := !pt.args || isInputType(prop.Type)
		propertyName := names.Ident(pt.mod.propertyName(prop))
		propertyType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			true,                // is input
			pt.state,            // is state
			requireInitializers, // requires initializers
			false,               // outer optional
			false,               // inputless overload
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.ToCode(ctx.imports),
			FieldName: propertyName.String(),
		})

		setterName := names.Ident(prop.Name).AsProperty().Setter()
		assignment := func(propertyName names.Ident) string {
			if prop.Secret {
				return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Output), propertyName)
			}
			if prop.IsRequired() {
				return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
			}
			return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
		}

		// add main setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName.String(),
			Assignment:   assignment(propertyName),
			ListType:     getListType(ctx, propertyType),
		})

		if isInputType(prop.Type) { // we have a wrapped field so we add an unwrapped helper setter
			var typ schema.Type = &schema.OptionalType{ElementType: codegen.UnwrapType(prop.Type)}
			if prop.IsRequired() {
				typ = codegen.UnwrapType(typ)
			}
			propertyTypeUnwrapped := pt.mod.typeString(
				ctx,
				typ,
				pt.propertyTypeQualifier,
				true,                // is input
				pt.state,            // is state
				requireInitializers, // requires initializers
				false,               // outer optional
				true,                // inputless overload
			)

			assignmentUnwrapped := func(propertyName names.Ident) string {
				if prop.Secret {
					return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Output), propertyName)
				}
				if prop.IsRequired() {
					return fmt.Sprintf("this.%s = %s.of(%s.requireNonNull(%s))",
						propertyName, ctx.ref(names.Output), ctx.ref(names.Objects), propertyName)
				}
				return fmt.Sprintf("this.%s = %s.ofNullable(%s)", propertyName, ctx.ref(names.Output), propertyName)
			}

			if !propertyTypeUnwrapped.Equal(propertyType) {
				// add overloaded setter
				builderSetters = append(builderSetters, builderSetterTemplateContext{
					SetterName:   setterName,
					PropertyType: propertyTypeUnwrapped.ToCode(ctx.imports),
					PropertyName: propertyName.String(),
					Assignment:   assignmentUnwrapped(propertyName),
					ListType:     getListType(ctx, propertyTypeUnwrapped),
				})
			}
		}
	}

	fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     "    ",
		Name:       "Builder",
		IsFinal:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
	}); err != nil {
		return err
	}
	fprintf(w, "\n")

	// Close the class.
	fprintf(w, "}\n")

	return nil
}

func (pt *plainType) genOutputType(ctx *classFileContext) error {
	if len(pt.properties) > 250 {
		return pt.genJumboOutputType(ctx)
	}
	return pt.genNormalOutputType(ctx)
}

func (pt *plainType) genJumboOutputType(ctx *classFileContext) error {
	// generates a class for Outputs where pt.properties >= 250
	w := ctx.writer
	const indent = "    "

	props := pt.properties

	// Open the class and annotate it appropriately.
	fprintf(w, "@%s\n", ctx.ref(names.CustomType))
	fprintf(w, "public final class %s {\n", pt.name)

	// Generate each output field.
	for _, prop := range props {
		fieldName := names.Ident(pt.mod.propertyName(prop))
		fieldType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false,
			false, // outer optional
			false, // inputless overload
		)
		if prop.Comment != "" || prop.DeprecationMessage != "" {
			fprintf(w, "    /**\n")
			if prop.Comment != "" {
				fprintf(w, "%s\n", formatBlockComment(prop.Comment, indent))
			}
			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @Deprecated\n")
				fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, indent))

			}
			fprintf(w, "     */\n")
		}
		printObsoleteAttribute(ctx, prop.DeprecationMessage, indent)
		fprintf(w, "    private %s %s;\n", fieldType.ToCode(ctx.imports), fieldName)
	}
	if len(props) > 0 {
		fprintf(w, "\n")
	}

	// Generate the constructor parameter names - used as a workaround for Java reflection issues
	var paramNamesStringBuilder strings.Builder
	paramNamesStringBuilder.WriteString("{")
	for i, prop := range props {
		if i > 0 {
			paramNamesStringBuilder.WriteString(",")
		}
		paramName := names.Ident(prop.Name)
		paramNamesStringBuilder.WriteString("\"" + paramName.String() + "\"")
	}
	paramNamesStringBuilder.WriteString("}")

	// Generate an appropriately-attributed constructor that will set this types' fields.
	fprintf(w, "    @%s.Constructor\n", ctx.ref(names.CustomType))
	// Generate empty constructor, not that the instance created
	// with this constructor may not be valid if there are 'required' fields.
	if len(props) > 0 {
		fprintf(w, "\n")
		fprintf(w, "    private %s() {\n", pt.name)
		for _, prop := range props {
			fieldName := names.Ident(pt.mod.propertyName(prop))
			emptyValue := emptyTypeInitializer(ctx, prop.Type, true)
			fprintf(w, "        this.%s = %s;\n", fieldName, emptyValue)
		}
		fprintf(w, "    }\n")
	}

	// Generate getters
	for _, prop := range props {
		if prop.Comment != "" || prop.DeprecationMessage != "" {
			fprintf(w, "    /**\n")
			if prop.Comment != "" {
				fprintf(w, "%s\n", formatBlockComment(prop.Comment, indent))
			}

			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @Deprecated\n")
				fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, indent))

			}
			fprintf(w, "     */\n")
		}
		paramName := names.Ident(prop.Name)
		getterName := names.Ident(prop.Name).AsProperty().Getter()
		getterType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
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

		printObsoleteAttribute(ctx, prop.DeprecationMessage, indent)
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
			false, // is state
			false, // requires initializers
			false, // outer optional
			false, // inputless overload
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.ToCode(ctx.imports),
			FieldName: propertyName.String(),
		})

		setterName := names.Ident(prop.Name).AsProperty().Setter()
		assignment := func(propertyName names.Ident) string {
			if prop.IsRequired() {
				return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
			}
			return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
		}

		// add setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName.String(),
			Assignment:   assignment(propertyName),
			ListType:     getListType(ctx, propertyType),
		})
	}

	fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     indent,
		Name:       "Builder",
		IsFinal:    true,
		IsJumbo:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
	}); err != nil {
		return err
	}
	fprintf(w, "\n")

	// Close the class.
	fprintf(w, "}\n")
	return nil
}

func (pt *plainType) genNormalOutputType(ctx *classFileContext) error {
	w := ctx.writer
	const indent = "    "

	props := pt.properties

	// Open the class and annotate it appropriately.
	fprintf(w, "@%s\n", ctx.ref(names.CustomType))
	fprintf(w, "public final class %s {\n", pt.name)

	// Generate each output field.
	for _, prop := range props {
		fieldName := names.Ident(pt.mod.propertyName(prop))
		fieldType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false,
			false, // outer optional
			false, // inputless overload
		)
		if prop.Comment != "" || prop.DeprecationMessage != "" {
			fprintf(w, "    /**\n")
			if prop.Comment != "" {
				fprintf(w, "%s\n", formatBlockComment(prop.Comment, indent))
			}
			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @Deprecated\n")
				fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, indent))

			}
			fprintf(w, "     */\n")
		}
		printObsoleteAttribute(ctx, prop.DeprecationMessage, indent+"    ")
		fprintf(w, "    private final %s %s;\n", fieldType.ToCode(ctx.imports), fieldName)
	}
	if len(props) > 0 {
		fprintf(w, "\n")
	}

	// Generate an appropriately-attributed constructor that will set this types' fields.
	fprintf(w, "    @%s.Constructor\n", ctx.ref(names.CustomType))
	fprintf(w, "    private %s(", pt.name)

	// Generate the constructor parameters.
	for i, prop := range props {
		// TODO: factor this out (with similar code in genInputType)
		paramName := names.Ident(prop.Name)
		paramType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false,
			false, // outer optional
			false, // inputless overload
		)

		if i == 0 && len(props) > 1 { // first param
			fprintf(w, "\n")
		}

		terminator := ""
		if i != len(props)-1 { // not last param
			terminator = ",\n"
		}

		paramDef := fmt.Sprintf("%s %s %s%s",
			fmt.Sprintf("@%s.Parameter(\"%s\")", ctx.ref(names.CustomType), prop.Name),
			paramType.ToCode(ctx.imports), paramName, terminator)
		if len(props) > 1 {
			paramDef = fmt.Sprintf("        %s", paramDef)
		}
		fprintf(w, "%s", paramDef)
	}

	fprintf(w, ") {\n")

	// Generate the constructor body.
	for _, prop := range props {
		paramName := names.Ident(prop.Name)
		fieldName := names.Ident(pt.mod.propertyName(prop))

		// Never `Objects.requireNotNull` here because we need
		// to tolerate providers failing to return required props.
		//
		// See https://github.com/pulumi/pulumi-java/issues/164
		fprintf(w, "        this.%s = %s;\n", fieldName, paramName)

	}
	fprintf(w, "    }\n")
	fprintf(w, "\n")

	// Generate getters
	for _, prop := range props {
		if prop.Comment != "" || prop.DeprecationMessage != "" {
			fprintf(w, "    /**\n")
			if prop.Comment != "" {
				fprintf(w, "%s\n", formatBlockComment(prop.Comment, indent))
			}

			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @Deprecated\n")
				fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, indent))

			}
			fprintf(w, "    */\n")
		}
		paramName := names.Ident(prop.Name)
		getterName := names.Ident(prop.Name).AsProperty().Getter()
		getterType := pt.mod.typeString(
			ctx,
			prop.Type,
			pt.propertyTypeQualifier,
			false,
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

		printObsoleteAttribute(ctx, prop.DeprecationMessage, indent)
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
			false, // is state
			false, // requires initializers
			false, // outer optional
			false, // inputless overload
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.ToCode(ctx.imports),
			FieldName: propertyName.String(),
		})

		setterName := names.Ident(prop.Name).AsProperty().Setter()
		assignment := func(propertyName names.Ident) string {
			if prop.IsRequired() {
				return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
			}
			return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
		}

		// add setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName.String(),
			Assignment:   assignment(propertyName),
			ListType:     getListType(ctx, propertyType),
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
	}); err != nil {
		return err
	}
	fprintf(w, "\n")

	// Close the class.
	fprintf(w, "}\n")
	return nil
}

func primitiveValue(value interface{}) (string, string, error) {
	v := reflect.ValueOf(value)
	if v.Kind() == reflect.Interface {
		v = v.Elem()
	}

	switch v.Kind() {
	case reflect.Bool:
		if v.Bool() {
			return "true", "boolean", nil
		}
		return "false", "boolean", nil
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32:
		return strconv.FormatInt(v.Int(), 10), "integer", nil
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32:
		return strconv.FormatUint(v.Uint(), 10), "integer", nil
	case reflect.Float32, reflect.Float64:
		return strconv.FormatFloat(v.Float(), 'e', -1, 64), "number", nil
	case reflect.String:
		return fmt.Sprintf("%q", v.String()), "string", nil
	default:
		return "", "", errors.Errorf("unsupported default value of type %T", value)
	}
}

func (mod *modContext) getDefaultValue(
	ctx *classFileContext,
	dv *schema.DefaultValue,
	t schema.Type,
) (string, string, error) {
	var val string
	schemaType := t.String()
	if dv.Value != nil {
		switch enumOrUnion := t.(type) {
		case *schema.EnumType:
			pkg, err := parsePackageName(mod.packageName)
			if err != nil {
				return "", "", err
			}
			enumName := tokenToName(enumOrUnion.Token)
			for _, e := range enumOrUnion.Elements {
				if e.Value != dv.Value {
					continue
				}

				elName := e.Name
				if elName == "" {
					elName = fmt.Sprintf("%v", e.Value)
				}
				safeName, err := names.MakeSafeEnumName(elName, enumName)
				if err != nil {
					return "", "", err
				}
				val = pkg.Dot(names.Ident("enums")).Dot(names.Ident(enumName)).Dot(names.Ident(safeName)).String()
				break
			}
			if val == "" {
				return "", "", errors.Errorf("default value '%v' not found in enum '%s'", dv.Value, enumName)
			}
		default:
			v, st, err := primitiveValue(dv.Value)
			if err != nil {
				return "", "", err
			}
			val = v
			schemaType = st
		}
	}

	if len(dv.Environment) != 0 {
		getType := ""
		switch t {
		case schema.BoolType:
			getType = "Boolean"
		case schema.IntType:
			getType = "Integer"
		case schema.NumberType:
			getType = "Double"
		}

		envVars := fmt.Sprintf("%q", dv.Environment[0])
		for _, e := range dv.Environment[1:] {
			envVars += fmt.Sprintf(", %q", e)
		}

		getEnv := fmt.Sprintf("%s.getEnv%s(%s).orElse(null)", mod.utilitiesRef(ctx), getType, envVars)
		if val != "" {
			val = fmt.Sprintf("%s == null ? %s : %s", getEnv, val, getEnv)
		} else {
			val = getEnv
		}
	}

	return val, schemaType, nil
}

func genAlias(ctx *classFileContext, alias *schema.Alias) {
	w := ctx.writer
	fprintf(w, "%s.of(", ctx.ref(names.Output))
	fprintf(w, "%s.builder()", ctx.ref(names.Alias))
	if alias.Name != nil {
		fprintf(w, ".name(\"%v\")", *alias.Name)
	}
	if alias.Project != nil {
		fprintf(w, ".project(\"%v\")", *alias.Project)
	}
	if alias.Type != nil {
		fprintf(w, ".type(\"%v\")", *alias.Type)
	}
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
			fprintf(w, "%s\n", formatBlockComment(r.Comment, ""))
		}

		if r.DeprecationMessage != "" {
			fprintf(w, " * @Deprecated\n")
			fprintf(w, "%s\n", formatBlockComment(r.DeprecationMessage, ""))

		}
		fprintf(w, " */\n")
	}

	// Open the class.
	className := name
	var baseType string
	optionsType := "io.pulumi.resources.CustomResourceOptions"
	switch {
	case r.IsProvider:
		baseType = "io.pulumi.resources.ProviderResource"
	case r.IsComponent:
		baseType = "io.pulumi.resources.ComponentResource"
		optionsType = "io.pulumi.resources.ComponentResourceOptions"
	default:
		baseType = "io.pulumi.resources.CustomResource"
	}

	printObsoleteAttribute(ctx, r.DeprecationMessage, "")
	fprintf(w, "@%s(type=\"%s\")\n",
		ctx.imports.Ref(names.ResourceType),
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
			"outputs",
			false,
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
				fprintf(w, "%s\n", formatBlockComment(prop.Comment, "    "))
			}

			if prop.DeprecationMessage != "" {
				fprintf(w, "     * @Deprecated\n")
				fprintf(w, "%s\n", formatBlockComment(prop.DeprecationMessage, "    "))

			}
			fprintf(w, "     */\n")
		}

		outputExportParameters := strings.Join(
			propertyType.ParameterTypesTransformed(func(ts TypeShape) string {
				return ts.ToCodeWithOptions(ctx.imports, TypeShapeStringOptions{
					CommentOutAnnotations: true,
					GenericErasure:        true,
					AppendClassLiteral:    true,
				})
			}),
			", ",
		)
		outputExportType := propertyType.ToCodeWithOptions(ctx.imports, TypeShapeStringOptions{
			SkipAnnotations:    true,
			GenericErasure:     true,
			AppendClassLiteral: true,
		})
		outputParameterType := propertyType.ToCodeWithOptions(ctx.imports, TypeShapeStringOptions{
			CommentOutAnnotations: true,
		})
		printObsoleteAttribute(ctx, prop.DeprecationMessage, "    ")
		fprintf(w,
			"    @%s(name=\"%s\", type=%s, parameters={%s})\n",
			ctx.ref(names.Export), wireName, outputExportType, outputExportParameters)
		fprintf(w,
			"    private %s<%s> %s;\n", ctx.imports.Ref(names.Output), outputParameterType, propertyName)
		fprintf(w, "\n")

		if prop.Comment != "" {
			fprintf(w, "    /**\n")
			fprintf(w, "%s\n", formatBlockComment("@return "+prop.Comment, "    "))
			fprintf(w, "     */\n")
		}

		// Add getter
		getterType := outputParameterType
		getterName := names.Ident(prop.Name).AsProperty().Getter()
		fprintf(w, "    public %s<%s> %s() {\n", ctx.imports.Ref(names.Output), getterType, getterName)
		fprintf(w, "        return this.%s;\n", propertyName)
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
		tok = mod.pkg.Name
	}

	argsOverride := fmt.Sprintf("args == null ? %s.Empty : args", ctx.ref(argsFQN))
	if hasConstInputs {
		argsOverride = "makeArgs(args)"
	}

	// Name only constructor
	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     */\n")
	fprintf(w, "    public %s(String name) {\n", className)
	fprintf(w, "        this(name, %s.Empty);\n", ctx.ref(argsFQN))
	fprintf(w, "    }\n")

	// Name+Args constructor

	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     * @param args The arguments to use to populate this resource's properties.\n")
	fprintf(w, "     */\n")
	fprintf(w, "    public %s(String name, %s args) {\n", className, argsType)
	fprintf(w, "        this(name, args, null);\n")
	fprintf(w, "    }\n")

	// Constructor
	isComponent := ""
	if r.IsComponent {
		isComponent = ", true"
	}
	fprintf(w, "    /**\n")
	fprintf(w, "     *\n")
	fprintf(w, "     * @param name The _unique_ name of the resulting resource.\n")
	fprintf(w, "     * @param args The arguments to use to populate this resource's properties.\n")
	fprintf(w, "     * @param options A bag of options that control this resource's behavior.\n")
	fprintf(w, "     */\n")

	fprintf(w, "    public %s(String name, %s args, @%s %s options) {\n",
		className, argsType, ctx.ref(names.Nullable), optionsType)
	fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, %s.empty())%s);\n",
		tok, argsOverride, ctx.imports.Ref(names.Output), isComponent)
	fprintf(w, "    }\n")

	// Write a private constructor for the use of `get`.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", "null"
		if r.StateInputs != nil {
			stateParam, stateRef = fmt.Sprintf("@%s %s state, ", ctx.ref(names.Nullable), ctx.ref(stateFQN)), "state"
		}

		fprintf(w, "\n")
		fprintf(w, "    private %s(String name, %s<String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Output), stateParam, ctx.ref(names.Nullable), optionsType)
		fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, id));\n", tok, stateRef)
		fprintf(w, "    }\n")
	}

	if hasConstInputs {
		// Write the method that will calculate the resource arguments.
		fprintf(w, "\n")
		fprintf(w, "    private static %s makeArgs(%s args) {\n", ctx.ref(argsFQN), argsType)
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
		fprintf(w, "    }\n")
	}

	// Write the method that will calculate the resource options.
	fprintf(w, "\n")
	fprintf(w,
		"    private static %[1]s makeResourceOptions(@%[2]s %[1]s options, @%[2]s %[3]s<String> id) {\n",
		optionsType, ctx.ref(names.Nullable), ctx.ref(names.Output))
	fprintf(w, "        var defaultOptions = %s.builder()\n", optionsType)
	fprintf(w, "            .version(%s.getVersion())\n", mod.utilitiesRef(ctx))

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

		fprintf(w, "    public static %s get(String name, %s<String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Output), stateParam, ctx.ref(names.Nullable), optionsType)
		fprintf(w, "        return new %s(name, id, %soptions);\n", className, stateRef)
		fprintf(w, "    }\n")
	}

	// Close the class.
	fprintf(w, "}\n")

	return nil
}

func (mod *modContext) genFunction(ctx *classFileContext, fun *schema.Function, argsFQN, resultFQN names.FQN) error {
	w := ctx.writer
	className := tokenToFunctionName(fun.Token)

	var typeParameter string
	if fun.Outputs != nil {
		typeParameter = ctx.imports.Ref(resultFQN)
	} else {
		typeParameter = ctx.imports.Ref(names.Void)
	}

	var argsParamDef string
	argsParamRef := "io.pulumi.resources.InvokeArgs.Empty"

	if fun.Inputs != nil {
		allOptionalInputs := true
		for _, prop := range fun.Inputs.Properties {
			allOptionalInputs = allOptionalInputs && !prop.IsRequired()
		}

		var nullable string
		if allOptionalInputs {
			// If the number of required input properties was zero, we can make the args object optional.
			nullable = fmt.Sprintf("@%s ", ctx.ref(names.Nullable))
		}

		argsParamDef = fmt.Sprintf("%s%s args, ", nullable, ctx.ref(argsFQN))
		argsParamRef = fmt.Sprintf("args == null ? %s.Empty : args", ctx.ref(argsFQN))
	}

	printObsoleteAttribute(ctx, fun.DeprecationMessage, "")
	// Open the class we'll use for datasources.
	fprintf(w, "public class %s {\n", className)

	// [pulumi/pulumi-java#197]
	fprintf(w, "    private %s() {}\n", className)

	indent := "    "
	// Emit javadoc
	if fun.Comment != "" || fun.DeprecationMessage != "" {
		fprintf(w, "    /**\n")
		fprintf(w, "    %s\n", formatBlockComment(fun.Comment, indent))
		if fun.Inputs != nil && fun.Inputs.Comment != "" {
			fprintf(w, "     *\n")
			fprintf(w, "    %s\n", formatBlockComment(fun.Inputs.Comment, indent))
		}
		if fun.Outputs != nil && fun.Outputs.Comment != "" {
			fprintf(w, "     *\n")
			fprintf(w, "    %s\n", formatBlockComment(fun.Outputs.Comment, indent))
		}
		if fun.DeprecationMessage != "" {
			fprintf(w, "     * @Deprecated\n")
			fprintf(w, "    %s\n", formatBlockComment(fun.DeprecationMessage, indent))
		}
		fprintf(w, "     */\n")
	}

	// Emit the datasource method.
	printObsoleteAttribute(ctx, fun.DeprecationMessage, "    ")
	fprintf(w, "    public static %s<%s> invokeAsync(%s@%s %s options) {\n",
		ctx.ref(names.CompletableFuture), typeParameter, argsParamDef, ctx.ref(names.Nullable), ctx.ref(names.InvokeOptions))
	fprintf(w,
		"        return %s.getInstance().invokeAsync(\"%s\", %s.of(%s.class), %s, %s.withVersion(options));\n",
		ctx.ref(names.Deployment), fun.Token, ctx.ref(names.TypeShape), typeParameter, argsParamRef, mod.utilitiesRef(ctx))
	fprintf(w, "    }\n")

	// Close the class.
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

func (mod *modContext) genEnum(ctx *classFileContext, qualifier string, enum *schema.EnumType) error {
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
		fprintf(w, "%s\n", formatBlockComment(enum.Comment, indent))
		fprintf(w, "%s */\n", indent)
	}

	underlyingType := mod.typeString(
		ctx,
		enum.ElementType,
		qualifier,
		false,
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
					fprintf(w, "%s\n", formatBlockComment(e.Comment, indent))
				}

				if e.DeprecationMessage != "" {
					fprintf(w, "%s * @Deprecated\n", indent)
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
		fprintf(w, "%spublic String toString() {\n", indent)
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

func visitObjectTypes(properties []*schema.Property, visitor func(*schema.ObjectType)) {
	codegen.VisitTypeClosure(properties, func(t schema.Type) {
		if o, ok := t.(*schema.ObjectType); ok {
			visitor(o)
		}
	})
}

func (mod *modContext) genType(
	ctx *classFileContext,
	obj *schema.ObjectType,
	propertyTypeQualifier string,
	input, state bool,
) error {
	pt := &plainType{
		mod:                   mod,
		name:                  mod.typeName(obj, state, obj.IsInputShape()),
		comment:               obj.Comment,
		propertyTypeQualifier: propertyTypeQualifier,
		properties:            obj.Properties,
		state:                 state,
		args:                  obj.IsInputShape(),
	}

	contract.Assertf(pt.name == ctx.className.String(), "This is required by the java compiler")
	if input {
		pt.baseClass = "io.pulumi.resources.ResourceArgs"
		if !obj.IsInputShape() {
			pt.baseClass = "io.pulumi.resources.InvokeArgs"
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

func (mod *modContext) getConfigProperty(
	ctx *classFileContext,
	schemaType schema.Type,
	key string,
) (TypeShape, MethodCall) {
	typeShape := func(t schema.Type) TypeShape {
		return mod.typeString(
			ctx,
			t,
			"inputs",
			false,
			false,
			true,  // requireInitializers - set to true so we preserve Optional
			true,  // outer optional
			false, // inputless overload
		)
	}

	getFunc := MethodCall{
		This: "config",
		Args: []string{fmt.Sprintf("%q", key)},
	}
	switch schemaType {
	case schema.StringType:
		getFunc.Method = "get"
	case schema.BoolType:
		getFunc.Method = "getBoolean"
	case schema.IntType:
		getFunc.Method = "getInteger"
	case schema.NumberType:
		getFunc.Method = "getDouble"
	default:
		switch t := schemaType.(type) {
		case *schema.TokenType:
			if t.UnderlyingType != nil {
				return mod.getConfigProperty(ctx, t.UnderlyingType, key)
			}
		}
		// TODO: C# has a special case for Arrays here, should we port it?

		// Calling this SDK method:
		//
		// public <T> Optional<T> getObject(String key, TypeShape<T> shapeOfT)
		getFunc.Method = "getObject"

		// TODO it is unclear if `getObject` can handle nested
		// optionals (that is, GSON deserializer can read
		// them). We do not handle that case, but we must
		// handle outer Optional which is quite frequent. For
		// now we simply drop it, so if the type is
		// Optional(X) we pass X as the TypeShape to
		// `getObject`.
		t := unOptional(schemaType)
		getFunc.Args = append(getFunc.Args, typeShape(t).StringJavaTypeShape(ctx.imports))
	}

	return typeShape(schemaType), getFunc
}

func (mod *modContext) genConfig(ctx *classFileContext, variables []*schema.Property) error {
	w := ctx.writer

	// Open the config class.
	fprintf(w, "public final class Config {\n")
	fprintf(w, "\n")
	// Create a config bag for the variables to pull from.
	fprintf(w, "    private static final io.pulumi.Config config = io.pulumi.Config.of(%q);", mod.pkg.Name)
	fprintf(w, "\n")

	// Emit an entry for all config variables.
	for _, p := range variables {

		typ := p.Type

		// TODO mini-awsnative.json example seems to hit this
		// case where a prop is required but has no default
		// value; where should the default be coming from?
		// Should we be generating code that throws?
		if p.IsRequired() && p.DefaultValue == nil {
			typ = &schema.OptionalType{ElementType: typ}
		}

		propertyType, getFunc := mod.getConfigProperty(ctx, typ, p.Name)
		propertyName := names.Ident(mod.propertyName(p))
		returnStatement := getFunc.String()

		if p.DefaultValue != nil {
			defaultValueString, defType, err := mod.getDefaultValue(ctx, p.DefaultValue, typ)
			if err != nil {
				return err
			}
			defaultValueInitializer := typeInitializer(ctx, typ, defaultValueString, defType)
			returnStatement = fmt.Sprintf("%s.combine(%s, %s)",
				ctx.ref(names.Optionals),
				returnStatement,
				defaultValueInitializer)
		}

		if p.Comment != "" {
			fprintf(w, "/**\n")
			fprintf(w, "%s\n", formatBlockComment(p.Comment, ""))
			fprintf(w, " */\n")
		}

		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          "    ",
			GetterType:      propertyType.ToCode(ctx.imports),
			GetterName:      propertyName.String(),
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
	return path.Join("src", "main", "java")
}

func (mod *modContext) gen(fs fs) error {
	pkgComponents := strings.Split(mod.packageName, ".")

	dir := path.Join(gradleProjectPath(), path.Join(pkgComponents...))

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

	addClassFile := func(pkg names.FQN, className names.Ident, contents string) {
		fqn := pkg.Dot(className)
		relPath := path.Join(strings.Split(fqn.String(), ".")...)
		path := path.Join(gradleProjectPath(), relPath) + ".java"
		files = append(files, path)
		fs.add(path, []byte(contents))
	}

	addClass := func(javaPkg names.FQN, javaClass names.Ident, gen func(*classFileContext) error) error {
		javaCode, err := genClassFile(javaPkg, javaClass, gen)
		if err != nil {
			return err
		}
		addClassFile(javaPkg, javaClass, fmt.Sprintf("%s\n%s", mod.genHeader(), javaCode))
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
			pkgName, err := parsePackageName(packageName(mod.packages, mod.pkg.Name))
			if err != nil {
				return err
			}
			return jvmUtilitiesTemplate.Execute(ctx.writer, jvmUtilitiesTemplateContext{
				Name:        pkgName.String(),
				PackagePath: strings.ReplaceAll(mod.packageName, ".", "/"),
				ClassName:   "Utilities",
				Tool:        mod.tool,
			})
		}); err != nil {
			return err
		}

		// Ensure that the target module directory contains a README.md file.
		readme := mod.pkg.Description
		if readme != "" && readme[len(readme)-1] != '\n' {
			readme += "\n"
		}
		fs.add("README.md", []byte(readme))
	case "config":
		if len(mod.pkg.Config) > 0 {
			configPkg, err := parsePackageName(mod.configClassPackageName)
			if err != nil {
				return err
			}
			if err := addClass(configPkg, names.Ident("Config"), func(ctx *classFileContext) error {
				return mod.genConfig(ctx, mod.pkg.Config)
			}); err != nil {
				return err
			}
		}
	}

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
				baseClass:             "io.pulumi.resources.ResourceArgs",
				propertyTypeQualifier: "inputs",
				properties:            r.InputProperties,
				args:                  true,
			}
			return args.genInputType(ctx)
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
					baseClass:             "io.pulumi.resources.ResourceArgs",
					propertyTypeQualifier: "inputs",
					properties:            r.StateInputs.Properties,
					args:                  true,
					state:                 true,
				}
				return state.genInputType(ctx)
			}); err != nil {
				return err
			}
		}
	}

	// Functions
	for _, f := range mod.functions {

		// TODO[pulumi/pulumi-jvm#262]: Support proper codegen for methods
		if f.IsMethod {
			continue
		}

		if f.IsOverlay {
			// This function code is generated by the provider, so no further action is required.
			continue
		}

		outputsPkg := javaPkg.Dot(names.Ident("outputs"))
		resultClass := names.Ident(tokenToName(f.Token) + "Result")
		resultFQN := outputsPkg.Dot(resultClass)
		inputsPkg := javaPkg.Dot(names.Ident("inputs"))
		argsClass := names.Ident(tokenToName(f.Token) + "Args")
		argsFQN := inputsPkg.Dot(argsClass)

		if err := addClass(javaPkg, names.Ident(tokenToName(f.Token)), func(ctx *classFileContext) error {
			return mod.genFunction(ctx, f, argsFQN, resultFQN)
		}); err != nil {
			return err
		}

		// Emit the args and result types, if any.
		if f.Inputs != nil {
			if err := addClass(inputsPkg, argsClass, func(ctx *classFileContext) error {
				args := &plainType{
					mod:                   mod,
					name:                  ctx.className.String(),
					baseClass:             "io.pulumi.resources.InvokeArgs",
					propertyTypeQualifier: "inputs",
					properties:            f.Inputs.Properties,
				}
				return args.genInputType(ctx)
			}); err != nil {
				return err
			}
		}

		if f.Outputs != nil {
			if err := addClass(outputsPkg, resultClass, func(ctx *classFileContext) error {
				res := &plainType{
					mod:                   mod,
					name:                  ctx.className.String(),
					propertyTypeQualifier: "outputs",
					properties:            f.Outputs.Properties,
				}
				contract.Assert(resultClass.String() == res.name)
				return res.genOutputType(ctx)
			}); err != nil {
				return err
			}
		}
	}

	// Input/Output types
	inputsPkg := javaPkg.Dot(names.Ident("inputs"))
	for _, t := range mod.types {
		if t.IsOverlay {
			// This type is generated by the provider, so no further action is required.
			continue
		}
		var plainTypeClassName names.Ident
		if mod.details(t).plainType {
			t := t
			if t.IsInputShape() {
				t = t.PlainShape
			}
			plainTypeClassName = names.Ident(mod.typeName(t, false, t.IsInputShape()))
			if err := addClass(inputsPkg, plainTypeClassName, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "inputs", true, false)
			}); err != nil {
				return err
			}
		}
		if mod.details(t).inputType {
			className := names.Ident(mod.typeName(t, false, t.IsInputShape()))
			// Avoid name collision with `plainType` by
			// avoiding the `inputType` generation. The
			// naming scheme might need revisiting so both
			// can be accommodated.
			if !inputsPkg.Dot(plainTypeClassName).Equal(inputsPkg.Dot(className)) {
				if err := addClass(inputsPkg, className, func(ctx *classFileContext) error {
					return mod.genType(ctx, t, "inputs", true, false)
				}); err != nil {
					return err
				}
			}
		}
		if mod.details(t).stateType {
			className := names.Ident(mod.typeName(t, true, t.IsInputShape()))
			if err := addClass(javaPkg.Dot(names.Ident("inputs")), className, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "inputs", true, true)
			}); err != nil {
				return err
			}
		}
		if mod.details(t).outputType {
			className := names.Ident(mod.typeName(t, false, t.IsInputShape()))
			if err := addClass(javaPkg.Dot(names.Ident("outputs")), className, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "outputs", false, false)
			}); err != nil {
				return err
			}
		}
	}

	// Enums
	if len(mod.enums) > 0 {
		for _, enum := range mod.enums {
			enumClassName := names.Ident(tokenToName(enum.Token))
			if err := addClass(javaPkg.Dot(names.Ident("enums")), enumClassName, func(ctx *classFileContext) error {
				return mod.genEnum(ctx, "enums", enum)
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
		if info, ok := p.Language["jvm"].(PropertyInfo); ok && info.Name != "" {
			names[p] = info.Name
		}
	}
}

func generateModuleContextMap(tool string, pkg *schema.Package) (map[string]*modContext, *PackageInfo, error) {
	// Decode Java-specific info for each package as we discover them.
	infos := map[*schema.Package]*PackageInfo{}
	var getPackageInfo = func(p *schema.Package) *PackageInfo {
		info, ok := infos[p]
		if !ok {
			if err := p.ImportLanguages(map[string]schema.Language{"jvm": Importer}); err != nil {
				panic(err)
			}

			var jvmInfo PackageInfo
			if raw, ok := pkg.Language["jvm"]; ok {
				jvmInfo, ok = raw.(PackageInfo)
				if !ok {
					panic(fmt.Sprintf("Failed to cast `pkg.Language[\"jvm\"]`=%v to `PackageInfo`", raw))
				}
			}
			info = &jvmInfo
			infos[p] = info
		}
		return info
	}
	infos[pkg] = getPackageInfo(pkg)

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
	details := map[*schema.ObjectType]*typeDetails{}

	var getMod func(modName string, p *schema.Package) *modContext
	getMod = func(modName string, p *schema.Package) *modContext {
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
				pkg:                    p,
				mod:                    modName,
				tool:                   tool,
				packageName:            pkgName,
				rootPackageName:        rootPackage,
				basePackageName:        basePackage,
				packages:               info.Packages,
				typeDetails:            details,
				propertyNames:          propertyNames,
				dictionaryConstructors: info.DictionaryConstructors,
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
			if p == pkg {
				modules[modName] = mod
			}
		}
		return mod
	}

	getModFromToken := func(token string, p *schema.Package) *modContext {
		return getMod(p.TokenToModule(token), p)
	}

	// Create the config module if necessary.
	if len(pkg.Config) > 0 {
		cfg := getMod("config", pkg)
		cfg.configClassPackageName = cfg.basePackageName + packageName(infos[pkg].Packages, pkg.Name)
	}

	visitObjectTypes(pkg.Config, func(t *schema.ObjectType) {
		getModFromToken(t.Token, pkg).details(t).plainType = true
	})

	// Find input and output types referenced by resources.
	scanResource := func(r *schema.Resource) {
		mod := getModFromToken(r.Token, pkg)
		mod.resources = append(mod.resources, r)
		visitObjectTypes(r.Properties, func(t *schema.ObjectType) {
			getModFromToken(t.Token, t.Package).details(t).outputType = true
		})
		visitObjectTypes(r.InputProperties, func(t *schema.ObjectType) {
			if r.IsProvider {
				getModFromToken(t.Token, t.Package).details(t).outputType = true
			}
			getModFromToken(t.Token, t.Package).details(t).inputType = true
		})

		// We don't generate plain input types unless we use them. To always
		// generate them, we can move the following line to the above function.
		// It looks like the C# codegen always generates them.
		visitPlainObjectTypes(r.InputProperties, func(t *schema.ObjectType) {
			getModFromToken(t.Token, t.Package).details(t).plainType = true
		})
		if r.StateInputs != nil {
			visitObjectTypes(r.StateInputs.Properties, func(t *schema.ObjectType) {
				getModFromToken(t.Token, t.Package).details(t).inputType = true
				getModFromToken(t.Token, t.Package).details(t).stateType = true
			})
		}
	}

	scanResource(pkg.Provider)
	for _, r := range pkg.Resources {
		scanResource(r)
	}

	// Find input and output types referenced by functions.
	for _, f := range pkg.Functions {
		mod := getModFromToken(f.Token, pkg)
		mod.functions = append(mod.functions, f)
		if f.Inputs != nil {
			visitObjectTypes(f.Inputs.Properties, func(t *schema.ObjectType) {
				details := getModFromToken(t.Token, t.Package).details(t)
				details.inputType = true
				details.plainType = true
			})
		}
		if f.Outputs != nil {
			visitObjectTypes(f.Outputs.Properties, func(t *schema.ObjectType) {
				details := getModFromToken(t.Token, t.Package).details(t)
				details.outputType = true
				details.plainType = true
			})
		}
	}

	// Find nested types.
	for _, t := range pkg.Types {
		switch typ := codegen.UnwrapType(t).(type) {
		case *schema.ObjectType:
			mod := getModFromToken(typ.Token, pkg)
			mod.types = append(mod.types, typ)
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

// genGradleProject generates gradle files
func genGradleProject(pkg *schema.Package,
	basePackageName string,
	packageName string,
	packageInfo PackageInfo,
	files fs) error {
	genSettingsFile, err := genSettingsFile(basePackageName + packageName)
	if err != nil {
		return err
	}
	files.add("settings.gradle", genSettingsFile)
	genBuildFile, err := genBuildFile(pkg.Name, basePackageName, packageInfo)
	if err != nil {
		return err
	}
	files.add("build.gradle", genBuildFile)
	return nil
}

// genSettingsFile emits settings.gradle
func genSettingsFile(packageName string) ([]byte, error) {
	w := &bytes.Buffer{}
	err := jvmSettingsTemplate.Execute(w, jvmSettingsTemplateContext{
		PackageName: packageName,
	})
	if err != nil {
		return nil, err
	}
	return w.Bytes(), nil
}

// genBuildFile emits build.gradle
func genBuildFile(name string, basePackageName string, pkgInfo PackageInfo) ([]byte, error) {
	w := &bytes.Buffer{}
	err := jvmBuildTemplate.Execute(w, jvmBuildTemplateContext{
		Name:            name,
		BasePackageName: strings.TrimSuffix(basePackageName, "."),
		PackageInfo:     pkgInfo,
	})
	if err != nil {
		return nil, err
	}
	return w.Bytes(), nil
}

func GeneratePackage(tool string, pkg *schema.Package, extraFiles map[string][]byte) (map[string][]byte, error) {
	modules, info, err := generateModuleContextMap(tool, pkg)
	if err != nil {
		return nil, err
	}

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

	// Finally emit the package metadata.
	if err := genGradleProject(
		pkg,
		info.BasePackageOrDefault(),
		packageName(info.Packages, pkg.Name),
		*info,
		files,
	); err != nil {
		return nil, err
	}
	return files, nil
}

func isInputType(t schema.Type) bool {
	if optional, ok := t.(*schema.OptionalType); ok {
		t = optional.ElementType
	}

	_, stillOption := t.(*schema.OptionalType)
	contract.Assert(!stillOption)
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

func unOptional(t schema.Type) schema.Type {
	switch tt := t.(type) {
	case *schema.OptionalType:
		return tt.ElementType
	default:
		return t
	}
}
