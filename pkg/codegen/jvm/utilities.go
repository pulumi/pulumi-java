// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"regexp"
	"strings"
	"unicode"

	"github.com/pkg/errors"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

// isReservedWord returns true if s is a Java reserved word
// as per https://docs.oracle.com/javase/specs/jls/se10/html/jls-3.html#jls-3.9
func isReservedWord(s string) bool {
	switch s {
	case "abstract", "assert", "boolean", "break", "byte",
		"case", "catch", "char", "class", "const",
		"continue", "default", "do", "double", "else",
		"enum", "extends", "final", "finally", "float",
		"for", "if", "goto", "implements", "import",
		"instanceof", "int", "interface", "long", "native",
		"new", "package", "private", "protected", "public",
		"return", "short", "static", "strictfp", "super",
		"switch", "synchronized", "this", "throw", "throws",
		"transient", "try", "void", "volatile", "while",
		"_":
		return true
	// Treat semi-keywords as keywords.
	case "false", "null", "true", "var":
		return true
	default:
		return false
	}
}

// isLegalIdentifierStart returns true if it is legal for c to be the first character of a Java identifier
// as per https://docs.oracle.com/javase/specs/jls/se10/html/jls-3.html#jls-Identifier
func isLegalIdentifierStart(c rune) bool {
	return c == '_' || c == '$' ||
		unicode.In(c, unicode.Letter)
}

// isLegalIdentifierPart returns true if it is legal for c to be part of a Java identifier (besides the first character)
// as per https://docs.oracle.com/javase/specs/jls/se10/html/jls-3.html#jls-Identifier
func isLegalIdentifierPart(c rune) bool {
	return c == '_' || c == '$' ||
		unicode.In(c, unicode.Letter, unicode.Digit)
}

// makeValidIdentifier replaces characters that are not allowed in Java identifiers with underscores.
// A reserved word is prefixed with $. No attempt is made to ensure that the result is unique.
func makeValidIdentifier(name string) string {
	var builder strings.Builder
	for i, c := range name {
		if i == 0 && c == '$' {
			builder.WriteRune(c)
			continue
		}
		if c == '-' { // skip `-` so azure-native becomes azurenative
			continue
		} else if !isLegalIdentifierPart(c) {
			builder.WriteRune('_')
		} else {
			if i == 0 && !isLegalIdentifierStart(c) {
				builder.WriteRune('_')
			}
			builder.WriteRune(c)
		}
	}
	name = builder.String()
	if isReservedWord(name) {
		return "$" + name
	}
	return name
}

// TODO

func makeSafeEnumName(name, typeName string) (string, error) {
	// Replace common single character enum names.
	safeName := codegen.ExpandShortEnumName(name)

	// If the name is one illegal character, return an error.
	if len(safeName) == 1 && !isLegalIdentifierStart(rune(safeName[0])) {
		return "", errors.Errorf("enum name %s is not a valid identifier", safeName)
	}

	// Capitalize and make a valid identifier.
	safeName = strings.Title(makeValidIdentifier(safeName))

	// If there are multiple underscores in a row, replace with one.
	regex := regexp.MustCompile(`_+`)
	safeName = regex.ReplaceAllString(safeName, "_")

	// If the enum name starts with an underscore, add the type name as a prefix.
	if strings.HasPrefix(safeName, "_") {
		safeName = typeName + safeName
	}

	return safeName, nil
}

// The following 3 functions are copied from pkg/codegen utils with one change.
// `visitPlainObjectTypes` does not drill into Input types, while
// `codegen.visitObjectTypes` does.
func visitPlainObjectTypes(properties []*schema.Property, visitor func(*schema.ObjectType)) {
	VisitPlainTypeClosure(properties, func(t schema.Type) {
		if o, ok := t.(*schema.ObjectType); ok {
			visitor(o)
		}
	})
}

func visitPlainTypeClosure(t schema.Type, visitor func(t schema.Type), seen codegen.Set) {
	if seen.Has(t) {
		return
	}
	seen.Add(t)

	visitor(t)

	switch st := t.(type) {
	case *schema.ArrayType:
		visitPlainTypeClosure(st.ElementType, visitor, seen)
	case *schema.MapType:
		visitPlainTypeClosure(st.ElementType, visitor, seen)
	case *schema.ObjectType:
		for _, p := range st.Properties {
			visitPlainTypeClosure(p.Type, visitor, seen)
		}
	case *schema.UnionType:
		for _, e := range st.ElementTypes {
			visitPlainTypeClosure(e, visitor, seen)
		}
	case *schema.OptionalType:
		visitPlainTypeClosure(st.ElementType, visitor, seen)
	}
}

func VisitPlainTypeClosure(properties []*schema.Property, visitor func(t schema.Type)) {
	seen := codegen.Set{}
	for _, p := range properties {
		visitPlainTypeClosure(p.Type, visitor, seen)
	}
}
