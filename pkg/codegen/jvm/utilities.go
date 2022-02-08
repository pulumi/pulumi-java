// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
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
