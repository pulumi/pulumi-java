package jvm

import (
	"bytes"
	"fmt"
	"io"
	"path"
	"path/filepath"
	"reflect"
	"sort"
	"strconv"
	"strings"
	"unicode"

	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
)

type stringSet map[string]struct{}

func (ss stringSet) add(s string) {
	ss[s] = struct{}{}
}

func (ss stringSet) has(s string) bool {
	_, ok := ss[s]
	return ok
}

type typeDetails struct {
	outputType bool
	inputType  bool
	stateType  bool
	argsType   bool
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
	compatibility          string
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

// TODO: can we get rid of this?
func (mod *modContext) isK8sCompatMode() bool {
	return mod.compatibility == "kubernetes20"
}

// TODO: can we get rid of this?
func (mod *modContext) isTFCompatMode() bool {
	return mod.compatibility == "tfbridge20"
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

func (mod *modContext) typeName(t *schema.ObjectType, state, input, args bool) string {
	name := tokenToName(t.Token)
	if state {
		return name + "GetArgs"
	}
	if !mod.isTFCompatMode() { // TODO: do we need this?
		if args {
			return name + "Args"
		}
		return name
	}

	switch {
	case input:
		return name + "Args"
	case mod.details(t).plainType:
		return name + "Result"
	}
	return name
}

func (mod *modContext) typeString(
	t schema.Type,
	qualifier string,
	input bool,
	state bool,
	wrapInput bool, // wrap the type in Input
	args bool,
	requireInitializers bool,
	optional bool,
	nullable bool,
) TypeShape {
	return mod.typeStringInner(t, qualifier, input, state, wrapInput, args, requireInitializers, optional, nullable)
}

func (mod *modContext) typeStringInner(
	t schema.Type,
	qualifier string,
	input bool,
	state bool,
	wrapInput bool,
	args bool,
	requireInitializers bool,
	optional bool,
	nullable bool,
) TypeShape {
	var typ TypeShape
	switch t := t.(type) {
	case *schema.EnumType:
		typ.Type = mod.tokenToPackage(t.Token, "")
		typ.Type += "."
		typ.Type += "enums" // TODO: try to replace with 'qualifier'
		typ.Type += "."
		typ.Type += tokenToName(t.Token)
	case *schema.ArrayType:
		var listType string
		switch {
		case requireInitializers:
			listType = "List"
		default: // TODO: decide weather or not to use ImmutableList
			listType, optional = "List", false
		}

		typ.Type = listType
		typ.Parameters = append(
			typ.Parameters,
			mod.typeStringInner(t.ElementType, qualifier, input, state, false, args, false, false, false),
		)
	case *schema.MapType:
		var mapType string
		switch {
		case requireInitializers:
			mapType = "Map"
			typ.Parameters = append(
				typ.Parameters, TypeShape{Type: "String"},
			)
		default:
			mapType = "Map" // TODO: decide weather or not to use ImmutableMap
			typ.Parameters = append(
				typ.Parameters, TypeShape{Type: "String"},
			)
		}

		typ.Type = mapType
		typ.Parameters = append(
			typ.Parameters,
			mod.typeStringInner(t.ElementType, qualifier, input, state, false, args, false, false, false),
		)
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
				compatibility:   info.Compatibility,
				basePackageName: info.BasePackageOrDefault(),
			}
		}
		objectType := namingCtx.tokenToPackage(t.Token, qualifier)
		if objectType == namingCtx.packageName && qualifier == "" {
			objectType = qualifier
		}
		if objectType != "" {
			objectType += "."
		}
		objectType += mod.typeName(t, state, input, args)
		typ.Type = objectType
	case *schema.ResourceType:
		var resourceType string
		if strings.HasPrefix(t.Token, "pulumi:providers:") {
			pkgName := strings.TrimPrefix(t.Token, "pulumi:providers:")
			resourceType = fmt.Sprintf("%s%s.Provider", mod.basePackageName, packageName(mod.packages, pkgName))
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
					compatibility:   info.Compatibility,
					basePackageName: info.BasePackageOrDefault(),
				}
			}
			resourceType = namingCtx.tokenToPackage(t.Token, "")
			if resourceType != "" {
				resourceType += "."
			}
			resourceType += tokenToName(t.Token)
		}
		typ.Type = resourceType
	case *schema.TokenType:
		// Use the underlying type for now.
		if t.UnderlyingType != nil {
			return mod.typeStringInner(t.UnderlyingType, qualifier, input, state, wrapInput, args, requireInitializers, optional, nullable)
		}

		tokenType := tokenToName(t.Token)
		if ns := mod.tokenToPackage(t.Token, qualifier); ns != mod.packageName {
			tokenType = ns + "." + tokenType
		}
		typ.Type = tokenType
	case *schema.UnionType:
		elementTypeSet := stringSet{}
		var elementTypes []TypeShape
		for _, e := range t.ElementTypes {
			// If this is an output and a "relaxed" enum, emit the type as the underlying primitive type rather than the union.
			// Eg. Output<String> rather than Output<Either<EnumType, String>>
			if typ, ok := e.(*schema.EnumType); ok && !input {
				return mod.typeStringInner(typ.ElementType, qualifier, input, state, wrapInput, args, requireInitializers, optional, nullable)
			}

			et := mod.typeStringInner(e, qualifier, input, state, false, args, false, false, false)
			if !elementTypeSet.has(et.Type) {
				elementTypeSet.add(et.Type)
				elementTypes = append(elementTypes, et)
			}
		}

		switch len(elementTypes) {
		case 1:
			return mod.typeStringInner(t.ElementTypes[0], qualifier, input, state, wrapInput, args, requireInitializers, optional, nullable)
		case 2:
			unionType := "Either"
			typ.Type = unionType
			typ.Parameters = append(typ.Parameters, elementTypes...)
		default:
			typ.Type = "Object"
		}
	default:
		switch t {
		case schema.BoolType:
			typ.Type = "Boolean"
		case schema.IntType:
			typ.Type = "Integer"
		case schema.NumberType:
			typ.Type = "Double"
		case schema.StringType:
			typ.Type = "String"
		case schema.ArchiveType:
			typ.Type = "Archive"
		case schema.AssetType:
			typ.Type = "AssetOrArchive"
		case schema.JSONType:
			typ.Type = "com.google.gson.JsonElement"
		case schema.AnyType:
			typ.Type = "Object"
		}
	}

	if wrapInput {
		var wrapTyp = TypeShape{
			Type:       "Input",
			Parameters: []TypeShape{typ},
		}
		typ = wrapTyp
	}
	if optional {
		var optionalTyp = TypeShape{
			Type:       "Optional",
			Parameters: []TypeShape{typ},
		}
		typ = optionalTyp
	}
	if nullable {
		typ.Annotations = append(typ.Annotations, "@Nullable")
	}
	return typ
}

func emptyTypeInitializer(
	t schema.Type,
	wrapInput bool,
	wrapOptional bool,
) string {
	if wrapInput {
		return "Input.empty()"
	}
	if wrapOptional {
		return "Optional.empty()"
	}
	switch t.(type) {
	case *schema.ArrayType:
		return "List.of()"
	case *schema.MapType:
		return "Map.of()"
	case *schema.UnionType:
		return "null" // TODO: should we return an "empty Either" (not sure what that means exactly)
	default:
		return "null"
	}
}

func typeInitializer(
	t schema.Type,
	wrapInput bool,
	wrapOptional bool,
	nested string,
	nestedType string,
) string {
	switch t.(type) {
	case *schema.ArrayType:
		if wrapInput {
			return fmt.Sprintf("Input.ofList(%s)", nested)
		}
		if wrapOptional {
			return fmt.Sprintf("Optional.of(List.of(%s))", nested)
		}
		return fmt.Sprintf("List.of(%s)", nested)
	case *schema.MapType:
		if wrapInput {
			return fmt.Sprintf("Input.ofMap(%s)", nested)
		}
		if wrapOptional {
			return fmt.Sprintf("Optional.of(Map.of(%s))", nested)
		}
		return fmt.Sprintf("Map.of(%s)", nested)
	case *schema.UnionType:
		if t.(*schema.UnionType).ElementTypes[0].String() == nestedType {
			if wrapInput {
				return fmt.Sprintf("Input.ofLeft(%s)", nested)
			}
			if wrapOptional {
				return fmt.Sprintf("Optional.of(Either.leftOf(%s))", nested)
			}
			return fmt.Sprintf("Either.leftOf(%s)", nested)
		}
		if t.(*schema.UnionType).ElementTypes[1].String() == nestedType {
			if wrapInput {
				return fmt.Sprintf("Input.ofRight(%s)", nested)
			}
			if wrapOptional {
				return fmt.Sprintf("Optional.of(Either.rightOf(%s))", nested)
			}
			return fmt.Sprintf("Either.rightOf(%s)", nested)
		}
		panic(fmt.Sprintf("this should never happen: '%s' does not match either '%s' or '%s', for nested of: '%s'",
			nestedType, t.(*schema.UnionType).ElementTypes[0], t.(*schema.UnionType).ElementTypes[1], nested,
		))
	default:
		if wrapInput {
			return fmt.Sprintf("Input.ofNullable(%s)", nested)
		}
		if wrapOptional {
			return fmt.Sprintf("Optional.ofNullable(%s)", nested)
		}
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

func (pt *plainType) genInputProperty(w io.Writer, prop *schema.Property) error {
	isArgsType := pt.args && !prop.IsPlain
	requireInitializers := !pt.args || prop.IsPlain

	wireName := prop.Name
	propertyName := javaIdentifier(pt.mod.propertyName(prop))
	propertyType := pt.mod.typeString(
		prop.Type,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		isArgsType,          // wrap input
		isArgsType,          // has args
		requireInitializers, // requires initializers
		false,               // is optional
		!prop.IsRequired,    // is nullable
	)

	// First generate the input annotation.
	attributeArgs := ""
	if prop.IsRequired {
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
	_, _ = fmt.Fprintf(w, "%s@InputImport(name=\"%s\"%s)\n", indent, wireName, attributeArgs)
	_, _ = fmt.Fprintf(w, "%sprivate final %s %s;\n", indent, propertyType, propertyName)
	_, _ = fmt.Fprintf(w, "\n")

	// Add getter
	getterType := pt.mod.typeString(
		prop.Type,
		pt.propertyTypeQualifier,
		true,                            // is input
		pt.state,                        // is state
		isArgsType,                      // wrap input
		isArgsType,                      // has args
		requireInitializers,             // FIXME: should not require initializers, make it immutable
		!prop.IsRequired && !isArgsType, // is optional or an Input
		false,                           // is nullable
	)
	getterTypeNonOptional := pt.mod.typeString(
		prop.Type,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		isArgsType,          // wrap input
		isArgsType,          // has args
		requireInitializers, // FIXME: should not require initializers, make it immutable
		false,               // is a list or map
		false,               // is non-nullable
	)
	getterName := javaIdentifier("get" + title(prop.Name))
	// TODO: add docs comment
	printObsoleteAttribute(w, prop.DeprecationMessage, indent)
	required := prop.IsRequired
	returnStatement := fmt.Sprintf("this.%s", propertyName)
	if !required {
		emptyStatement := emptyTypeInitializer(prop.Type, isArgsType, false)
		switch prop.Type.(type) {
		case *schema.ArrayType, *schema.MapType, *schema.UnionType: // the most common case actually
			getterType = getterTypeNonOptional
		default:
			emptyStatement = emptyTypeInitializer(prop.Type, isArgsType, true)
			returnStatement = typeInitializer(prop.Type, false, !isArgsType, returnStatement, prop.Type.String())
		}
		returnStatement = fmt.Sprintf("this.%s == null ? %s : %s", propertyName, emptyStatement, returnStatement)
	}

	if err := getterTemplate.Execute(w, getterTemplateContext{
		Indent:          strings.Repeat("    ", 1),
		GetterType:      getterType.String(),
		GetterName:      getterName,
		ReturnStatement: returnStatement,
	}); err != nil {
		return err
	}

	return nil
}

func (pt *plainType) genInputType(w io.Writer) error {
	_, _ = fmt.Fprintf(w, "\n")

	// Open the class.
	// TODO: add docs comment
	_, _ = fmt.Fprintf(w, "public final class %s extends %s {\n", pt.name, pt.baseClass)
	_, _ = fmt.Fprintf(w, "\n")
	_, _ = fmt.Fprintf(w, "    public static final %s Empty = new %s();\n", pt.name, pt.name)
	_, _ = fmt.Fprintf(w, "\n")

	// Declare each input property.
	for _, p := range pt.properties {
		if err := pt.genInputProperty(w, p); err != nil {
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
		isArgsType := pt.args && !prop.IsPlain
		requireInitializers := !pt.args || prop.IsPlain
		paramType := pt.mod.typeString(
			prop.Type,
			pt.propertyTypeQualifier,
			true,
			pt.state,
			isArgsType,
			isArgsType,
			requireInitializers,
			false,
			!prop.IsRequired,
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
			paramDef = fmt.Sprintf("        %s", paramDef)
		}
		_, _ = fmt.Fprint(w, paramDef)
	}

	_, _ = fmt.Fprintf(w, ") {\n")

	// Generate the constructor body
	for _, prop := range pt.properties {
		paramName := javaIdentifier(prop.Name)
		fieldName := javaIdentifier(pt.mod.propertyName(prop))
		isArgsType := pt.args && !prop.IsPlain
		// set default values or assign given values
		hasDefaultValue := prop.DefaultValue != nil
		var defaultValueCode string
		if hasDefaultValue {
			defaultValueString, defaultValueTypeString, err := pt.mod.getDefaultValue(prop.DefaultValue, prop.Type)
			if err != nil {
				return err
			}
			defaultValueInitializer := typeInitializer(prop.Type, isArgsType, false, defaultValueString, defaultValueTypeString)
			defaultValueCode = fmt.Sprintf("%s == null ? %s : ", paramName, defaultValueInitializer)
		}
		if prop.IsRequired {
			_, _ = fmt.Fprintf(w, "        this.%[1]s = %[2]sObjects.requireNonNull(%[3]s, \"expected parameter '%[3]s' to be non-null\");\n", fieldName, defaultValueCode, paramName)
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
			isArgsType := pt.args && !prop.IsPlain
			emptyValue := emptyTypeInitializer(prop.Type, isArgsType, false)
			_, _ = fmt.Fprintf(w, "        this.%s = %s;\n", fieldName, emptyValue)
		}
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// Generate the builder
	var builderFields []builderFieldTemplateContext
	var builderSetters []builderSetterTemplateContext
	for _, prop := range pt.properties {
		isArgsType := pt.args && !prop.IsPlain
		requireInitializers := !pt.args || prop.IsPlain
		propertyName := javaIdentifier(pt.mod.propertyName(prop))
		propertyType := pt.mod.typeString(
			prop.Type,
			pt.propertyTypeQualifier,
			true,                // is input
			pt.state,            // is state
			isArgsType,          // wrap input
			isArgsType,          // has args
			requireInitializers, // requires initializers
			false,               // is optional
			!prop.IsRequired,    // is nullable
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.String(),
			FieldName: propertyName,
		})

		setterName := javaIdentifier("set" + title(prop.Name))
		assignment := func(propertyName string) string {
			if prop.Secret {
				return fmt.Sprintf("this.%s = Input.ofNullable(%s).asSecret()", propertyName, propertyName)
			} else {
				if prop.IsRequired {
					return fmt.Sprintf("this.%s = Objects.requireNonNull(%s)", propertyName, propertyName)
				} else {
					return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
				}
			}
		}

		// add main setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.String(),
			PropertyName: propertyName,
			Assignment:   assignment(propertyName),
		})

		if isArgsType { // we have a wrapped field so we add an unwrapped helper setter
			propertyTypeUnwrapped := pt.mod.typeString(
				prop.Type,
				pt.propertyTypeQualifier,
				true,                // is input
				pt.state,            // is state
				false,               // don't wrap input
				isArgsType,          // has args
				requireInitializers, // requires initializers
				false,               // is optional
				!prop.IsRequired,    // is nullable
			)

			assignmentUnwrapped := func(propertyName string) string {
				if prop.Secret {
					return fmt.Sprintf("this.%s = Input.ofNullable(%s).asSecret()", propertyName, propertyName)
				} else {
					if prop.IsRequired {
						return fmt.Sprintf("this.%s = Input.of(Objects.requireNonNull(%s))", propertyName, propertyName)
					} else {
						return fmt.Sprintf("this.%s = Input.ofNullable(%s)", propertyName, propertyName)
					}
				}
			}

			// add overloaded setter
			builderSetters = append(builderSetters, builderSetterTemplateContext{
				SetterName:   setterName,
				PropertyType: propertyTypeUnwrapped.String(),
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
	}); err != nil {
		return err
	}
	_, _ = fmt.Fprintf(w, "\n")

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (pt *plainType) genOutputType(w io.Writer) error {
	indent := strings.Repeat("    ", 0)

	// Open the class and annotate it appropriately.
	_, _ = fmt.Fprintf(w, "%s@OutputCustomType\n", indent)
	_, _ = fmt.Fprintf(w, "%spublic final class %s {\n", indent, pt.name)

	// Generate each output field.
	for _, prop := range pt.properties {
		fieldName := javaIdentifier(pt.mod.propertyName(prop))
		fieldType := pt.mod.typeString(prop.Type, pt.propertyTypeQualifier, false, false, false, false, false, false, !prop.IsRequired)
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
	_, _ = fmt.Fprintf(w, "%s    @OutputCustomType.Constructor(%s)\n", indent, paramNamesStringBuilder.String())
	_, _ = fmt.Fprintf(w, "%s    private %s(", indent, pt.name)

	// Generate the constructor parameters.
	for i, prop := range pt.properties {
		// TODO: factor this out (with similar code in genInputType)
		paramName := javaIdentifier(prop.Name)
		paramType := pt.mod.typeString(prop.Type, pt.propertyTypeQualifier, false, false, false, false, false, false, !prop.IsRequired)

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
		if prop.IsRequired {
			_, _ = fmt.Fprintf(w, "%s        this.%s = Objects.requireNonNull(%s);\n", indent, fieldName, paramName)
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
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false,
			false,
			false,
			!prop.IsRequired,
			false,
		)
		getterTypeNonOptional := pt.mod.typeString(
			prop.Type,
			pt.propertyTypeQualifier,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
		)

		// TODO: add docs comment
		var returnStatement string
		if prop.IsRequired {
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
				returnStatement = fmt.Sprintf("Optional.ofNullable(this.%s)", paramName)
			}
		}

		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          strings.Repeat("    ", 1),
			GetterType:      getterType.String(),
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
			prop.Type,
			pt.propertyTypeQualifier,
			false,            // is input
			false,            // is state
			false,            // wrap input
			false,            // has args
			false,            // requires initializers
			false,            // is optional
			!prop.IsRequired, // is nullable
		)

		// add field
		builderFields = append(builderFields, builderFieldTemplateContext{
			FieldType: propertyType.String(),
			FieldName: propertyName,
		})

		setterName := javaIdentifier("set" + title(prop.Name))
		assignment := func(propertyName string) string {
			if prop.IsRequired {
				return fmt.Sprintf("this.%s = Objects.requireNonNull(%s)", propertyName, propertyName)
			} else {
				return fmt.Sprintf("this.%s = %s", propertyName, propertyName)
			}
		}

		// add setter
		builderSetters = append(builderSetters, builderSetterTemplateContext{
			SetterName:   setterName,
			PropertyType: propertyType.String(),
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

func genAlias(w io.Writer, alias *schema.Alias) {
	_, _ = fmt.Fprintf(w, "Input.of(")
	_, _ = fmt.Fprintf(w, "Alias.builder()")
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

func (mod *modContext) genResource(w io.Writer, r *schema.Resource) error {
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
	case mod.isK8sCompatMode():
		baseType = "io.pulumi.kubernetes.KubernetesResource"
	case r.IsComponent:
		baseType = "io.pulumi.resources.ComponentResource"
		optionsType = "io.pulumi.resources.ComponentResourceOptions"
	default:
		baseType = "io.pulumi.resources.CustomResource"
	}

	printObsoleteAttribute(w, r.DeprecationMessage, "")
	_, _ = fmt.Fprintf(w, "@ResourceType(type=\"%s\")\n", r.Token)
	_, _ = fmt.Fprintf(w, "public class %s extends %s {\n", className, baseType)

	var secretProps []string
	// Emit all output properties.
	for _, prop := range r.Properties {
		// Write the property attribute
		wireName := prop.Name
		propertyName := mod.propertyName(prop)
		propertyType := mod.typeString(prop.Type, "outputs", false, false, false, false, false, false, !prop.IsRequired)
		// TODO: C# has some kind of workaround here for strings

		if prop.Secret {
			secretProps = append(secretProps, prop.Name)
		}

		// TODO: add docs comment
		outputExportParameters := strings.Join(
			propertyType.ParameterTypesTransformed(func(ts TypeShape) string {
				return ts.StringWithOptions(TypeShapeStringOptions{
					CommentOutAnnotations: true,
					GenericErasure:        true,
					AppendClassLiteral:    true,
				})
			}),
			", ",
		)
		outputExportType := propertyType.StringWithOptions(TypeShapeStringOptions{
			SkipAnnotations:    true,
			GenericErasure:     true,
			AppendClassLiteral: true,
		})
		outputParameterType := propertyType.StringWithOptions(TypeShapeStringOptions{
			CommentOutAnnotations: true,
		})
		_, _ = fmt.Fprintf(w, "    @OutputExport(name=\"%s\", type=%s, parameters={%s})\n", wireName, outputExportType, outputExportParameters)
		_, _ = fmt.Fprintf(w, "    private Output<%s> %s;\n", outputParameterType, propertyName)
		_, _ = fmt.Fprintf(w, "\n")

		// Add getter
		getterType := outputParameterType
		getterName := javaIdentifier("get" + title(prop.Name))
		_, _ = fmt.Fprintf(w, "    public Output<%s> %s() {\n", getterType, getterName)
		_, _ = fmt.Fprintf(w, "        return this.%s;\n", propertyName)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	if len(r.Properties) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
	}

	// Emit the class constructor
	argsClassName := className + "Args"
	argsType := argsClassName

	allOptionalInputs := true
	hasConstInputs := false
	for _, prop := range r.InputProperties {
		allOptionalInputs = allOptionalInputs && !prop.IsRequired
		hasConstInputs = hasConstInputs || prop.ConstValue != nil
	}
	if allOptionalInputs {
		// If the number of required input properties was zero, we can make the args object optional.
		argsType = fmt.Sprintf("@Nullable %s", argsType)
	}

	tok := r.Token
	if r.IsProvider {
		tok = mod.pkg.Name
	}

	argsOverride := fmt.Sprintf("args == null ? %sArgs.Empty : args", className)
	if hasConstInputs {
		argsOverride = "makeArgs(args)"
	}

	// TODO: add docs comment

	_, _ = fmt.Fprintf(w, "    public %s(String name, %s args, @Nullable %s options) {\n", className, argsType, optionsType)
	if r.IsComponent {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, Input.empty()), true);\n", tok, argsOverride)
	} else {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, Input.empty()));\n", tok, argsOverride)
	}
	_, _ = fmt.Fprintf(w, "    }\n")

	// Write a private constructor for the use of `get`.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", "null"
		if r.StateInputs != nil {
			stateParam, stateRef = fmt.Sprintf("@Nullable %sState state, ", className), "state"
		}

		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "    private %s(String name, Input<String> id, %s@Nullable %s options) {\n", className, stateParam, optionsType)
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, id));\n", tok, stateRef)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	if hasConstInputs {
		// Write the method that will calculate the resource arguments.
		_, _ = fmt.Fprintf(w, "\n")
		_, _ = fmt.Fprintf(w, "    private static %s makeArgs(%s args) {\n", argsClassName, argsType)
		_, _ = fmt.Fprintf(w, "        var builder = args == null ? %[1]s.builder() : %[1]s.builder(args);\n", argsClassName)
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
	_, _ = fmt.Fprintf(w, "    private static %[1]s makeResourceOptions(@Nullable %[1]s options, @Nullable Input<String> id) {\n", optionsType)
	_, _ = fmt.Fprintf(w, "        var defaultOptions = %s.builder()\n", optionsType)
	_, _ = fmt.Fprintf(w, "            .setVersion(Utilities.getVersion())\n")

	if len(r.Aliases) > 0 {
		_, _ = fmt.Fprintf(w, "            .setAliases(List.of(\n")
		for i, alias := range r.Aliases {
			_, _ = fmt.Fprintf(w, "                ")
			genAlias(w, alias)
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
		_, _ = fmt.Fprintf(w, "            .setAdditionalSecretOutputs(List.of(\n")
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
			stateParam = fmt.Sprintf("@Nullable %sState state, ", className)
			stateRef = "state, "
			// TODO: add docs param
		}

		_, _ = fmt.Fprintf(w, "    public static %s get(String name, Input<String> id, %s@Nullable %s options) {\n", className, stateParam, optionsType)
		_, _ = fmt.Fprintf(w, "        return new %s(name, id, %soptions);\n", className, stateRef)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (mod *modContext) genFunction(w io.Writer, fun *schema.Function) error {
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
			allOptionalInputs = allOptionalInputs && !prop.IsRequired
		}

		var nullable string
		if allOptionalInputs {
			// If the number of required input properties was zero, we can make the args object optional.
			nullable = "@Nullable "
		}

		argsParamDef = fmt.Sprintf("%s%sArgs args, ", nullable, className)
		argsParamRef = fmt.Sprintf("args == null ? %sArgs.Empty : args", className)
	}

	printObsoleteAttribute(w, fun.DeprecationMessage, "")
	// Open the class we'll use for datasources.
	_, _ = fmt.Fprintf(w, "public class %s {\n", className)

	// TODO: add docs comment

	// Emit the datasource method.
	_, _ = fmt.Fprintf(w, "    public static CompletableFuture<%s> invokeAsync(%s@Nullable io.pulumi.deployment.InvokeOptions options) {\n",
		typeParameter, argsParamDef)
	_, _ = fmt.Fprintf(w, "        return io.pulumi.deployment.Deployment.getInstance().invokeAsync(\"%s\", io.pulumi.core.internal.Reflection.TypeShape.of(%s.class), %s, Utilities.withVersion(options));\n",
		fun.Token, typeParameter, argsParamRef)
	_, _ = fmt.Fprintf(w, "    }\n")

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func printObsoleteAttribute(w io.Writer, deprecationMessage, indent string) {
	if deprecationMessage != "" {
		_, _ = fmt.Fprintf(w, "%s@Deprecated /* %s */\n", indent, strings.Replace(deprecationMessage, `"`, `""`, -1))
	}
}

func (mod *modContext) genEnum(w io.Writer, qualifier string, enum *schema.EnumType) error {
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

	underlyingType := mod.typeString(enum.ElementType, qualifier, false, false, false, false, false, false, false)
	switch enum.ElementType {
	case schema.IntType, schema.StringType, schema.NumberType:
		// Open the enum and annotate it appropriately.
		_, _ = fmt.Fprintf(w, "%s@EnumType\n", indent)
		_, _ = fmt.Fprintf(w, "%spublic enum %s {\n", indent, enumName)
		indent := strings.Repeat(indent, 2)

		// Enum values
		for i, e := range enum.Elements {
			// TODO: add docs comment
			printObsoleteAttribute(w, e.DeprecationMessage, indent)
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
			_, _ = fmt.Fprintf(w, "%s    this.value = Objects.requireNonNull(value);\n", indent)
		} else {
			_, _ = fmt.Fprintf(w, "%s    this.value = value;\n", indent)
		}
		_, _ = fmt.Fprintf(w, "%s}\n", indent)
		_, _ = fmt.Fprintf(w, "\n")

		// Explicit conversion operator
		_, _ = fmt.Fprintf(w, "%[1]s@EnumType.Converter\n", indent)
		_, _ = fmt.Fprintf(w, "%[1]spublic %s getValue() {\n", indent, underlyingType)
		_, _ = fmt.Fprintf(w, "%s    return this.value;\n", indent)
		_, _ = fmt.Fprintf(w, "%s}\n", indent)
		_, _ = fmt.Fprintf(w, "\n")

		// toString override
		_, _ = fmt.Fprintf(w, "%s@Override\n", indent)
		_, _ = fmt.Fprintf(w, "%spublic String toString() {\n", indent)
		_, _ = fmt.Fprintf(w, "%s    return new StringJoiner(\", \", \"%s[\", \"]\")\n", indent, enumName)
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

func visitObjectTypes(properties []*schema.Property, visitor func(*schema.ObjectType, bool)) {
	codegen.VisitTypeClosure(properties, func(t codegen.Type) {
		if o, ok := t.Type.(*schema.ObjectType); ok {
			visitor(o, t.Plain)
		}
	})
}

func (mod *modContext) genType(w io.Writer, obj *schema.ObjectType, propertyTypeQualifier string, input, state, args bool) error {
	pt := &plainType{
		mod:                   mod,
		name:                  mod.typeName(obj, state, input, args),
		comment:               obj.Comment,
		propertyTypeQualifier: propertyTypeQualifier,
		properties:            obj.Properties,
		state:                 state,
		args:                  args,
	}

	if input {
		pt.baseClass = "io.pulumi.resources.ResourceArgs"
		if !args && mod.details(obj).plainType {
			pt.baseClass = "io.pulumi.resources.InvokeArgs"
		}
		return pt.genInputType(w)
	}

	return pt.genOutputType(w)
}

// pulumiImports is a slice of common imports that are used with the genHeader method.
var pulumiImports = []string{
	"javax.annotation.Nullable",
	"java.util.Objects",
	"java.util.Optional",
	"java.util.Map",
	"java.util.List",
	"java.util.concurrent.CompletableFuture",
	"io.pulumi.core.*",
	"io.pulumi.core.internal.annotations.*",
}

func (mod *modContext) getUtilitiesImport() string {
	return mod.rootPackageName + ".Utilities"
}

func (mod *modContext) getPulumiImports() []string {
	return append(pulumiImports, mod.getUtilitiesImport())
}

func (mod *modContext) getTypeImports(t schema.Type, recurse bool, imports map[string]codegen.StringSet, seen codegen.Set) {
	if seen.Has(t) {
		return
	}
	seen.Add(t)

	switch t := t.(type) {
	case *schema.ArrayType:
		mod.getTypeImports(t.ElementType, recurse, imports, seen)
		return
	case *schema.MapType:
		mod.getTypeImports(t.ElementType, recurse, imports, seen)
		return
	case *schema.ObjectType:
		for _, p := range t.Properties {
			mod.getTypeImports(p.Type, recurse, imports, seen)
		}
		return
	case *schema.ResourceType:
		// If it's an external resource, we'll be using fully-qualified type names,
		// so there's no need for an import.
		if t.Resource != nil && t.Resource.Package != mod.pkg {
			return
		}

		modName, name, modPath := mod.pkg.TokenToModule(t.Token), tokenToName(t.Token), ""
		if modName != mod.mod {
			mp, err := filepath.Rel(mod.mod, modName)
			contract.Assert(err == nil)
			if path.Base(mp) == "." {
				mp = path.Dir(mp)
			}
			modPath = filepath.ToSlash(mp)
		}
		if len(modPath) == 0 {
			return
		}
		if imports[modPath] == nil {
			imports[modPath] = codegen.NewStringSet()
		}
		imports[modPath].Add(name)
		return
	case *schema.TokenType:
		return
	case *schema.UnionType:
		for _, e := range t.ElementTypes {
			mod.getTypeImports(e, recurse, imports, seen)
		}
		return
	default:
		return
	}
}

func (mod *modContext) getImports(member interface{}, imports map[string]codegen.StringSet) {
	seen := codegen.Set{}
	switch member := member.(type) {
	case *schema.ObjectType:
		for _, p := range member.Properties {
			mod.getTypeImports(p.Type, true, imports, seen)
		}
		return
	case *schema.ResourceType:
		mod.getTypeImports(member, true, imports, seen)
		return
	case *schema.Resource:
		for _, p := range member.Properties {
			mod.getTypeImports(p.Type, false, imports, seen)
		}
		for _, p := range member.InputProperties {
			mod.getTypeImports(p.Type, false, imports, seen)
		}
		return
	case *schema.Function:
		if member.Inputs != nil {
			mod.getTypeImports(member.Inputs, false, imports, seen)
		}
		if member.Outputs != nil {
			mod.getTypeImports(member.Outputs, false, imports, seen)
		}
		return
	case []*schema.Property:
		for _, p := range member {
			mod.getTypeImports(p.Type, false, imports, seen)
		}
		return
	default:
		return
	}
}

func (mod *modContext) genHeader(w io.Writer, imports []string, qualifier string) {
	if len(qualifier) > 0 {
		qualifier = "." + qualifier
	}
	_, _ = fmt.Fprintf(w, "// *** WARNING: this file was generated by %v. ***\n", mod.tool)
	_, _ = fmt.Fprintf(w, "// *** Do not edit by hand unless you're certain you know what you are doing! ***\n")
	_, _ = fmt.Fprintf(w, "\n")
	_, _ = fmt.Fprintf(w, "package %s%s;\n", mod.packageName, qualifier)
	_, _ = fmt.Fprintf(w, "\n")

	for _, i := range imports {
		_, _ = fmt.Fprintf(w, "import %s;\n", i)
	}
	if len(imports) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
	}
}

func (mod *modContext) getConfigProperty(schemaType schema.Type, key string) (TypeShape, MethodCall) {
	propertyType := mod.typeString(
		schemaType,
		"types",
		false,
		false,
		false,
		false, /*args*/
		false,
		true, // all config's 'get*' methods return an Optional
		false,
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
				return mod.getConfigProperty(t.UnderlyingType, key)
			}
		}
		// TODO: C# has a special case for Arrays here, should we port it?

		getFunc.Method = "getObject"
		getFunc.Args = append(getFunc.Args, propertyType.StringJavaTypeShape())
	}

	return propertyType, getFunc
}

func (mod *modContext) genConfig(variables []*schema.Property) (string, error) {
	w := &bytes.Buffer{}

	mod.genHeader(w, []string{
		"java.util.Optional",
	}, "")

	// Open the config class.
	_, _ = fmt.Fprintf(w, "public final class Config {\n")
	_, _ = fmt.Fprintf(w, "\n")
	// Create a config bag for the variables to pull from.
	_, _ = fmt.Fprintf(w, "    private static final io.pulumi.Config config = io.pulumi.Config.of(%q);", mod.pkg.Name)
	_, _ = fmt.Fprintf(w, "\n")

	// Emit an entry for all config variables.
	for _, p := range variables {
		propertyType, getFunc := mod.getConfigProperty(p.Type, p.Name)
		propertyName := javaIdentifier(mod.propertyName(p))

		returnStatement := getFunc.String()

		hasDefaultValue := p.DefaultValue != nil
		if hasDefaultValue {
			defaultValueString, defaultValueTypeString, err := mod.getDefaultValue(p.DefaultValue, p.Type)
			if err != nil {
				return "", err
			}
			defaultValueInitializer := typeInitializer(p.Type, false, false, defaultValueString, defaultValueTypeString)
			returnStatement += ".orElse(" + defaultValueInitializer + ")"
		}

		// TODO: printComment(w, p.Comment, "        ")
		if err := getterTemplate.Execute(w, getterTemplateContext{
			Indent:          strings.Repeat("    ", 1),
			GetterType:      propertyType.String(),
			GetterName:      propertyName,
			ReturnStatement: returnStatement,
		}); err != nil {
			return "", err
		}
		_, _ = fmt.Fprintf(w, "\n")
	}

	// TODO: finish the config generation, emit any nested types.

	// Close the config class and namespace.
	_, _ = fmt.Fprintf(w, "}\n")

	return w.String(), nil
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

// Set to avoid generating a file with the same name twice.
var generatedPaths = codegen.Set{}

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

		// TODO: how can we get rid of this?
		// The way the legacy codegen for kubernetes is structured, inputs for a resource args type and resource args
		// subtype could become a single class because of the name + namespace clash. We use a set of generated types
		// to prevent generating classes with equal full names in multiple files. The check should be removed if we
		// ever change the namespacing in the k8s SDK to the standard one.
		if mod.isK8sCompatMode() {
			if generatedPaths.Has(p) {
				return
			}
			generatedPaths.Add(p)
			fs.add(p, []byte(contents))
		} else {
			fs.add(p, []byte(contents))
		}
	}

	// Utilities, config
	switch mod.mod {
	case "":
		utilities, err := mod.genUtilities()
		if err != nil {
			return err
		}
		fs.add(filepath.Join(dir, "Utilities.java"), []byte(utilities))

		// Ensure that the target module directory contains a README.md file.
		readme := mod.pkg.Description
		if readme != "" && readme[len(readme)-1] != '\n' {
			readme += "\n"
		}
		fs.add("README.md", []byte(readme))
	case "config":
		if len(mod.pkg.Config) > 0 {
			config, err := mod.genConfig(mod.pkg.Config)
			if err != nil {
				return err
			}
			addFile("Config.java", config)
			return nil
		}
	}

	// Resources
	for _, r := range mod.resources {
		imports := map[string]codegen.StringSet{}
		mod.getImports(r, imports)

		buffer := &bytes.Buffer{}
		var additionalImports []string
		for _, i := range imports {
			additionalImports = append(additionalImports, i.SortedValues()...)
		}
		sort.Strings(additionalImports)
		importStrings := mod.getPulumiImports()
		importStrings = append(importStrings, additionalImports...)
		importStrings = append(importStrings, mod.packageName+".inputs.*")
		mod.genHeader(buffer, importStrings, "")
		if err := mod.genResource(buffer, r); err != nil {
			return err
		}
		addFile(resourceName(r)+".java", buffer.String())

		// Generate the resource args type.
		args := &plainType{
			mod:                   mod,
			res:                   r,
			name:                  resourceName(r) + "Args",
			baseClass:             "io.pulumi.resources.ResourceArgs",
			propertyTypeQualifier: "inputs",
			properties:            r.InputProperties,
			args:                  true,
		}
		argsBuffer := &bytes.Buffer{}
		mod.genHeader(argsBuffer, mod.getPulumiImports(), "inputs")
		if err := args.genInputType(argsBuffer); err != nil {
			return err
		}
		addFile(path.Join("inputs", resourceName(r)+"Args.java"), argsBuffer.String())

		// Generate the `get` args type, if any.
		if r.StateInputs != nil {
			state := &plainType{
				mod:                   mod,
				res:                   r,
				name:                  resourceName(r) + "State",
				baseClass:             "io.pulumi.resources.ResourceArgs",
				propertyTypeQualifier: "inputs",
				properties:            r.StateInputs.Properties,
				args:                  true,
				state:                 true,
			}
			stateBuffer := &bytes.Buffer{}
			mod.genHeader(stateBuffer, mod.getPulumiImports(), "inputs")
			if err := state.genInputType(stateBuffer); err != nil {
				return err
			}
			addFile(path.Join("inputs", resourceName(r)+"State.java"), stateBuffer.String())
		}
	}

	// Functions
	for _, f := range mod.functions {
		imports := map[string]codegen.StringSet{}
		mod.getImports(f, imports)

		buffer := &bytes.Buffer{}
		importStrings := mod.getPulumiImports()
		for _, i := range imports {
			importStrings = append(importStrings, i.SortedValues()...)
		}
		if f.Inputs != nil {
			importStrings = append(importStrings, mod.packageName+".inputs.*")
		}
		if f.Outputs != nil {
			importStrings = append(importStrings, mod.packageName+".outputs.*")
		}
		mod.genHeader(buffer, importStrings, "")
		if err := mod.genFunction(buffer, f); err != nil {
			return err
		}
		addFile(tokenToName(f.Token)+".java", buffer.String())

		// Emit the args and result types, if any.
		if f.Inputs != nil {
			args := &plainType{
				mod:                   mod,
				name:                  tokenToName(f.Token) + "Args",
				baseClass:             "io.pulumi.resources.InvokeArgs",
				propertyTypeQualifier: "inputs",
				properties:            f.Inputs.Properties,
			}
			argsBuffer := &bytes.Buffer{}
			mod.genHeader(argsBuffer, importStrings, "inputs")
			if err := args.genInputType(argsBuffer); err != nil {
				return err
			}
			addFile(path.Join("inputs", tokenToName(f.Token)+"Args.java"), argsBuffer.String())
		}

		if f.Outputs != nil {
			res := &plainType{
				mod:                   mod,
				name:                  tokenToName(f.Token) + "Result",
				propertyTypeQualifier: "outputs",
				properties:            f.Outputs.Properties,
			}
			resultBuffer := &bytes.Buffer{}
			mod.genHeader(resultBuffer, importStrings, "outputs")
			if err := res.genOutputType(resultBuffer); err != nil {
				return err
			}
			addFile(path.Join("outputs", tokenToName(f.Token)+"Result.java"), resultBuffer.String())
		}
	}

	// Input/Output types
	for _, t := range mod.types {
		if mod.details(t).inputType || mod.details(t).stateType {
			if mod.details(t).inputType {
				if mod.details(t).argsType {
					buffer := &bytes.Buffer{}
					mod.genHeader(buffer, mod.getPulumiImports(), "inputs")
					if err := mod.genType(buffer, t, "inputs", true, false, true); err != nil {
						return err
					}
					addFile(path.Join("inputs", mod.typeName(t, false, true, true)+".java"), buffer.String())
				}
				if mod.details(t).plainType {
					buffer := &bytes.Buffer{}
					mod.genHeader(buffer, mod.getPulumiImports(), "inputs")
					if err := mod.genType(buffer, t, "inputs", true, false, false); err != nil {
						return err
					}
					addFile(path.Join("inputs", mod.typeName(t, false, true, false)+".java"), buffer.String())
				}
			}

			if mod.details(t).stateType {
				buffer := &bytes.Buffer{}
				mod.genHeader(buffer, mod.getPulumiImports(), "inputs")
				if err := mod.genType(buffer, t, "inputs", true, true, true); err != nil {
					return err
				}
				addFile(path.Join("inputs", mod.typeName(t, true, true, true)+".java"), buffer.String())
			}
		}
		if mod.details(t).outputType {
			buffer := &bytes.Buffer{}
			mod.genHeader(buffer, mod.getPulumiImports(), "outputs")
			if err := mod.genType(buffer, t, "outputs", false, false, false); err != nil {
				return err
			}
			addFile(path.Join("outputs", mod.typeName(t, false, false, false)+".java"), buffer.String())
		}
	}

	// Enums
	if len(mod.enums) > 0 {
		importStrings := mod.getPulumiImports()
		importStrings = append(importStrings, []string{
			"java.util.StringJoiner",
		}...)

		for i, enum := range mod.enums {
			buffer := &bytes.Buffer{}
			mod.genHeader(buffer, importStrings, "enums")
			if err := mod.genEnum(buffer, "enums", enum); err != nil {
				return err
			}
			if i != len(mod.enums)-1 {
				_, _ = fmt.Fprintf(buffer, "\n")
			}
			addFile(path.Join("enums", tokenToName(enum.Token)+".java"), buffer.String())
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
		computePropertyNames(r.Properties, propertyNames)
		computePropertyNames(r.InputProperties, propertyNames)
		if r.StateInputs != nil {
			computePropertyNames(r.StateInputs.Properties, propertyNames)
		}
	}
	for _, f := range pkg.Functions {
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
				compatibility:          info.Compatibility,
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

	visitObjectTypes(pkg.Config, func(t *schema.ObjectType, _ bool) {
		getModFromToken(t.Token, pkg).details(t).outputType = true
	})

	// Find input and output types referenced by resources.
	scanResource := func(r *schema.Resource) {
		mod := getModFromToken(r.Token, pkg)
		mod.resources = append(mod.resources, r)
		visitObjectTypes(r.Properties, func(t *schema.ObjectType, _ bool) {
			getModFromToken(t.Token, t.Package).details(t).outputType = true
		})
		visitObjectTypes(r.InputProperties, func(t *schema.ObjectType, plain bool) {
			if r.IsProvider {
				getModFromToken(t.Token, t.Package).details(t).outputType = true
			}
			getModFromToken(t.Token, t.Package).details(t).inputType = true
			if plain {
				getModFromToken(t.Token, t.Package).details(t).plainType = true
			} else {
				getModFromToken(t.Token, t.Package).details(t).argsType = true
			}
		})
		if r.StateInputs != nil {
			visitObjectTypes(r.StateInputs.Properties, func(t *schema.ObjectType, _ bool) {
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
			visitObjectTypes(f.Inputs.Properties, func(t *schema.ObjectType, _ bool) {
				details := getModFromToken(t.Token, t.Package).details(t)
				details.inputType = true
				details.plainType = true
			})
		}
		if f.Outputs != nil {
			visitObjectTypes(f.Outputs.Properties, func(t *schema.ObjectType, _ bool) {
				details := getModFromToken(t.Token, t.Package).details(t)
				details.outputType = true
				details.plainType = true
			})
		}
	}

	// Find nested types.
	for _, t := range pkg.Types {
		switch typ := t.(type) {
		case *schema.ObjectType:
			mod := getModFromToken(typ.Token, pkg)
			mod.types = append(mod.types, typ)
		case *schema.EnumType:
			mod := getModFromToken(typ.Token, pkg)
			mod.enums = append(mod.enums, typ)
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
