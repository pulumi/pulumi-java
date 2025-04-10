// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
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

func (ts TypeShape) WithoutAnnotations() TypeShape {
	return TypeShape{Type: ts.Type, Parameters: ts.Parameters}
}

// ToCode converts to Java code, may add imports to use short names.
func (ts TypeShape) ToCode(imports *names.Imports) string {
	return ts.ToCodeWithOptions(imports, TypeShapeStringOptions{})
}

// ToCodeErased converts to Java code with erased generics, may add imports to use short names.
func (ts TypeShape) ToCodeErased(imports *names.Imports) string {
	return ts.ToCodeWithOptions(imports, TypeShapeStringOptions{
		GenericErasure:  true,
		SkipAnnotations: true,
	})
}

// ToCodeClassLiteral converts to Java class literal, may add imports to use short names.
func (ts TypeShape) ToCodeClassLiteral(imports *names.Imports) string {
	return ts.ToCodeWithOptions(imports, TypeShapeStringOptions{
		GenericErasure:     true,
		SkipAnnotations:    true,
		AppendClassLiteral: true,
	})
}

func (ts TypeShape) ToCodeCommentedAnnotations(imports *names.Imports) string {
	return ts.ToCodeWithOptions(imports, TypeShapeStringOptions{
		CommentOutAnnotations: true,
	})
}

func (ts TypeShape) ToCodeWithOptions(imports *names.Imports, opts TypeShapeStringOptions) string {
	// guard against unexpected output, should not happen outside of tests
	if ts.Type.String() == "" {
		return ""
	}
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

func (ts TypeShape) ToTree(imports *names.Imports) (string, string) {
	refs := []string{}
	refs, tree := ts.toTreeInternal(refs, imports)
	treeJSON, err := json.Marshal(tree)
	if err != nil {
		panic(err)
	}
	return strings.Join(refs, ","), string(treeJSON)
}

func (ts TypeShape) toTreeInternal(refs []string, imports *names.Imports) ([]string, []interface{}) {
	refIndex := func(classLiteral string) (int, bool) {
		return sliceIndex(len(refs), func(i int) bool {
			return refs[i] == classLiteral
		})
	}
	requireRefIndex := func(classLiteral string) int {
		classRef, ok := refIndex(classLiteral)
		if !ok {
			panic(fmt.Errorf(
				"expected a class reference index for a class literal: '%s'", classLiteral,
			))
		}
		return classRef
	}
	rootClassLiteral := ts.ToCodeClassLiteral(imports)
	// guard against unexpected output, should not happen outside of tests
	if rootClassLiteral == "" {
		return []string{}, []interface{}{}
	}
	_, ok := refIndex(rootClassLiteral)
	if !ok {
		refs = append(refs, rootClassLiteral)
	}
	classRef := requireRefIndex(rootClassLiteral)
	tree := []interface{}{classRef}

	for _, param := range ts.Parameters {
		subRefs, subTree := param.toTreeInternal(refs, imports)
		refs = subRefs
		if len(subTree) == 1 {
			tree = append(tree, subTree[0])
		} else {
			tree = append(tree, subTree)
		}
	}

	return refs, tree
}

func sliceIndex(limit int, predicate func(i int) bool) (int, bool) {
	for i := 0; i < limit; i++ {
		if predicate(i) {
			return i, true
		}
	}
	return -1, false
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

// StringJavaTypeShape returns a Java code representation.
func (ts TypeShape) StringJavaTypeShape(imports *names.Imports) string {
	shape := fmt.Sprintf("%s.<%s>builder(%s)",
		imports.Ref(names.TypeShape),
		ts.ToCodeCommentedAnnotations(imports),
		ts.ToCodeClassLiteral(imports),
	)

	for _, parameter := range ts.Parameters {
		if len(parameter.Parameters) > 0 {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.StringJavaTypeShape(imports))
		} else {
			shape += fmt.Sprintf(".addParameter(%s)", parameter.ToCodeClassLiteral(imports))
		}
	}
	shape += ".build()"
	return shape
}

func (ts TypeShape) UnOutput() (bool, TypeShape) {
	if ts.Type.Equal(names.Output) {
		return true, ts.Parameters[0]
	}
	return false, ts
}

func (ts TypeShape) UnList() (bool, TypeShape) {
	if ts.Type.Equal(names.List) {
		return true, ts.Parameters[0]
	}
	return false, ts
}

func (ts TypeShape) Optional() TypeShape {
	return TypeShape{
		Type:       names.Optional,
		Parameters: []TypeShape{ts},
	}
}

func (ts TypeShape) Output() TypeShape {
	return TypeShape{
		Type:       names.Output,
		Parameters: []TypeShape{ts},
	}
}

func (ts TypeShape) UnOptional() (bool, TypeShape) {
	if ts.Type.Equal(names.Optional) {
		return true, ts.Parameters[0]
	}
	return false, ts
}

func (ts TypeShape) UnNullable() (bool, TypeShape) {
	annotations := []string{}
	isNullable := false
	for _, a := range ts.Annotations {
		if strings.Contains(a, "Nullable") {
			isNullable = true
		} else {
			annotations = append(annotations, a)
		}
	}
	if isNullable {
		return true, TypeShape{ts.Type, ts.Parameters, annotations}
	}
	return false, ts
}

func (ts TypeShape) UnEither() (bool, TypeShape, TypeShape) {
	if ts.Type.Equal(names.Either) {
		return true, ts.Parameters[0], ts.Parameters[1]
	}
	return false, TypeShape{}, TypeShape{}
}

func (ts TypeShape) StripWrapperClasses() TypeShape {
	if len(ts.Parameters) == 0 {
		return ts
	}

	if ts.Type.Equal(names.Output) || ts.Type.Equal(names.Optional) || ts.Type.Equal(names.Either) {
		return ts.Parameters[0].StripWrapperClasses()
	}

	strippedParameters := make([]TypeShape, len(ts.Parameters))
	for i, parameter := range ts.Parameters {
		strippedParameters[i] = parameter.StripWrapperClasses()
	}

	return TypeShape{
		Type:        ts.Type,
		Parameters:  strippedParameters,
		Annotations: ts.Annotations,
	}
}
