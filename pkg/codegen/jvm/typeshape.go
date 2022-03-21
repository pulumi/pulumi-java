// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"fmt"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/codegen/jvm/names"
)

type TypeShape struct {
	Type        names.FQN
	Parameters  []TypeShape
	Annotations []string
}

type TypeShapeStringOptions struct {
	// useful for type parameters because (most) annotations are not allowed on a generic parameter type
	CommentOutAnnotations bool
	// useful for annotation parameters and other contexts where comments or annotations are not allowed
	SkipAnnotations bool
	// useful for .class or [].class to skip the generic part of the type
	GenericErasure bool
	// useful to append .class
	AppendClassLiteral bool
	// useful to append [].class
	AppendClassArrayLiteral bool
}

func (ts TypeShape) Equal(other TypeShape) bool {
	if !ts.Type.Equal(other.Type) {
		return false
	}
	if len(ts.Parameters) != len(other.Parameters) {
		return false
	}
	for i := range ts.Parameters {
		if !ts.Parameters[i].Equal(other.Parameters[i]) {
			return false
		}
	}
	if len(ts.Annotations) != len(other.Annotations) {
		return false
	}
	for i := range ts.Annotations {
		if ts.Annotations[i] != other.Annotations[i] {
			return false
		}
	}
	return true
}

// Converts to Java code, may add imports to use short names.
func (ts TypeShape) ToCode(imports *names.Imports) string {
	return ts.ToCodeWithOptions(imports, TypeShapeStringOptions{})
}

func (ts TypeShape) ToCodeWithOptions(imports *names.Imports, opts TypeShapeStringOptions) string {
	var annotationsString string
	if !opts.SkipAnnotations {
		annotationsString = strings.Join(ts.Annotations, " ")
		if len(ts.Annotations) > 0 {
			if opts.CommentOutAnnotations {
				annotationsString = "/* " + annotationsString + " */"
			}
			annotationsString = annotationsString + " "
		}
	}

	var parametersString string
	if !opts.GenericErasure && len(ts.Parameters) > 0 {
		parametersStrings := make([]string, len(ts.Parameters))
		for i, parameter := range ts.Parameters {
			parameterString := parameter.ToCodeWithOptions(imports, TypeShapeStringOptions{CommentOutAnnotations: true})
			if len(parameterString) > 0 {
				parametersStrings[i] = parameterString
			}
		}
		if len(parametersStrings) > 0 {
			parametersString = "<" + strings.Join(parametersStrings, ",") + ">"
		}
	}

	var classLiteral string
	if opts.AppendClassLiteral {
		classLiteral = ".class"
	}
	if opts.AppendClassArrayLiteral {
		classLiteral = "[].class"
	}

	return fmt.Sprintf("%s%s%s%s", annotationsString, imports.Ref(ts.Type), parametersString, classLiteral)
}

func (ts TypeShape) ParameterTypes(imports *names.Imports) []string {
	return ts.ParameterTypesTransformed(func(ts TypeShape) string {
		return imports.Ref(ts.Type)
	})
}

func (ts TypeShape) ParameterTypesTransformed(f func(TypeShape) string) []string {
	parameterTypes := make([]string, len(ts.Parameters))
	for i, v := range ts.Parameters {
		parameterTypes[i] = f(v)
	}
	return parameterTypes
}

func (ts TypeShape) ListType(ctx *classFileContext) string {
	if ts.Type.String() == names.List.String() {
		for _, a := range ts.Parameters {
			if strings.HasPrefix(a.Type.String(), names.JavaUtil.Dot("").String()) {
				continue
			}
			return a.ToCode(ctx.imports)
		}
	}
	return ""
}

func (ts TypeShape) StringJavaTypeShape(imports *names.Imports) string {
	shape := fmt.Sprintf("%s.<%s>builder(%s)",
		imports.Ref(names.TypeShape),
		ts.ToCodeWithOptions(imports, TypeShapeStringOptions{
			CommentOutAnnotations: true,
			GenericErasure:        false,
		}),
		ts.ToCodeWithOptions(imports, TypeShapeStringOptions{
			CommentOutAnnotations: true,
			GenericErasure:        true,
			AppendClassLiteral:    true,
		}),
	)

	for _, parameter := range ts.Parameters {
		if len(parameter.Parameters) > 0 {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.StringJavaTypeShape(imports))
		} else {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.ToCodeWithOptions(imports, TypeShapeStringOptions{
				CommentOutAnnotations: true,
				GenericErasure:        true,
				AppendClassLiteral:    true,
			}))
		}
	}
	shape += ".build()"
	return shape
}
