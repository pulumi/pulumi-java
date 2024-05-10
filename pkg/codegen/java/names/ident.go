package names

import (
	"fmt"
	"regexp"
	"strings"
	"unicode"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
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

// isReservedResourceMethod checks if a method is valid when inherited from Object.
func isReservedMethod(s string) bool {
	switch s {
	// These names conflict with methods on Object.
	case "notify", "notifyAll", "toString", "clone", "equals", "hashCode", "getClass", "wait", "finalize":
		return true
	// These names conflict with methods on our builders.
	case "builder":
		return true
	default:
		return isReservedWord(s)
	}
}

// isReservedResourceMethod checks if a method is valid when inherited from Resource.
func isReservedResourceMethod(s string) bool {
	switch s {
	// These names conflict with methods on Resource.
	case "getResourceType", "getResourceName", "getChildResources", "getUrn", "getId",
		"pulumiResourceType", "pulumiResourceName", "pulumiChildResources", "urn", "id":
		return true
	default:
		return isReservedMethod(s)
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

func MakeSafeEnumName(name, _ string) (string, error) {
	// Replace common single character enum names.
	safeName := codegen.ExpandShortEnumName(name)

	// If the name is one illegal character, return an error.
	if len(safeName) == 1 && !isLegalIdentifierStart(rune(safeName[0])) {
		return "", fmt.Errorf("enum name %s is not a valid identifier", safeName)
	}

	// Capitalize and make a valid identifier.
	safeName = strings.Title(Ident(safeName).String())

	// If there are multiple underscores in a row, replace with one.
	regex := regexp.MustCompile(`_+`)
	safeName = regex.ReplaceAllString(safeName, "_")

	return safeName, nil
}

// Ident is a valid Java identifier
type Ident string

func (id Ident) String() string {
	return id.makeValid()
}

// makeValidIdentifier replaces characters that are not allowed in Java identifiers with underscores.
// A reserved word is suffixed with _. No attempt is made to ensure that the result is unique.
func (id Ident) makeValid() string {
	name := string(id)
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
		return name + "_"
	}
	return name
}

func (id Ident) FQN() FQN {
	return FQN{[]Ident{}, id}
}

// Property is a valid Java identifier for a property
type Property Ident

func (id Ident) AsProperty() Property {
	return Property(id)
}

// Field returns a name of a valid Java field for this property.
func (id Property) Field() string {
	return Ident(id).String()
}

// Getter returns a name of a Java getter for this property.
func (id Property) Getter() string {
	if isReservedMethod(id.Field()) {
		return id.Field() + "_"
	}
	return id.Field()
}

// ResourceGetter returns a name of a Java Pulumi Resource getter for this property.
func (id Property) ResourceGetter() string {
	name := id.Getter()
	if isReservedResourceMethod(name) {
		return name + "_"
	}
	return name
}

// Setter returns a name of a Java setter for this property.
func (id Property) Setter() string {
	if isReservedMethod(id.Field()) {
		return id.Field() + "_"
	}
	return id.Field()
}

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

// Title converts the input string to a title cased string.
// If `s` has upper case letters, they are kept.
func Title(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	return string(append([]rune{unicode.ToUpper(runes[0])}, runes[1:]...))
}

// LowerCamelCase sets the first character to lowercase
// LowerCamelCase("LowerCamelCase") -> "lowerCamelCase"
func LowerCamelCase(s string) string {
	if s == "" {
		return ""
	}
	runes := []rune(s)
	return string(append([]rune{unicode.ToLower(runes[0])}, runes[1:]...))
}
