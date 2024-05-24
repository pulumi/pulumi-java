package codebase

import (
	"fmt"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func (g *generator) generateInvokeFunctionCallExpression(
	e *model.FunctionCallExpression,
) Expression {
	if len(e.Args) < 1 {
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  "Invoke function requires at least one argument",
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}

	fn := e.Args[0]

	fnSym := g.functionSymbol(fn)
	fnSchema, ok := g.getFunctionSchema(fn)

	if !ok {
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagWarning,
			Summary:  fmt.Sprintf("Could not find function schema for %v", fn),
			Subject:  fn.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}

	args := []Expression{}

	switch ea := e.Args[1].(type) {
	case *model.ObjectConsExpression:
		if len(ea.Items) > 0 {
			args = append(args, g.generateTypedObjectConsExpression(ea, fnSchema.Inputs, true))
		}
	}

	result := fnSym.AsExpression().Call(args...)
	return result
}

func (g *generator) isFunctionInvocation(v *pcl.LocalVariable) (*schema.Function, bool) {
	switch v := v.Definition.Value.(type) {
	case *model.FunctionCallExpression:
		if v.Name != pcl.Invoke || len(v.Args) == 0 {
			return nil, false
		}

		return g.getFunctionSchema(v.Args[0])
	}

	return nil, false
}

func (g *generator) getFunctionSchema(e model.Expression) (*schema.Function, bool) {
	token, ok := expressionString(e)
	if !ok {
		return nil, false
	}

	for _, pkg := range g.program.PackageReferences() {
		fn, ok, err := pcl.LookupFunction(pkg, token)
		if !ok {
			continue
		}

		if err != nil {
			g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
				Severity: hcl.DiagWarning,
				Summary:  fmt.Sprintf("Error occurred while looking up schema for function '%s'", token),
				Detail:   err.Error(),
				Subject:  e.SyntaxNode().Range().Ptr(),
			})

			return nil, false
		}

		return fn, true
	}

	return nil, false
}

func (g *generator) generateToJSONFunctionCallExpression(
	e *model.FunctionCallExpression,
) Expression {
	args := []Expression{}
	for _, arg := range e.Args {
		args = append(args, g.generateJSONExpression(arg))
	}

	result := g.compilationUnit.ImportStatic(JPulumiSerialization, "serializeJson").AsExpression().Call(args...)
	return result
}

func (g *generator) generateJSONExpression(e model.Expression) Expression {
	switch e := e.(type) {
	case *model.ObjectConsExpression:
		args := []Expression{}
		for _, item := range e.Items {
			args = append(
				args,
				g.compilationUnit.ImportStatic(JPulumiSerialization, "jsonProperty").AsExpression().Call(
					g.generateExpression(item.Key),
					g.generateJSONExpression(item.Value),
				),
			)
		}

		result := g.compilationUnit.ImportStatic(JPulumiSerialization, "jsonObject").AsExpression().Call(args...)
		return result
	case *model.TupleConsExpression:
		args := []Expression{}
		for _, item := range e.Expressions {
			args = append(args, g.generateJSONExpression(item))
		}

		result := g.compilationUnit.ImportStatic(JPulumiSerialization, "jsonArray").AsExpression().Call(args...)
		return result
	default:
		return g.generateExpression(e)
	}
}
