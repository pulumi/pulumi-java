package jvm

import (
	"bytes"
	"fmt"
	"path"
	"reflect"
	"strconv"
	"strings"
	"unicode"

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

// title converts the input string to a title case
// where only the initial letter is upper-cased.
func title(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	return string(append([]rune{unicode.ToUpper(runes[0])}, runes[1:]...))
}

func javaIdentifier(s string) string {
	// Note: Some schema field names may look like $ref or $schema.
	// We DO NOT remove the leading $ since it is a valid identifier (but C# does this).

	return makeValidIdentifier(s)
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
}

func (mod *modContext) propertyName(p *schema.Property) string {
	if n, ok := mod.propertyNames[p]; ok {
		return n
	}
	return p.Name
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
	return title(components[2])
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
	// Called in the context of an overload without an `Input<T>` wrapper. We
	// should act like we are inside an Input<T>.
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
		return TypeShape{
			Type:       names.Input,
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
			var info JVMPackageInfo
			contract.AssertNoError(extPkg.ImportLanguages(map[string]schema.Language{"jvm": Importer}))
			if v, ok := t.Package.Language["jvm"].(JVMPackageInfo); ok {
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
				var info JVMPackageInfo
				contract.AssertNoError(extPkg.ImportLanguages(map[string]schema.Language{"jvm": Importer}))
				if v, ok := t.Resource.Package.Language["jvm"].(JVMPackageInfo); ok {
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
			return TypeShape{Type: names.Object, Annotations: elementTypeSet.SortedValues()}
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
			return TypeShape{Type: names.JsonElement}
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
		return fmt.Sprintf("%s.empty()", ctx.ref(names.Input))
	}
	if _, ok := t.(*schema.OptionalType); ok && !optionalAsNull {
		return fmt.Sprintf("%s.empty()", ctx.ref(names.Optional))
	}
	switch codegen.UnwrapType(t).(type) {
	case *schema.ArrayType:
		return fmt.Sprintf("%s.of()", ctx.ref(names.List))
	case *schema.MapType:
		return fmt.Sprintf("%s.of()", ctx.ref(names.Map))
	case *schema.UnionType:
		return "null" // TODO: should we return an "empty Either" (not sure what that means exactly)
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
			return fmt.Sprintf("%s.ofList(%s)", ctx.ref(names.Input), nested)
		case *schema.MapType:
			return fmt.Sprintf("%s.ofMap(%s)", ctx.ref(names.Input), nested)
		case *schema.UnionType:
			return handleUnion(t,
				fmt.Sprintf("%s.ofLeft", ctx.ref(names.Input)),
				fmt.Sprintf("%s.ofRight", ctx.ref(names.Input)))
		default:
			return fmt.Sprintf("%s.ofNullable(%s)", ctx.ref(names.Input), nested)
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

// TODO: documentation comments

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

func (pt *plainType) genInputProperty(ctx *classFileContext, prop *schema.Property) error {
	w := ctx.writer
	requireInitializers := !pt.args || isInputType(prop.Type)

	wireName := prop.Name
	propertyName := javaIdentifier(pt.mod.propertyName(prop))
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
		json := true
		if prop.Type == schema.StringType {
			json = false
		} else if t, ok := prop.Type.(*schema.TokenType); ok && t.UnderlyingType == schema.StringType {
			json = false
		}
		if json {
			attributeArgs += ", json=true"
		}
	}

	indent := strings.Repeat("    ", 1)

	// TODO: add docs comment
	_, _ = fmt.Fprintf(w, "%s@%s(name=\"%s\"%s)\n", indent, ctx.ref(names.InputImport), wireName, attributeArgs)
	_, _ = fmt.Fprintf(w, "%sprivate final %s %s;\n", indent, propertyType.ToCode(ctx.imports), propertyName)
	_, _ = fmt.Fprintf(w, "\n")

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

	getterName := javaIdentifier("get" + title(prop.Name))
	// TODO: add docs comment
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
		Indent:          strings.Repeat(" ", 4),
		GetterType:      getterType.ToCode(ctx.imports),
		GetterName:      getterName,
		ReturnStatement: returnStatement,
	}); err != nil {
		return err
	}

	return nil
}

func (pt *plainType) genInputType(ctx *classFileContext) error {
	w := ctx.writer
	_, _ = fmt.Fprintf(w, "\n")

	// Open the class.
	// TODO: add docs comment
	_, _ = fmt.Fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	_, _ = fmt.Fprintf(w, "\n")
	_, _ = fmt.Fprintf(w, "    public static final %s Empty = new %s();\n", pt.name, pt.name)
	_, _ = fmt.Fprintf(w, "\n")

	// Declare each input property.
	for _, p := range pt.properties {
		if err := pt.genInputProperty(ctx, p); err != nil {
			return err
		}
		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "\n")
	}

	// Generate the constructor.
	_, _ = fmt.Fprintf(w, "    public %s(", pt.name)

	// Generate the constructor parameters.
	for i, prop := range pt.properties {
		// TODO: factor this out (with similar code in genOutputType)
		paramName := javaIdentifier(prop.Name)
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

		if i == 0 && len(pt.properties) > 1 { // first param
			_, _ = fmt.Fprint(w, "\n")
		}

		terminator := ""
		if i != len(pt.properties)-1 { // not last param
			terminator = ",\n"
		}

		paramDef := fmt.Sprintf("%s %s%s", paramType, paramName, terminator)
		if len(pt.properties) > 1 {
			paramDef = fmt.Sprintf("        %s", paramDef)
		}
		_, _ = fmt.Fprint(w, paramDef)
	}

	_, _ = fmt.Fprintf(w, ") {\n")

	// Generate the constructor body
	for _, prop := range pt.properties {
		paramName := javaIdentifier(prop.Name)
		fieldName := javaIdentifier(pt.mod.propertyName(prop))
		// set default values or assign given values
		var defaultValueCode string
		if prop.DefaultValue != nil {
			defaultValueString, defType, err := pt.mod.getDefaultValue(prop.DefaultValue, codegen.UnwrapType(prop.Type))
			if err != nil {
				return err
			}
			defaultValueInitializer := typeInitializer(ctx, prop.Type, defaultValueString, defType)
			defaultValueCode = fmt.Sprintf("%s == null ? %s : ", paramName, defaultValueInitializer)
		}
		if prop.IsRequired() {
			_, _ = fmt.Fprintf(w, "        this.%[1]s = %[2]s%[4]s.requireNonNull(%[3]s, \"expected parameter '%[3]s' to be non-null\");\n",
				fieldName, defaultValueCode, paramName, ctx.ref(names.Objects))
		} else {
			_, _ = fmt.Fprintf(w, "        this.%s = %s%s;\n", fieldName, defaultValueCode, paramName)
		}
	}
	_, _ = fmt.Fprintf(w, "    }\n")

	// Generate empty constructor, not that the instance created
	// with this constructor may not be valid if there are 'required' fields.
	if len(pt.properties) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "    private %s() {\n", pt.name)
		for _, prop := range pt.properties {
			fieldName := javaIdentifier(pt.mod.propertyName(prop))
			emptyValue := emptyTypeInitializer(ctx, prop.Type, true)
			_, _ = fmt.Fprintf(w, "        this.%s = %s;\n", fieldName, emptyValue)
		}
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// Generate the builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range pt.properties {
		requireInitializers := !pt.args || isInputType(prop.Type)
		propertyName := javaIdentifier(pt.mod.propertyName(prop))
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
			FieldName: propertyName,
		})

		setterName := javaIdentifier("set" + title(prop.Name))
		assignment := func(propertyName string) string {
			if prop.Secret {
				return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Input), propertyName)
			} else {
				if prop.IsRequired() {
					return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
				} else {
					return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
				}
			}
		}

		// add main setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName,
			Assignment:   assignment(propertyName),
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

			assignmentUnwrapped := func(propertyName string) string {
				if prop.Secret {
					return fmt.Sprintf("this.%s = %s.ofNullable(%s).asSecret()", propertyName, ctx.ref(names.Input), propertyName)
				} else {
					if prop.IsRequired() {
						return fmt.Sprintf("this.%s = %s.of(%s.requireNonNull(%s))",
							propertyName, ctx.ref(names.Input), ctx.ref(names.Objects), propertyName)
					} else {
						return fmt.Sprintf("this.%s = %s.ofNullable(%s)", propertyName, ctx.ref(names.Input), propertyName)
					}
				}
			}

			// add overloaded setter
			builderSetters = append(builderSetters, builderSetterTemplateContext{
				SetterName:   setterName,
				PropertyType: propertyTypeUnwrapped.ToCode(ctx.imports),
				PropertyName: propertyName,
				Assignment:   assignmentUnwrapped(propertyName),
			})
		}
	}

	_, _ = fmt.Fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     strings.Repeat("    ", 1),
		Name:       "Builder",
		IsFinal:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
	}); err != nil {
		return err
	}
	_, _ = fmt.Fprintf(w, "\n")

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (pt *plainType) genOutputType(ctx *classFileContext) error {
	w := ctx.writer
	indent := strings.Repeat("    ", 0)

	// Open the class and annotate it appropriately.
	_, _ = fmt.Fprintf(w, "%s@%s\n", indent, ctx.ref(names.OutputCustomType))
	_, _ = fmt.Fprintf(w, "%spublic final class %s {\n", indent, pt.name)

	// Generate each output field.
	for _, prop := range pt.properties {
		fieldName := javaIdentifier(pt.mod.propertyName(prop))
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
		// TODO: add docs comment
		_, _ = fmt.Fprintf(w, "%s    private final %s %s;\n", indent, fieldType, fieldName)
	}
	if len(pt.properties) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
	}

	// Generate the constructor parameter names - used as a workaround for Java reflection issues
	var paramNamesStringBuilder strings.Builder
	paramNamesStringBuilder.WriteString("{")
	for i, prop := range pt.properties {
		if i > 0 {
			paramNamesStringBuilder.WriteString(",")
		}
		paramName := javaIdentifier(prop.Name)
		paramNamesStringBuilder.WriteString("\"" + paramName + "\"")
	}
	paramNamesStringBuilder.WriteString("}")

	// Generate an appropriately-attributed constructor that will set this types' fields.
	_, _ = fmt.Fprintf(w, "%s    @%s.Constructor(%s)\n", indent, ctx.ref(names.OutputCustomType), paramNamesStringBuilder.String())
	_, _ = fmt.Fprintf(w, "%s    private %s(", indent, pt.name)

	// Generate the constructor parameters.
	for i, prop := range pt.properties {
		// TODO: factor this out (with similar code in genInputType)
		paramName := javaIdentifier(prop.Name)
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

		if i == 0 && len(pt.properties) > 1 { // first param
			_, _ = fmt.Fprint(w, "\n")
		}

		terminator := ""
		if i != len(pt.properties)-1 { // not last param
			terminator = ",\n"
		}

		paramDef := fmt.Sprintf("%s %s%s", paramType, paramName, terminator)
		if len(pt.properties) > 1 {
			paramDef = fmt.Sprintf("%s        %s", indent, paramDef)
		}
		_, _ = fmt.Fprint(w, paramDef)
	}

	_, _ = fmt.Fprintf(w, ") {\n")

	// Generate the constructor body.
	for _, prop := range pt.properties {
		paramName := javaIdentifier(prop.Name)
		fieldName := javaIdentifier(pt.mod.propertyName(prop))
		if prop.IsRequired() {
			_, _ = fmt.Fprintf(w, "%s        this.%s = %s.requireNonNull(%s);\n", indent, ctx.ref(names.Objects), fieldName, paramName)
		} else {
			_, _ = fmt.Fprintf(w, "%s        this.%s = %s;\n", indent, fieldName, paramName)
		}
	}
	_, _ = fmt.Fprintf(w, "%s    }\n", indent)
	_, _ = fmt.Fprintf(w, "\n")

	// Generate getters
	for _, prop := range pt.properties {
		paramName := javaIdentifier(prop.Name)
		getterName := javaIdentifier("get" + title(prop.Name))
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

		// TODO: add docs comment
		var returnStatement string
		if prop.IsRequired() {
			returnStatement = fmt.Sprintf("this.%s", paramName)
		} else {
			switch prop.Type.(type) {
			case *schema.ArrayType:
				getterType = getterTypeNonOptional
				returnStatement = fmt.Sprintf("this.%s == null ? List.of() : this.%s", paramName, paramName)
			case *schema.MapType:
				getterType = getterTypeNonOptional
				returnStatement = fmt.Sprintf("this.%s == null ? Map.of() : this.%s", paramName, paramName)
			default:
				returnStatement = fmt.Sprintf("%s.ofNullable(this.%s)", ctx.ref(names.Optional), paramName)
			}
		}

		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          strings.Repeat("    ", 1),
			GetterType:      getterType.ToCode(ctx.imports),
			GetterName:      getterName,
			ReturnStatement: returnStatement,
		}); err != nil {
			return err
		}

		_, _ = fmt.Fprintf(w, "\n")
	}

	// Generate Builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range pt.properties {
		propertyName := javaIdentifier(pt.mod.propertyName(prop))
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
			FieldName: propertyName,
		})

		setterName := javaIdentifier("set" + title(prop.Name))
		assignment := func(propertyName string) string {
			if prop.IsRequired() {
				return fmt.Sprintf("this.%s = %s.requireNonNull(%s)", propertyName, ctx.ref(names.Objects), propertyName)
			} else {
				return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
			}
		}

		// add setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.ToCode(ctx.imports),
			PropertyName: propertyName,
			Assignment:   assignment(propertyName),
		})
	}

	_, _ = fmt.Fprintf(w, "\n")
	if err := builderTemplate.Execute(w, builderTemplateContext{
		Indent:     strings.Repeat("    ", 1),
		Name:       "Builder",
		IsFinal:    true,
		Fields:     builderFields,
		Setters:    builderSetters,
		ResultType: pt.name,
		Objects:    ctx.ref(names.Objects),
	}); err != nil {
		return err
	}
	_, _ = fmt.Fprintf(w, "\n")

	// Close the class.
	_, _ = fmt.Fprintf(w, "%s}\n", indent)
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
		return strconv.FormatFloat(v.Float(), 'f', -1, 64), "number", nil
	case reflect.String:
		return fmt.Sprintf("%q", v.String()), "string", nil
	default:
		return "", "", errors.Errorf("unsupported default value of type %T", value)
	}
}

func (mod *modContext) getDefaultValue(dv *schema.DefaultValue, t schema.Type) (string, string, error) {
	var val string
	schemaType := t.String()
	if dv.Value != nil {
		switch enumOrUnion := t.(type) {
		case *schema.EnumType:
			enumName := tokenToName(enumOrUnion.Token)
			for _, e := range enumOrUnion.Elements {
				if e.Value != dv.Value {
					continue
				}

				elName := e.Name
				if elName == "" {
					elName = fmt.Sprintf("%v", e.Value)
				}
				safeName, err := makeSafeEnumName(elName, enumName)
				if err != nil {
					return "", "", err
				}
				val = fmt.Sprintf("%s.enums.%s.%s", mod.packageName, enumName, safeName)
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

		getEnv := fmt.Sprintf("Utilities.getEnv%s(%s).orElse(null)", getType, envVars)
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
	_, _ = fmt.Fprintf(w, "%s.of(", ctx.ref(names.Input))
	_, _ = fmt.Fprintf(w, "%s.builder()", ctx.ref(names.Alias))
	if alias.Name != nil {
		_, _ = fmt.Fprintf(w, ".setName(\"%v\")", *alias.Name)
	}
	if alias.Project != nil {
		_, _ = fmt.Fprintf(w, ".setProject(\"%v\")", *alias.Project)
	}
	if alias.Type != nil {
		_, _ = fmt.Fprintf(w, ".setType(\"%v\")", *alias.Type)
	}
	_, _ = fmt.Fprintf(w, ".build()")
	_, _ = fmt.Fprintf(w, ")")
}

func (mod *modContext) genResource(ctx *classFileContext, r *schema.Resource, argsFQN, stateFQN names.FQN) error {
	w := ctx.writer
	// Create a resource module file into which all of this resource's types will go.
	name := resourceName(r)

	// TODO: add docs comment

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
	_, _ = fmt.Fprintf(w, "@%s(type=\"%s\")\n",
		ctx.imports.Ref(names.ResourceType),
		r.Token)
	_, _ = fmt.Fprintf(w, "public class %s extends %s {\n", className, baseType)

	var secretProps []string
	// Emit all output properties.
	for _, prop := range r.Properties {
		// Write the property attribute
		wireName := prop.Name
		propertyName := mod.propertyName(prop)
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

		// TODO: add docs comment
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
		_, _ = fmt.Fprintf(w, "    @%s(name=\"%s\", type=%s, parameters={%s})\n", ctx.ref(names.OutputExport), wireName, outputExportType, outputExportParameters)
		_, _ = fmt.Fprintf(w, "    private %s<%s> %s;\n", ctx.imports.Ref(names.Output), outputParameterType, propertyName)
		_, _ = fmt.Fprintf(w, "\n")

		// Add getter
		getterType := outputParameterType
		getterName := javaIdentifier("get" + title(prop.Name))
		_, _ = fmt.Fprintf(w, "    public %s<%s> %s() {\n", ctx.imports.Ref(names.Output), getterType, getterName)
		_, _ = fmt.Fprintf(w, "        return this.%s;\n", propertyName)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	if len(r.Properties) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
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

	// TODO: add docs comment

	_, _ = fmt.Fprintf(w, "    public %s(String name, %s args, @%s %s options) {\n", className, argsType, ctx.ref(names.Nullable), optionsType)
	if r.IsComponent {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, %s.empty()), true);\n", tok, argsOverride, ctx.imports.Ref(names.Input))
	} else {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, %s.empty()));\n", tok, argsOverride, ctx.imports.Ref(names.Input))
	}
	_, _ = fmt.Fprintf(w, "    }\n")

	// Write a private constructor for the use of `get`.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", "null"
		if r.StateInputs != nil {
			stateParam, stateRef = fmt.Sprintf("@%s %s state, ", ctx.ref(names.Nullable), ctx.ref(stateFQN)), "state"
		}

		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "    private %s(String name, %s<String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Input), stateParam, ctx.ref(names.Nullable), optionsType)
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, id));\n", tok, stateRef)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	if hasConstInputs {
		// Write the method that will calculate the resource arguments.
		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "    private static %s makeArgs(%s args) {\n", ctx.ref(argsFQN), argsType)
		_, _ = fmt.Fprintf(w, "        var builder = args == null ? %[1]s.builder() : %[1]s.builder(args);\n", ctx.ref(argsFQN))
		_, _ = fmt.Fprintf(w, "        return builder\n")
		for _, prop := range r.InputProperties {
			if prop.ConstValue != nil {
				v, _, err := primitiveValue(prop.ConstValue)
				if err != nil {
					return err
				}
				setterName := javaIdentifier("set" + title(mod.propertyName(prop)))
				_, _ = fmt.Fprintf(w, "            .%s(%s)\n", setterName, v)
			}
		}
		_, _ = fmt.Fprintf(w, "            .build();\n")
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// Write the method that will calculate the resource options.
	_, _ = fmt.Fprintf(w, "\n")
	_, _ = fmt.Fprintf(w, "    private static %[1]s makeResourceOptions(@%[2]s %[1]s options, @%[2]s %[3]s<String> id) {\n",
		optionsType, ctx.ref(names.Nullable), ctx.ref(names.Input))
	_, _ = fmt.Fprintf(w, "        var defaultOptions = %s.builder()\n", optionsType)
	_, _ = fmt.Fprintf(w, "            .setVersion(Utilities.getVersion())\n")

	if len(r.Aliases) > 0 {
		_, _ = fmt.Fprintf(w, "            .setAliases(%s.of(\n", ctx.ref(names.List))
		for i, alias := range r.Aliases {
			_, _ = fmt.Fprintf(w, "                ")
			genAlias(ctx, alias)
			isLastElement := i == len(r.Aliases)-1
			if isLastElement {
				_, _ = fmt.Fprintf(w, "\n")
			} else {
				_, _ = fmt.Fprintf(w, ",\n")
			}
		}
		_, _ = fmt.Fprintf(w, "            ))\n")
	}
	if len(secretProps) > 0 {
		_, _ = fmt.Fprintf(w, "            .setAdditionalSecretOutputs(%s.of(\n", ctx.ref(names.List))
		for i, sp := range secretProps {
			_, _ = fmt.Fprintf(w, "                ")
			_, _ = fmt.Fprintf(w, "%q", sp)
			isLastElement := i == len(secretProps)-1
			if isLastElement {
				_, _ = fmt.Fprintf(w, "\n")
			} else {
				_, _ = fmt.Fprintf(w, ",\n")
			}
		}
		_, _ = fmt.Fprintf(w, "            ))\n")
	}

	_, _ = fmt.Fprintf(w, "            .build();\n")
	_, _ = fmt.Fprintf(w, "        return %s.merge(defaultOptions, options, id);\n", optionsType)
	_, _ = fmt.Fprintf(w, "    }\n\n")

	// Write the `get` method for reading instances of this resource unless this is a provider resource or ComponentResource.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", ""

		// TODO: add docs comments
		if r.StateInputs != nil {
			stateParam = fmt.Sprintf("@%s %sState state, ", ctx.ref(names.Nullable), className)
			stateRef = "state, "
			// TODO: add docs param
		}

		_, _ = fmt.Fprintf(w, "    public static %s get(String name, %s<String> id, %s@%s %s options) {\n",
			className, ctx.ref(names.Input), stateParam, ctx.ref(names.Nullable), optionsType)
		_, _ = fmt.Fprintf(w, "        return new %s(name, id, %soptions);\n", className, stateRef)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (mod *modContext) genFunction(ctx *classFileContext, fun *schema.Function) error {
	w := ctx.writer
	className := tokenToFunctionName(fun.Token)

	var typeParameter string
	if fun.Outputs != nil {
		typeParameter = fmt.Sprintf("%sResult", className)
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

		argsParamDef = fmt.Sprintf("%s%sArgs args, ", nullable, className)
		argsParamRef = fmt.Sprintf("args == null ? %sArgs.Empty : args", className)
	}

	printObsoleteAttribute(ctx, fun.DeprecationMessage, "")
	// Open the class we'll use for datasources.
	_, _ = fmt.Fprintf(w, "public class %s {\n", className)

	// TODO: add docs comment

	// Emit the datasource method.
	_, _ = fmt.Fprintf(w, "    public static %s<%s> invokeAsync(%s@%s %s options) {\n",
		ctx.ref(names.CompletableFuture), typeParameter, argsParamDef, ctx.ref(names.Nullable), ctx.ref(names.InvokeOptions))
	_, _ = fmt.Fprintf(w, "        return %s.getInstance().invokeAsync(\"%s\", %s.of(%s.class), %s, Utilities.withVersion(options));\n",
		ctx.ref(names.Deployment), fun.Token, ctx.ref(names.TypeShape), typeParameter, argsParamRef)
	_, _ = fmt.Fprintf(w, "    }\n")

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func printObsoleteAttribute(ctx *classFileContext, deprecationMessage, indent string) {
	w := ctx.writer
	if deprecationMessage != "" {
		_, _ = fmt.Fprintf(w, "%s@Deprecated /* %s */\n",
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

		safeName, err := makeSafeEnumName(e.Name, enumName)
		if err != nil {
			return err
		}
		e.Name = safeName
	}

	// TODO: add docs comment

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
		_, _ = fmt.Fprintf(w, "%s@%s\n", indent, ctx.ref(names.EnumType))
		_, _ = fmt.Fprintf(w, "%spublic enum %s {\n", indent, enumName)
		indent := strings.Repeat(indent, 2)

		// Enum values
		for i, e := range enum.Elements {
			// TODO: add docs comment
			printObsoleteAttribute(ctx, e.DeprecationMessage, indent)
			var separator string
			if i == len(enum.Elements)-1 { // last element
				separator = ";"
			} else {
				separator = ","
			}
			if enum.ElementType == schema.StringType {
				_, _ = fmt.Fprintf(w, "%s%s(%q)%s\n", indent, e.Name, e.Value, separator)
			} else if enum.ElementType == schema.NumberType {
				_, _ = fmt.Fprintf(w, "%s%s(%f)%s\n", indent, e.Name, e.Value, separator)
			} else {
				_, _ = fmt.Fprintf(w, "%s%s(%v)%s\n", indent, e.Name, e.Value, separator)
			}
		}
		_, _ = fmt.Fprintf(w, "\n")

		_, _ = fmt.Fprintf(w, "%sprivate final %s value;\n", indent, underlyingType)
		_, _ = fmt.Fprintf(w, "\n")

		// Constructor
		_, _ = fmt.Fprintf(w, "%s%s(%s value) {\n", indent, enumName, underlyingType)
		if enum.ElementType == schema.StringType {
			_, _ = fmt.Fprintf(w, "%s    this.value = %s.requireNonNull(value);\n", indent, ctx.ref(names.Objects))
		} else {
			_, _ = fmt.Fprintf(w, "%s    this.value = value;\n", indent)
		}
		_, _ = fmt.Fprintf(w, "%s}\n", indent)
		_, _ = fmt.Fprintf(w, "\n")

		// Explicit conversion operator
		_, _ = fmt.Fprintf(w, "%[1]s@%s.Converter\n", indent, ctx.ref(names.EnumType))
		_, _ = fmt.Fprintf(w, "%[1]spublic %s getValue() {\n", indent, underlyingType)
		_, _ = fmt.Fprintf(w, "%s    return this.value;\n", indent)
		_, _ = fmt.Fprintf(w, "%s}\n", indent)
		_, _ = fmt.Fprintf(w, "\n")

		// toString override
		_, _ = fmt.Fprintf(w, "%s@Override\n", indent)
		_, _ = fmt.Fprintf(w, "%spublic String toString() {\n", indent)
		_, _ = fmt.Fprintf(w, "%s    return new %s(\", \", \"%s[\", \"]\")\n", indent, ctx.ref(names.StringJoiner), enumName)
		_, _ = fmt.Fprintf(w, "%s        .add(\"value='\" + this.value + \"'\")\n", indent)
		_, _ = fmt.Fprintf(w, "%s        .toString();\n", indent)
		_, _ = fmt.Fprintf(w, "%s}\n", indent)
	default:
		// TODO: Issue to implement boolean-based enums [in C#]: https://github.com/pulumi/pulumi/issues/5652
		return fmt.Errorf("enums of type %s are not yet implemented for this language", enum.ElementType.String())
	}

	// Close the enum declaration
	_, _ = fmt.Fprintf(w, "%s}\n", indent)

	return nil
}

func visitObjectTypes(properties []*schema.Property, visitor func(*schema.ObjectType)) {
	codegen.VisitTypeClosure(properties, func(t schema.Type) {
		if o, ok := t.(*schema.ObjectType); ok {
			visitor(o)
		}
	})
}

func (mod *modContext) genType(ctx *classFileContext, obj *schema.ObjectType, propertyTypeQualifier string, input, state bool) error {
	pt := &plainType{
		mod:                   mod,
		name:                  mod.typeName(obj, state, obj.IsInputShape()),
		comment:               obj.Comment,
		propertyTypeQualifier: propertyTypeQualifier,
		properties:            obj.Properties,
		state:                 state,
		args:                  obj.IsInputShape(),
	}

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
	_, _ = fmt.Fprintf(w, "// *** WARNING: this file was generated by %v. ***\n", mod.tool)
	_, _ = fmt.Fprintf(w, "// *** Do not edit by hand unless you're certain you know what you are doing! ***\n")
	return buf.String()
}

func (mod *modContext) getConfigProperty(ctx *classFileContext, schemaType schema.Type, key string) (TypeShape, MethodCall) {
	propertyType := mod.typeString(
		ctx,
		schemaType,
		"types",
		false,
		false,
		false,
		false, // outer optional
		false, // inputless overload
	)

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

		getFunc.Method = "getObject"
		getFunc.Args = append(getFunc.Args, propertyType.StringJavaTypeShape(ctx.imports))
	}

	return propertyType, getFunc
}

func (mod *modContext) genConfig(ctx *classFileContext, variables []*schema.Property) error {
	w := ctx.writer

	// Open the config class.
	_, _ = fmt.Fprintf(w, "public final class Config {\n")
	_, _ = fmt.Fprintf(w, "\n")
	// Create a config bag for the variables to pull from.
	_, _ = fmt.Fprintf(w, "    private static final io.pulumi.Config config = io.pulumi.Config.of(%q);", mod.pkg.Name)
	_, _ = fmt.Fprintf(w, "\n")

	// Emit an entry for all config variables.
	for _, p := range variables {
		propertyType, getFunc := mod.getConfigProperty(ctx, p.Type, p.Name)
		propertyName := javaIdentifier(mod.propertyName(p))

		returnStatement := getFunc.String()

		if p.DefaultValue != nil {
			defaultValueString, defType, err := mod.getDefaultValue(p.DefaultValue, p.Type)
			if err != nil {
				return err
			}
			defaultValueInitializer := typeInitializer(ctx, p.Type, defaultValueString, defType)
			returnStatement += ".orElse(" + defaultValueInitializer + ")"
		}

		// TODO: printComment(w, p.Comment, "        ")
		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          strings.Repeat("    ", 1),
			GetterType:      propertyType.ToCode(ctx.imports),
			GetterName:      propertyName,
			ReturnStatement: returnStatement,
		}); err != nil {
			return err
		}
		_, _ = fmt.Fprintf(w, "\n")
	}

	// TODO: finish the config generation, emit any nested types.

	// Close the config class and namespace.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

type fs map[string][]byte

func (fs fs) add(path string, contents []byte) {
	_, has := fs[path]
	contract.Assertf(!has, "duplicate file: %s", path)
	fs[path] = contents
}

func (mod *modContext) genUtilities() (string, error) {
	// Strip any 'v' off of the version.
	w := &bytes.Buffer{}
	err := jvmUtilitiesTemplate.Execute(w, jvmUtilitiesTemplateContext{
		Name:        packageName(mod.packages, mod.pkg.Name),
		PackageName: mod.packageName,
		PackagePath: strings.ReplaceAll(mod.packageName, ".", "/"),
		ClassName:   "Utilities",
		Tool:        mod.tool,
	})
	if err != nil {
		return "", err
	}

	return w.String(), nil
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

	addFile := func(name, contents string) {
		p := path.Join(dir, name)
		files = append(files, p)

		fs.add(p, []byte(contents))
	}

	addClassFile := func(pkg names.FQN, className names.Ident, contents string) {
		fqn := pkg.Dot(className)
		relPath := path.Join(strings.Split(fqn.ToString(), ".")...)
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
		utilities, err := mod.genUtilities()
		if err != nil {
			return err
		}
		addFile("Utilities.java", utilities)

		// Ensure that the target module directory contains a README.md file.
		readme := mod.pkg.Description
		if readme != "" && readme[len(readme)-1] != '\n' {
			readme += "\n"
		}
		fs.add("README.md", []byte(readme))
	case "config":
		if len(mod.pkg.Config) > 0 {
			if err := addClass(javaPkg, names.Ident("Config"), func(ctx *classFileContext) error {
				return mod.genConfig(ctx, mod.pkg.Config)
			}); err != nil {
				return err
			}
			return nil
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
		argsFQN := inputsPkg.Dot(argsClassName)

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
		if err := addClass(inputsPkg, argsClassName, func(ctx *classFileContext) error {
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
		if f.IsOverlay {
			// This function code is generated by the provider, so no further action is required.
			continue
		}

		if err := addClass(javaPkg, names.Ident(tokenToName(f.Token)), func(ctx *classFileContext) error {
			return mod.genFunction(ctx, f)
		}); err != nil {
			return err
		}

		// Emit the args and result types, if any.
		if f.Inputs != nil {
			inputsPkg := javaPkg.Dot(names.Ident("inputs"))
			argsClass := names.Ident(tokenToName(f.Token) + "Args")
			if err := addClass(inputsPkg, argsClass, func(ctx *classFileContext) error {
				args := &plainType{
					mod:                   mod,
					name:                  string(ctx.className),
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
			outputsPkg := javaPkg.Dot(names.Ident("outputs"))
			resultClass := names.Ident(tokenToName(f.Token) + "Result")
			if err := addClass(outputsPkg, resultClass, func(ctx *classFileContext) error {
				res := &plainType{
					mod:                   mod,
					name:                  string(ctx.className),
					propertyTypeQualifier: "outputs",
					properties:            f.Outputs.Properties,
				}
				return res.genOutputType(ctx)
			}); err != nil {
				return err
			}
		}
	}

	// Input/Output types
	for _, t := range mod.types {
		if t.IsOverlay {
			// This type is generated by the provider, so no further action is required.
			continue
		}
		if mod.details(t).plainType {
			t := t.PlainShape
			className := names.Ident(mod.typeName(t, false, t.IsInputShape()))
			if err := addClass(javaPkg.Dot(names.Ident("inputs")), className, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "inputs", true, false)
			}); err != nil {
				return err
			}
		}
		if mod.details(t).inputType {
			className := names.Ident(mod.typeName(t, false, t.IsInputShape()))
			if err := addClass(javaPkg.Dot(names.Ident("inputs")), className, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "inputs", true, false)
			}); err != nil {
				return err
			}
		}
		if mod.details(t).stateType {
			className := names.Ident(mod.typeName(t, true, true))
			if err := addClass(javaPkg.Dot(names.Ident("inputs")), className, func(ctx *classFileContext) error {
				return mod.genType(ctx, t, "inputs", true, true)
			}); err != nil {
				return err
			}
		}
		if mod.details(t).outputType {
			className := names.Ident(mod.typeName(t, false, false))
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
		if info, ok := p.Language["jvm"].(JVMPropertyInfo); ok && info.Name != "" {
			names[p] = info.Name
		}
	}
}

func generateModuleContextMap(tool string, pkg *schema.Package) (map[string]*modContext, *JVMPackageInfo, error) {
	// Decode Java-specific info for each package as we discover them.
	infos := map[*schema.Package]*JVMPackageInfo{}
	var getPackageInfo = func(p *schema.Package) *JVMPackageInfo {
		info, ok := infos[p]
		if !ok {
			if err := p.ImportLanguages(map[string]schema.Language{"jvm": Importer}); err != nil {
				panic(err)
			}
			jvmInfo, _ := pkg.Language["jvm"].(JVMPackageInfo)
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
		cfg.packageName = cfg.basePackageName + packageName(infos[pkg].Packages, pkg.Name)
	}

	visitObjectTypes(pkg.Config, func(t *schema.ObjectType) {
		getModFromToken(t.Token, pkg).details(t).outputType = true
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
func genGradleProject(pkg *schema.Package, basePackageName string, packageName string, packageReferences map[string]string, files fs) error {
	genSettingsFile, err := genSettingsFile(basePackageName + packageName)
	if err != nil {
		return err
	}
	files.add("settings.gradle", genSettingsFile)
	genBuildFile, err := genBuildFile(pkg.Name, basePackageName)
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
func genBuildFile(name string, basePackageName string) ([]byte, error) {
	w := &bytes.Buffer{}
	err := jvmBuildTemplate.Execute(w, jvmBuildTemplateContext{
		Name:            name,
		BasePackageName: strings.TrimSuffix(basePackageName, "."),
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
		info.PackageReferences,
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
