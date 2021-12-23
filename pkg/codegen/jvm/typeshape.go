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
	CommentOutAnnotations bool // useful for type parameters e.g. (most) annotations are not allowed on a generic parameter type
	GenericErasure        bool // useful for .class to skip the generic part of the type
}

func (ts TypeShape) String() string {
	return ts.StringWithOptions(TypeShapeStringOptions{})
}

func (ts TypeShape) StringWithOptions(opts TypeShapeStringOptions) string {
	annotationsString := strings.Join(ts.Annotations, " ")
	if len(ts.Annotations) > 0 {
		if opts.CommentOutAnnotations {
			annotationsString = "/* " + annotationsString + " */"
		}
		annotationsString = annotationsString + " "
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

	return fmt.Sprintf("%s%s%s", annotationsString, ts.Type, parametersString)
}

func (ts TypeShape) ParameterTypes() []string {
	parameterTypes := make([]string, len(ts.Parameters))
	for i, parameter := range ts.Parameters {
		parameterString := parameter.Type
		parameterTypes[i] = parameterString
	}
	return parameterTypes
}
