package codebase

import (
	"strings"
	"unicode"
)

// MakeValidIdentifier replaces characters that are not allowed in Java identifiers with underscores. A reserved word is
// prefixed with _. No attempt is made to ensure that the result is unique.
func MakeValidIdentifier(name string) string {
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
		return name + "_"
	}

	return name
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

// TitleCase uppercases the first character of an assumed-camel-cased string.
//
// TitleCase("titleCase") -> "TitleCase"
// TitleCase("TitleCase") -> "TitleCase"
func TitleCase(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	return string(append([]rune{unicode.ToUpper(runes[0])}, runes[1:]...))
}

// LowerCamelCase lowercases the first character of an assumed-camel-cased string.
//
// LowerCamelCase("lowerCamelCase") -> "lowerCamelCase"
// LowerCamelCase("LowerCamelCase") -> "lowerCamelCase"
func LowerCamelCase(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	return string(append([]rune{unicode.ToLower(runes[0])}, runes[1:]...))
}
