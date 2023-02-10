// Copyright 2022, Pulumi Corporation.  All rights reserved.
//
// Default value and config value code-generation facilities for SDK
// generation are kept in this separate file as they are a bit
// intricate.
package java

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/pulumi/pulumi/pkg/v3/codegen"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// TODO would this file be a convenient place to handle
// DefaultValue.ConstValue also?

// Helper type for config getter and default value generation. Use
// configExpr and defaultValueExpr, the rest are helper methods.
type defaultsGen struct {
	mod *modContext
	ctx *classFileContext
}

// Generates code for an expression to compute the value of config
// property. The expression assumes an environment where config is
// bound to a Config instance. The type of the generated expression
// must match targetType.
func (dg *defaultsGen) configExpr(
	propContext string,
	configProp *schema.Property,
	targetType TypeShape,
) (string, error) {
	return dg.builderExpr(propContext, configProp, targetType, "config", "")
}

// Generates an expression that evaluates to the default value of the
// desired type. The expression takes care of env var lookups and
// hydrating the expected targetType. Optional arg, if given, points
// to a user-provided value that takes priority over defaults.
func (dg *defaultsGen) defaultValueExpr(
	propContext string,
	prop *schema.Property,
	targetType TypeShape,
	arg string,
) (string, error) {
	return dg.builderExpr(propContext, prop, targetType, "", arg)
}

// Utility to generates a builder expr against the
// Codegen.PropertyBuilder API.
//
// config if non-empty is an expr of type Config.
//
// arg if non-empty is an expr of type targetType pointing to a
// user-provided nullable argument.
//
// propContext is free text only used in WARN statements to
// contextualize the property.
func (dg *defaultsGen) builderExpr(
	propContext string,
	prop *schema.Property,
	targetType TypeShape,
	config string,
	arg string,
) (string, error) {
	if code, ok := dg.builderExprSpecialCase(prop, targetType, config, arg); ok {
		return code, nil
	}

	isOutput, t0 := targetType.UnOutput()

	var builderTransformCode string
	if isOutput {
		if prop.Secret {
			builderTransformCode = ".secret()"
		} else {
			builderTransformCode = ".output()"
		}
	} else {
		// TODO[pulumi/pulumi#9744]: Codegen for secret
		// properties forgets the secret flag
		if prop.Secret {
			fmt.Printf("WARN: secret property %s (%s) does not have an Output type\n",
				prop.Name, propContext)
		}
		builderTransformCode = ""
	}

	isOptional, t := t0.UnOptional()

	var code string

	// For Either<A,B> try to codegen A or B opportunistically.
	if isEither, a, b := t.UnEither(); isEither {
		aV, err := dg.builderExprWithSimpleType(prop, a, config, arg,
			fmt.Sprintf(".left(%s)%s",
				b.ToCodeClassLiteral(dg.ctx.imports),
				builderTransformCode))
		if err != nil {
			bV, err2 := dg.builderExprWithSimpleType(prop, b, config, arg,
				fmt.Sprintf(".right(%s)%s",
					a.ToCodeClassLiteral(dg.ctx.imports),
					builderTransformCode))
			if err2 != nil {
				return "", fmt.Errorf("Cannot process Either"+
					"\nLeft: %v\nRight: %w", err, err2)
			}
			code = bV
		} else {
			code = aV
		}
	} else {
		c, err := dg.builderExprWithSimpleType(
			prop, t, config, arg, builderTransformCode)
		if err != nil {
			return "", err
		}
		code = c
	}

	if isOptional {
		return fmt.Sprintf("%s.get()", code), nil
	}
	if !prop.IsRequired() {
		return fmt.Sprintf("%s.getNullable()", code), nil
	}

	return fmt.Sprintf("%s.require()", code), nil
}

// Degenerate cases can have simpler code.
func (dg *defaultsGen) builderExprSpecialCase(
	prop *schema.Property,
	targetType TypeShape,
	config string,
	arg string,
) (string, bool) {
	// No defaults, no null check, already have an argument of wanted type, simply pass it on.
	if !prop.IsRequired() && arg != "" && prop.DefaultValue == nil && prop.ConstValue == nil {
		return arg, true
	}

	// Required prop without defaults, all we need to do is a null check with a good exception.
	if prop.IsRequired() && arg != "" && prop.DefaultValue == nil && prop.ConstValue == nil {
		msg := fmt.Sprintf("expected parameter '%s' to be non-null", prop.Name)
		return fmt.Sprintf("%s.requireNonNull(%s, %s)",
			dg.ctx.ref(names.Objects),
			arg,
			dg.quoteJavaStringLiteral(msg)), true
	}

	return "", false
}

// Helper for simple types - not Optional, Either, or Output.
func (dg *defaultsGen) builderExprWithSimpleType(
	prop *schema.Property,
	targetType TypeShape,
	config string,
	arg string,
	builderTransformCode string,
) (string, error) {
	var buf bytes.Buffer

	fmt.Fprint(&buf, dg.ctx.ref(names.Codegen))
	propLiteral := dg.quoteJavaStringLiteral(prop.Name)

	isObject := false
	if targetType.Type.Equal(names.Boolean) {
		fmt.Fprintf(&buf, ".booleanProp(%s)", propLiteral)
	} else if targetType.Type.Equal(names.Integer) {
		fmt.Fprintf(&buf, ".integerProp(%s)", propLiteral)
	} else if targetType.Type.Equal(names.String) {
		fmt.Fprintf(&buf, ".stringProp(%s)", propLiteral)
	} else if targetType.Type.Equal(names.Double) {
		fmt.Fprintf(&buf, ".doubleProp(%s)", propLiteral)
	} else {
		// Need to know TypeShape to deserialize at runtime
		isObject = true
		if len(targetType.Parameters) == 0 {
			fmt.Fprintf(&buf, ".objectProp(%s, %s.class)",
				propLiteral,
				dg.ctx.ref(targetType.Type))
		} else {
			fmt.Fprintf(&buf, ".objectProp(%s, %s)",
				propLiteral,
				targetType.StringJavaTypeShape(dg.ctx.imports))
		}
	}

	fmt.Fprintf(&buf, "%s", builderTransformCode)

	if config != "" {
		fmt.Fprintf(&buf, ".config(%s)", config)
	}

	if arg != "" {
		fmt.Fprintf(&buf, ".arg(%s)", arg)
	}

	if prop.DefaultValue == nil {
		return buf.String(), nil
	}

	dv := prop.DefaultValue

	if len(dv.Environment) > 0 {
		fmt.Fprintf(&buf, ".env(")
		for i, envVarName := range dv.Environment {
			if i != 0 {
				fmt.Fprintf(&buf, ", ")
			}
			fmt.Fprintf(&buf, "%s", dg.quoteJavaStringLiteral(envVarName))
		}
		fmt.Fprintf(&buf, ")")
	}

	if dv.Value != nil {
		// For enums, refer to matching Enum.CASE as a direct default value
		enumTypes := dg.detectEnumTypes(prop)
		if enumType, ok := enumTypes[targetType.Type.String()]; ok {
			code, err := dg.enumReference(dg.ctx, enumType, dv)
			if err != nil {
				return "", err
			}
			fmt.Fprintf(&buf, ".def(%s)", code)
		} else if isObject {
			encoded, err := json.Marshal(dv.Value)
			if err != nil {
				return "", err
			}
			j := dg.quoteJavaStringLiteral(string(encoded))
			fmt.Fprintf(&buf, ".defJson(%s)", j)
		} else {
			primCode, primType, err := primitiveValue(dv.Value)
			if err != nil {
				return "", err
			}
			if !primType.WithoutAnnotations().Equal(targetType.WithoutAnnotations()) {
				return "", fmt.Errorf("Type mismatch: %v vs %v",
					primType, targetType)
			}
			fmt.Fprintf(&buf, ".def(%s)", primCode)
		}
	}

	return buf.String(), nil
}

// Helper to find any Enum types referenced in the type of Property
// and index them by FQN.
func (dg *defaultsGen) detectEnumTypes(prop *schema.Property) map[string]*schema.EnumType {
	// Index all EnumTypes present in schemaType by FQN.
	enumTypes := map[string]*schema.EnumType{}
	codegen.VisitTypeClosure([]*schema.Property{prop}, func(t schema.Type) {
		switch eT := t.(type) {
		case *schema.EnumType:
			fqn := dg.mod.typeStringForEnumType(eT).Type
			enumTypes[fqn.String()] = eT
		}
	})
	return enumTypes
}

// In cases when the default value is a known enum reference, this
// will be set to the code literal, such as
// `io.pulumi.azurenative.insights.enums.WebTestKind.Ping`.
func (dg *defaultsGen) enumReference(
	ctx *classFileContext,
	enumType *schema.EnumType,
	dv *schema.DefaultValue,
) (string, error) {
	enumFQN := dg.mod.typeStringForEnumType(enumType).Type
	enumName := enumFQN.BaseIdent().String()

	for _, e := range enumType.Elements {
		if e.Value != dv.Value {
			continue
		}
		elName := e.Name
		if elName == "" {
			elName = fmt.Sprintf("%v", e.Value)
		}
		safeName, err := names.MakeSafeEnumName(elName, enumName)
		if err != nil {
			return "", err
		}
		return fmt.Sprintf("%s.%s", ctx.ref(enumFQN), safeName), nil
	}
	return "", fmt.Errorf("Bad default value for enum %s: %v",
		enumFQN.String(),
		dv.Value)
}

func (dg *defaultsGen) quoteJavaStringLiteral(s string) string {
	v, _, err := primitiveValue(s)
	if err != nil {
		panic(err)
	}
	return v
}
