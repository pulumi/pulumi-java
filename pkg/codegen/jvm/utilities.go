// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"fmt"
	"io"
	"strings"
	"unicode"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

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

// isReservedWord returns true if s is a C# reserved word as per
// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure#keywords
func isReservedWord(s string) bool {
	switch s {
	case "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const",
		"continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event", "explicit", "extern",
		"false", "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit", "in", "int", "interface",
		"internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override",
		"params", "private", "protected", "public", "readonly", "ref", "return", "sbyte", "sealed", "short",
		"sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true", "try", "typeof",
		"uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while":
		return true
	// Treat contextual keywords as keywords, as we don't validate the context around them.
	case "add", "alias", "ascending", "async", "await", "by", "descending", "dynamic", "equals", "from", "get",
		"global", "group", "into", "join", "let", "nameof", "on", "orderby", "partial", "remove", "select", "set",
		"unmanaged", "value", "var", "when", "where", "yield":
		return true
	default:
		return false
	}
}

// isLegalIdentifierStart returns true if it is legal for c to be the first character of a C# identifier as per
// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure
func isLegalIdentifierStart(c rune) bool {
	return c == '_' || c == '@' ||
		unicode.In(c, unicode.Lu, unicode.Ll, unicode.Lt, unicode.Lm, unicode.Lo, unicode.Nl)
}

// isLegalIdentifierPart returns true if it is legal for c to be part of a C# identifier (besides the first character)
// as per https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure
func isLegalIdentifierPart(c rune) bool {
	return c == '_' ||
		unicode.In(c, unicode.Lu, unicode.Ll, unicode.Lt, unicode.Lm, unicode.Lo, unicode.Nl, unicode.Mn, unicode.Mc,
			unicode.Nd, unicode.Pc, unicode.Cf)
}

// makeValidIdentifier replaces characters that are not allowed in C# identifiers with underscores. A reserved word is
// prefixed with @. No attempt is made to ensure that the result is unique.
func makeValidIdentifier(name string) string {
	var builder strings.Builder
	for i, c := range name {
		if i == 0 && c == '@' {
			builder.WriteRune(c)
			continue
		}
		if !isLegalIdentifierPart(c) {
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
		return "@" + name
	}
	return name
}

// toUpper converts the input string to a title case
// where only the initial letter is upper-cased.
func toUpperCase(input string) string {
	if input == "" {
		return ""
	}
	runes := []rune(input)
	return string(append([]rune{unicode.ToUpper(runes[0])}, runes[1:]...))
}

// toUpper converts the input string to a title case
// where only the initial letter is upper-cased.
func toLowerCase(input string) string {
	if input == "" {
		return ""
	}
	runes := []rune(input)
	return string(append([]rune{unicode.ToLower(runes[0])}, runes[1:]...))
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

func formatBlockComment(comment string, indent string) string {
	prefix := fmt.Sprintf("%s * ", indent)
	comment = strings.ReplaceAll(comment, "*/", "*{@literal /}")
	comment = codegen.FilterExamples(comment, "java")
	comment = strings.Join(strings.Split(comment, "\n"), "\n"+prefix)
	return prefix + comment
}

func fprintf(w io.Writer, format string, args ...interface{}) {
	_, err := fmt.Fprintf(w, format, args...)
	if err != nil {
		panic("error writing format string [format=" + format + "]")
	}
}
