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

const basePackage = "io.pulumi."

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

// Title converts the input string to a title case
// where only the initial letter is upper-cased.
func Title(s string) string {
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

// TODO

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
	return Title(components[2])
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

	pkg := basePackage + packageName(mod.packages, components[0])
	pkgName := mod.pkg.TokenToModule(tok)

	if mod.isK8sCompatMode() {
		if qualifier != "" {
			return pkg + ".Types." + qualifier + "." + packageName(mod.packages, pkgName)
		}
	}

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
	if !mod.isTFCompatMode() && !mod.isK8sCompatMode() {
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
	wrapInput bool,
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
		typ.Type += tokenToName(t.Token)
	case *schema.ArrayType:
		var listType string
		switch {
		case wrapInput:
			listType, optional = "InputList", false
		case requireInitializers:
			listType = "List"
		default:
			listType, optional = "ImmutableList", false
		}

		wrapInput = false
		typ.Type = listType
		typ.Parameters = append(
			typ.Parameters,
			mod.typeStringInner(t.ElementType, qualifier, input, state, wrapInput, args, false, false, false),
		)
	case *schema.MapType:
		var mapType string
		switch {
		case wrapInput:
			mapType, optional = "InputMap", false
		case requireInitializers:
			mapType = "Map"
			typ.Parameters = append(
				typ.Parameters, TypeShape{Type: "String"},
			)
		default:
			mapType = "ImmutableMap"
			typ.Parameters = append(
				typ.Parameters, TypeShape{Type: "String"},
			)
		}

		wrapInput = false
		typ.Type = mapType
		typ.Parameters = append(
			typ.Parameters,
			mod.typeStringInner(t.ElementType, qualifier, input, state, wrapInput, args, false, false, false),
		)
	case *schema.ObjectType:
		namingCtx := mod
		if t.Package != mod.pkg {
			// If object type belongs to another package, we apply naming conventions from that package,
			// including namespace naming and compatibility mode.
			extPkg := t.Package
			var info JVMPackageInfo
			contract.AssertNoError(extPkg.ImportLanguages(map[string]schema.Language{"jvm": Importer}))
			if v, ok := t.Package.Language["jvm"].(JVMPackageInfo); ok {
				info = v
			}
			namingCtx = &modContext{
				pkg:           extPkg,
				packages:      info.Packages,
				compatibility: info.Compatibility,
			}
		}
		objectType := namingCtx.tokenToPackage(t.Token, qualifier)
		if (objectType == namingCtx.packageName && qualifier == "") || objectType == namingCtx.packageName+"."+qualifier {
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
			resourceType = fmt.Sprintf("%s%s.provider", basePackage, packageName(mod.packages, pkgName))
		} else {
			namingCtx := mod
			// TODO
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
			if wrapInput {
				unionType = "InputUnion"
			}
			wrapInput = false
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
			if wrapInput {
				typ.Type = "InputJson"
				wrapInput = false
			} else {
				typ.Type = "com.google.gson.JsonElement"
			}
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
			Type:       "java.util.Optional",
			Parameters: []TypeShape{typ},
		}
		typ = optionalTyp
	}
	if nullable {
		typ.Annotations = append(typ.Annotations, "@Nullable")
	}
	return typ
}

// TODO

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

func (pt *plainType) genInputProperty(w io.Writer, prop *schema.Property, indent string) {
	argsType := pt.args && !prop.IsPlain
	requireInitializers := !pt.args || prop.IsPlain

	wireName := prop.Name
	propertyName := pt.mod.propertyName(prop)
	propertyType := pt.mod.typeString(
		prop.Type,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		argsType,            // wrap input
		argsType,            // has args
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

	indent = strings.Repeat(indent, 2)

	// TODO: add comment
	_, _ = fmt.Fprintf(w, "%s@InputImport(name=\"%s\"%s)\n", indent, wireName, attributeArgs)
	_, _ = fmt.Fprintf(w, "%sprivate %s %s;\n", indent, propertyType, propertyName)
	_, _ = fmt.Fprintf(w, "\n")

	// Add getter
	getterType := pt.mod.typeString(
		prop.Type,
		pt.propertyTypeQualifier,
		true,                // is input
		pt.state,            // is state
		argsType,            // wrap input
		argsType,            // has args
		requireInitializers, // FIMXE: should not require initializers
		!prop.IsRequired,    // is optional
		false,               // is nullable
	)
	getterName := Title(prop.Name)
	_, _ = fmt.Fprintf(w, "%spublic %s get%s() {\n", indent, getterType, getterName)
	required := prop.IsRequired
	if required {
		_, _ = fmt.Fprintf(w, "%s    return this.%s;\n", indent, propertyName)
	} else {
		_, _ = fmt.Fprintf(w, "%s    return java.util.Optional.ofNullable(this.%s);\n", indent, propertyName)
	}
	_, _ = fmt.Fprintf(w, "%s}\n", indent)

	// TODO
}

// TODO

func (pt *plainType) genInputType(w io.Writer, level int) error {
	// TODO: k8s compat mode generated type counting logic

	indent := strings.Repeat("    ", level)

	_, _ = fmt.Fprintf(w, "\n")

	final := "final "
	if pt.mod.isK8sCompatMode() && (pt.res == nil || !pt.res.IsProvider) {
		final = ""
	}

	// Open the class.
	// TODO: add comment
	_, _ = fmt.Fprintf(w, "%spublic static %sclass %s extends %s {\n", indent, final, pt.name, pt.baseClass)

	// Declare each input property.
	for _, p := range pt.properties {
		pt.genInputProperty(w, p, indent)
		_, _ = fmt.Fprintf(w, "\n")
	}

	// Generate a constructor that will set default values.
	_, _ = fmt.Fprintf(w, "%s    public %s() {\n", indent, pt.name)
	for _, prop := range pt.properties {
		if prop.DefaultValue != nil {
			dv, err := pt.mod.getDefaultValue(prop.DefaultValue, prop.Type)
			if err != nil {
				return err
			}
			propertyName := pt.mod.propertyName(prop)
			_, _ = fmt.Fprintf(w, "%s    this.%s = %s;\n", indent, propertyName, dv)
		}
	}
	_, _ = fmt.Fprintf(w, "%s    }\n", indent)

	// Close the class.
	_, _ = fmt.Fprintf(w, "%s}\n", indent)

	return nil
}

func (pt *plainType) genOutputType(w io.Writer, level int) {
	indent := strings.Repeat("    ", level)

	_, _ = fmt.Fprintf(w, "\n")

	// Open the class and attribute it appropriately.
	_, _ = fmt.Fprintf(w, "%s@OutputCustomType\n", indent)
	_, _ = fmt.Fprintf(w, "%spublic static final class %s {\n", indent, pt.name)

	// Generate each output field.
	for _, prop := range pt.properties {
		fieldName := pt.mod.propertyName(prop)
		required := prop.IsRequired || pt.mod.isK8sCompatMode()
		fieldType := pt.mod.typeString(prop.Type, pt.propertyTypeQualifier, false, false, false, false, false, false, !required)
		// TODO: add comment
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
		paramName := javaIdentifier(prop.Name)
		required := prop.IsRequired || pt.mod.isK8sCompatMode()
		paramType := pt.mod.typeString(prop.Type, pt.propertyTypeQualifier, false, false, false, false, false, false, !required)

		if i == 0 && len(pt.properties) > 1 { // first
			_, _ = fmt.Fprint(w, "\n")
		}

		terminator := ""
		if i != len(pt.properties)-1 { // not last
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
		fieldName := pt.mod.propertyName(prop)
		_, _ = fmt.Fprintf(w, "%s        this.%s = %s;\n", indent, fieldName, paramName)
	}
	_, _ = fmt.Fprintf(w, "%s    }\n", indent)
	_, _ = fmt.Fprintf(w, "\n")

	// Generate getters
	for _, prop := range pt.properties {
		paramName := javaIdentifier(prop.Name)
		getterName := Title(paramName)
		required := prop.IsRequired || pt.mod.isK8sCompatMode()
		getterType := pt.mod.typeString(prop.Type, pt.propertyTypeQualifier, false, false, false, false, false, !required, false)
		// TODO: add comment
		_, _ = fmt.Fprintf(w, "%s    public %s get%s() {\n", indent, getterType, getterName)
		if required {
			_, _ = fmt.Fprintf(w, "%s        return this.%s;\n", indent, paramName)
		} else {
			_, _ = fmt.Fprintf(w, "%s        return java.util.Optional.ofNullable(this.%s);\n", indent, paramName)
		}
		_, _ = fmt.Fprintf(w, "%s    }\n", indent)
	}

	// Close the class.
	_, _ = fmt.Fprintf(w, "%s}\n", indent)
}

func primitiveValue(value interface{}) (string, error) {
	v := reflect.ValueOf(value)
	if v.Kind() == reflect.Interface {
		v = v.Elem()
	}

	switch v.Kind() {
	case reflect.Bool:
		if v.Bool() {
			return "true", nil
		}
		return "false", nil
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32:
		return strconv.FormatInt(v.Int(), 10), nil
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32:
		return strconv.FormatUint(v.Uint(), 10), nil
	case reflect.Float32, reflect.Float64:
		return strconv.FormatFloat(v.Float(), 'f', -1, 64), nil
	case reflect.String:
		return fmt.Sprintf("%q", v.String()), nil
	default:
		return "", errors.Errorf("unsupported default value of type %T", value)
	}
}

func (mod *modContext) getDefaultValue(dv *schema.DefaultValue, t schema.Type) (string, error) {
	var val string
	if dv.Value != nil {
		switch enum := t.(type) {
		case *schema.EnumType:
			enumName := tokenToName(enum.Token)
			for _, e := range enum.Elements {
				if e.Value != dv.Value {
					continue
				}

				elName := e.Name
				if elName == "" {
					elName = fmt.Sprintf("%v", e.Value)
				}
				safeName, err := makeSafeEnumName(elName, enumName)
				if err != nil {
					return "", err
				}
				val = fmt.Sprintf("%s.%s.%s", mod.packageName, enumName, safeName)
				break
			}
			if val == "" {
				return "", errors.Errorf("default value '%v' not found in enum '%s'", dv.Value, enumName)
			}
		default:
			v, err := primitiveValue(dv.Value)
			if err != nil {
				return "", err
			}
			val = v
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

		getEnv := fmt.Sprintf("Utilities.getEnv%s(%s)", getType, envVars)
		if val != "" {
			val = fmt.Sprintf("%s == null ? %s : %s", getEnv, val, getEnv)
		} else {
			val = getEnv
		}
	}

	return val, nil
}

func genAlias(w io.Writer, alias *schema.Alias) {
	// TODO
}

func (mod *modContext) genResource(w io.Writer, r *schema.Resource) error {
	// Create a resource module file into which all of this resource's types will go.
	name := resourceName(r)

	// TODO: add comment

	// Open the class.
	className := name
	var baseType string
	optionsType := "io.pulumi.resources.CustomResourceOptions"
	switch {
	case r.IsProvider:
		baseType = "io.pulumi.resources.ProviderResource"
	case mod.isK8sCompatMode():
		baseType = "io.pulumi.resources.KubernetesResource"
	case r.IsComponent:
		baseType = "io.pulumi.resources.ComponentResource"
		optionsType = "io.pulumi.resources.ComponentResourceOptions"
	default:
		baseType = "io.pulumi.resources.CustomResource"
	}

	if r.DeprecationMessage != "" {
		_, _ = fmt.Fprintf(w, "@Deprecated(\"%s\")\n", strings.Replace(r.DeprecationMessage, `"`, `""`, -1))
	}
	_, _ = fmt.Fprintf(w, "@ResourceType(type=\"%s\")\n", r.Token)
	_, _ = fmt.Fprintf(w, "public class %s extends %s {\n", className, baseType)

	var secretProps []string
	// Emit all output properties.
	for _, prop := range r.Properties {
		// Write the property attribute
		wireName := prop.Name
		propertyName := mod.propertyName(prop)
		required := prop.IsRequired || mod.isK8sCompatMode()
		propertyType := mod.typeString(prop.Type, "Outputs", false, false, false, false, false, false, !required)
		// TODO: C# has some kind of workaround here

		if prop.Secret {
			secretProps = append(secretProps, prop.Name)
		}

		// TODO: add comment
		appendDotClass := func(s string) string {
			return s + ".class"
		}
		outputExportParameters := strings.Join(
			mapStrings(propertyType.ParameterTypes(), appendDotClass),
			", ",
		)
		outputExportType := appendDotClass(propertyType.Type)
		outputParameterType := propertyType.StringWithOptions(StringOptions{CommentOutAnnotations: true})
		_, _ = fmt.Fprintf(w, "    @OutputExport(name=\"%s\", type=%s, parameters={%s})\n", wireName, outputExportType, outputExportParameters)
		_, _ = fmt.Fprintf(w, "    private Output<%s> %s;\n", outputParameterType, propertyName)
		_, _ = fmt.Fprintf(w, "\n")

		// Add getter
		getterType := outputParameterType
		getterName := Title(prop.Name)
		_, _ = fmt.Fprintf(w, "    public Output<%s> get%s() {\n", getterType, getterName)
		_, _ = fmt.Fprintf(w, "        return this.%s;\n", propertyName)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	if len(r.Properties) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
	}

	// Emit the class constructor.
	argsClassName := className + ".Args"
	if mod.isK8sCompatMode() && !r.IsProvider {
		argsClassName = fmt.Sprintf("%s.%sArgs", mod.tokenToPackage(r.Token, "Inputs"), className)
	}
	argsType := argsClassName

	// TODO
	hasConstInputs := false
	// TODO

	tok := r.Token
	if r.IsProvider {
		tok = mod.pkg.Name
	}

	argsOverride := fmt.Sprintf("args == null ? %s.Args.Empty : args", className)
	if hasConstInputs {
		argsOverride = "makeArgs(args)"
	}

	// TODO: add docs

	_, _ = fmt.Fprintf(w, "    public %s(String name, %s args, @Nullable %s options) {\n", className, argsType, optionsType)
	if r.IsComponent {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, Input.empty()), true);\n", tok, argsOverride)
	} else {
		_, _ = fmt.Fprintf(w, "        super(\"%s\", name, %s, makeResourceOptions(options, Input.empty()));\n", tok, argsOverride)
	}
	_, _ = fmt.Fprintf(w, "    }\n")

	// Write a private constructor for the use of `Get`.
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
		_, _ = fmt.Fprintf(w, "    private static %[1]s makeArgs(%[1]s args) {\n", argsType)
		_, _ = fmt.Fprintf(w, "        return args != null ? args : %s.builder()\n", argsClassName)
		for _, prop := range r.InputProperties {
			if prop.ConstValue != nil {
				v, err := primitiveValue(prop.ConstValue)
				if err != nil {
					return err
				}
				setterSuffix := Title(mod.propertyName(prop))
				_, _ = fmt.Fprintf(w, "        .set%s(%s);\n", setterSuffix, v)
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
	// TODO: aliases and secrets
	_, _ = fmt.Fprintf(w, "            .build();\n")
	_, _ = fmt.Fprintf(w, "        return %s.merge(defaultOptions, options, id);\n", optionsType)
	_, _ = fmt.Fprintf(w, "    }\n\n")

	// Write the `Get` method for reading instances of this resource unless this is a provider resource or ComponentResource.
	if !r.IsProvider && !r.IsComponent {
		stateParam, stateRef := "", ""

		// TODO: add docs
		if r.StateInputs != nil {
			stateParam = fmt.Sprintf("@Nullable %sState state, ", className)
			stateRef = "state, "
			// TODO: add docs param
		}

		_, _ = fmt.Fprintf(w, "    public static %s get(String name, Input<String> id, %s@Nullable %s options) {\n", className, stateParam, optionsType)
		_, _ = fmt.Fprintf(w, "        return new %s(name, id, %soptions);\n", className, stateRef)
		_, _ = fmt.Fprintf(w, "    }\n")
	}

	// TODO: k8s compat mode?

	// Generate the resource args type.
	args := &plainType{
		mod:                   mod,
		res:                   r,
		name:                  "Args",
		baseClass:             "io.pulumi.resources.ResourceArgs",
		propertyTypeQualifier: "Inputs",
		properties:            r.InputProperties,
		args:                  true,
	}
	if err := args.genInputType(w, 1); err != nil {
		return err
	}

	// Generate the `Get` args type, if any.
	if r.StateInputs != nil {
		state := &plainType{
			mod:                   mod,
			res:                   r,
			name:                  "State",
			baseClass:             "io.pulumi.resources.ResourceArgs",
			propertyTypeQualifier: "Inputs",
			properties:            r.StateInputs.Properties,
			args:                  true,
			state:                 true,
		}
		if err := state.genInputType(w, 1); err != nil {
			return err
		}
	}

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (mod *modContext) genFunction(w io.Writer, fun *schema.Function) error {
	className := tokenToFunctionName(fun.Token)

	var typeParameter string
	if fun.Outputs != nil {
		typeParameter = fmt.Sprintf("%s.Result", className)
	}

	argsParamDef := "io.pulumi.deployment.InvokeOptions"
	argsParamRef := "io.pulumi.resources.InvokeArgs.Empty"

	// TODO

	if fun.DeprecationMessage != "" {
		_, _ = fmt.Fprintf(w, "@Deprecated(\"%s\")\n", strings.Replace(fun.DeprecationMessage, `"`, `""`, -1))
	}
	// Open the class we'll use for datasources.
	_, _ = fmt.Fprintf(w, "public class %s {\n", className)

	// TODO: add comment

	// Emit the datasource method.
	_, _ = fmt.Fprintf(w, "    public static CompletableFuture<%s> invokeAsync(%s options) {\n",
		typeParameter, argsParamDef)
	_, _ = fmt.Fprintf(w, "        return io.pulumi.deployment.Deployment.getInstance().invokeAsync(\"%s\", io.pulumi.core.internal.Reflection.TypeShape.of(%s.class), %s, Utilities.withVersion(options));\n",
		fun.Token, typeParameter, argsParamRef)
	_, _ = fmt.Fprintf(w, "    }\n")

	// Emit the args and result types, if any.
	if fun.Inputs != nil {
		_, _ = fmt.Fprintf(w, "\n")

		args := &plainType{
			mod:                   mod,
			name:                  "Args",
			baseClass:             "io.pulumi.resources.InvokeArgs",
			propertyTypeQualifier: "inputs",
			properties:            fun.Inputs.Properties,
		}
		if err := args.genInputType(w, 1); err != nil {
			return err
		}
	}
	if fun.Outputs != nil {
		_, _ = fmt.Fprintf(w, "\n")

		res := &plainType{
			mod:                   mod,
			name:                  "Result",
			propertyTypeQualifier: "outputs",
			properties:            fun.Outputs.Properties,
		}
		res.genOutputType(w, 1)
	}

	// Close the class.
	_, _ = fmt.Fprintf(w, "}\n")

	return nil
}

func (mod *modContext) genEnums(w io.Writer, enums []*schema.EnumType) error {
	// TODO

	return nil
}

// TODO

func visitObjectTypes(properties []*schema.Property, visitor func(*schema.ObjectType, bool)) {
	codegen.VisitTypeClosure(properties, func(t codegen.Type) {
		if o, ok := t.Type.(*schema.ObjectType); ok {
			visitor(o, t.Plain)
		}
	})
}

func (mod *modContext) genType(w io.Writer, obj *schema.ObjectType, propertyTypeQualifier string, input, state, args bool, level int) error {
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
		return pt.genInputType(w, level)
	}

	pt.genOutputType(w, level)
	return nil
}

// pulumiImports is a slice of common imports that are used with the genHeader method.
var pulumiImports = []string{
	"javax.annotation.Nullable",
	"java.util.Optional",
	"java.util.Map",
	"java.util.List",
	"com.google.common.collect.ImmutableMap",  // FIXME: do we really want to expose this dep
	"com.google.common.collect.ImmutableList", // FIXME: do we really want to expose this dep
	"java.util.concurrent.CompletableFuture",
	"io.pulumi.core.*",
	"io.pulumi.core.internal.annotations.*",
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
		// If it's an external resource, we'll be using fully-qualified type names, so there's no need
		// for an import.
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

func (mod *modContext) genHeader(w io.Writer, imports []string) {
	_, _ = fmt.Fprintf(w, "// *** WARNING: this file was generated by %v. ***\n", mod.tool)
	_, _ = fmt.Fprintf(w, "// *** Do not edit by hand unless you're certain you know what you are doing! ***\n")
	_, _ = fmt.Fprintf(w, "\n")
	_, _ = fmt.Fprintf(w, "package %s;\n", mod.packageName)
	_, _ = fmt.Fprintf(w, "\n")

	for _, i := range imports {
		_, _ = fmt.Fprintf(w, "import %s;\n", i)
	}
	if len(imports) > 0 {
		_, _ = fmt.Fprintf(w, "\n")
	}
}

// TODO

func (mod *modContext) genConfig(variables []*schema.Property) (string, error) {
	w := &bytes.Buffer{}

	// TODO

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

	// Ensure that the target module directory contains a README.md file.
	readme := mod.pkg.Description
	if readme != "" && readme[len(readme)-1] != '\n' {
		readme += "\n"
	}
	fs.add("README.md", []byte(readme))

	// Utilities, config
	switch mod.mod {
	case "":
		utilities, err := mod.genUtilities()
		if err != nil {
			return err
		}
		fs.add(filepath.Join(dir, "Utilities.java"), []byte(utilities))
	case "config":
		if len(mod.pkg.Config) > 0 {
			config, err := mod.genConfig(mod.pkg.Config)
			if err != nil {
				return err
			}
			addFile(filepath.Join(dir, "Config.java"), config)
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
		importStrings := pulumiImports
		importStrings = append(importStrings, additionalImports...)
		mod.genHeader(buffer, importStrings)

		if err := mod.genResource(buffer, r); err != nil {
			return err
		}

		addFile(resourceName(r)+".java", buffer.String())
	}

	// Functions
	for _, f := range mod.functions {
		imports := map[string]codegen.StringSet{}
		mod.getImports(f, imports)

		buffer := &bytes.Buffer{}
		importStrings := pulumiImports
		for _, i := range imports {
			importStrings = append(importStrings, i.SortedValues()...)
		}
		mod.genHeader(buffer, importStrings)

		if err := mod.genFunction(buffer, f); err != nil {
			return err
		}

		addFile(tokenToName(f.Token)+".java", buffer.String())
	}

	// Nested types
	for _, t := range mod.types {
		if mod.details(t).inputType || mod.details(t).stateType {
			buffer := &bytes.Buffer{}
			mod.genHeader(buffer, pulumiImports)

			_, _ = fmt.Fprintf(buffer, "public final class Inputs {\n")

			if mod.details(t).inputType {
				if mod.details(t).argsType {
					if err := mod.genType(buffer, t, "Inputs", true, false, true, 1); err != nil {
						return err
					}
				}
				if mod.details(t).plainType {
					if err := mod.genType(buffer, t, "Inputs", true, false, false, 1); err != nil {
						return err
					}
				}
			}

			if mod.details(t).stateType {
				if err := mod.genType(buffer, t, "Inputs", true, true, true, 1); err != nil {
					return err
				}
			}

			_, _ = fmt.Fprintf(buffer, "}\n")
			addFile(path.Join("Inputs.java"), buffer.String())
		}
		if mod.details(t).outputType {
			buffer := &bytes.Buffer{}
			mod.genHeader(buffer, pulumiImports)

			_, _ = fmt.Fprintf(buffer, "public final class Outputs {\n")
			if err := mod.genType(buffer, t, "Outputs", false, false, false, 1); err != nil {
				return err
			}
			_, _ = fmt.Fprintf(buffer, "}\n")

			// TODO: C# has a k8s compat mode here

			addFile(path.Join("Outputs.java"), buffer.String())
		}
	}

	// Enums
	if len(mod.enums) > 0 {
		buffer := &bytes.Buffer{}
		mod.genHeader(buffer, pulumiImports)

		if err := mod.genEnums(buffer, mod.enums); err != nil {
			return err
		}

		addFile("Enums.java", buffer.String())
	}

	return nil
}

// TODO

func computePropertyNames(props []*schema.Property, names map[*schema.Property]string) {
	for _, p := range props {
		if info, ok := p.Language["jvm"].(JVMPropertyInfo); ok && info.Name != "" {
			names[p] = info.Name
		}
	}
}

// TODO

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
			pkgName := basePackage + packageName(info.Packages, pkg.Name)
			if modName != "" {
				pkgName += "." + packageName(info.Packages, modName)
			}
			mod = &modContext{
				pkg:                    p,
				mod:                    modName,
				tool:                   tool,
				packageName:            pkgName,
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
		cfg.packageName = basePackage + packageName(infos[pkg].Packages, pkg.Name)
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

// genGradleProject generates gradle files
func genGradleProject(pkg *schema.Package, packageName string, packageReferences map[string]string, files fs) error {
	genSettingsFile, err := genSettingsFile(pkg, packageName, packageReferences)
	if err != nil {
		return err
	}
	files.add("settings.gradle", genSettingsFile)
	genBuildFile, err := genBuildFile(pkg, packageName, packageReferences)
	if err != nil {
		return err
	}
	files.add("build.gradle", genBuildFile)
	return nil
}

// genSettingsFile emits settings.gradle
func genSettingsFile(pkg *schema.Package, packageName string, packageReferences map[string]string) ([]byte, error) {
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
func genBuildFile(pkg *schema.Package, packageName string, packageReferences map[string]string) ([]byte, error) {
	w := &bytes.Buffer{}
	err := jvmBuildTemplate.Execute(w, jvmBuildTemplateContext{})
	if err != nil {
		return nil, err
	}
	return w.Bytes(), nil
}

// TODO

func GeneratePackage(tool string, pkg *schema.Package, extraFiles map[string][]byte) (map[string][]byte, error) {
	modules, info, err := generateModuleContextMap(tool, pkg)
	if err != nil {
		return nil, err
	}

	className := basePackage + packageName(info.Packages, pkg.Name)

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
	if err := genGradleProject(pkg, className, info.PackageReferences, files); err != nil {
		return nil, err
	}
	return files, nil
}
