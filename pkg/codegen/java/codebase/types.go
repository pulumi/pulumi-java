package codebase

import "github.com/pulumi/pulumi/pkg/v3/codegen/schema"

func unwrapType(t schema.Type) schema.Type {
	switch t := t.(type) {
	case *schema.InputType:
		return unwrapType(t.ElementType)
	case *schema.OptionalType:
		return unwrapType(t.ElementType)
	default:
		return t
	}
}
