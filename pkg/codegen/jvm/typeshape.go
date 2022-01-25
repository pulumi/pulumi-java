package jvm

import (
	"fmt"
	"strings"
)

type TypeShape struct {
	Type        string
	Parameters  []TypeShape
	Annotations []string
}

type TypeShapeStringOptions struct {
	CommentOutAnnotations   bool // useful for type parameters because (most) annotations are not allowed on a generic parameter type
	SkipAnnotations         bool // useful for annotation parameters and other contexts where comments or annotations are not allowed
	GenericErasure          bool // useful for .class or [].class to skip the generic part of the type
	AppendClassLiteral      bool // useful to append .class
	AppendClassArrayLiteral bool // useful to append [].class
}

func (ts TypeShape) String() string {
	return ts.StringWithOptions(TypeShapeStringOptions{})
}

func (ts TypeShape) StringWithOptions(opts TypeShapeStringOptions) string {
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
			parameterString := parameter.StringWithOptions(TypeShapeStringOptions{CommentOutAnnotations: true})
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

	return fmt.Sprintf("%s%s%s%s", annotationsString, ts.Type, parametersString, classLiteral)
}

func (ts TypeShape) ParameterTypes() []string {
	return ts.ParameterTypesTransformed(func(ts TypeShape) string {
		return ts.Type
	})
}

func (ts TypeShape) ParameterTypesTransformed(f func(TypeShape) string) []string {
	parameterTypes := make([]string, len(ts.Parameters))
	for i, v := range ts.Parameters {
		parameterTypes[i] = f(v)
	}
	return parameterTypes
}

func (ts TypeShape) StringJavaTypeShape() string {
	var shape string
	shape += fmt.Sprintf("TypeShape.<%s>builder(%s)"+
		ts.StringWithOptions(TypeShapeStringOptions{
			CommentOutAnnotations: true,
			GenericErasure:        true,
		}),
		ts.StringWithOptions(TypeShapeStringOptions{
			CommentOutAnnotations: true,
			GenericErasure:        true,
			AppendClassLiteral:    true,
		}),
	)
	for _, parameter := range ts.Parameters {
		if len(parameter.Parameters) > 0 {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.StringJavaTypeShape())
		} else {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.StringWithOptions(TypeShapeStringOptions{
				CommentOutAnnotations: true,
				GenericErasure:        true,
				AppendClassLiteral:    true,
			}))
		}
	}
	shape += ".build()"
	return shape
}
