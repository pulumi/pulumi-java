package codebase

import (
	"fmt"

	"github.com/hashicorp/hcl/v2"
	"github.com/hashicorp/hcl/v2/hclsyntax"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/zclconf/go-cty/cty"
)

func quoted(es []Expression) []Expression {
	result := make([]Expression, len(es))
	for i, e := range es {
		result[i] = QuoteE(e)
	}
	return result
}

func (g *generator) generateResourceArgumentExpressions(
	t schema.Type,
	e model.Expression,
) []Expression {
	switch e := e.(type) {
	case *model.BinaryOpExpression:
		return []Expression{g.generateBinaryOpExpression(e)}
	case *model.FunctionCallExpression:
		return []Expression{g.generateFunctionCallExpression(e)}
	case *model.LiteralValueExpression:
		return []Expression{g.generateLiteralValueExpression(e)}
	case *model.ObjectConsExpression:
		switch t := t.(type) {
		case *schema.InputType:
			return g.generateResourceArgumentExpressions(t.ElementType, e)
		case *schema.MapType:
			return []Expression{g.generateMapObjectConsExpression(e)}
		case *schema.ObjectType:
			return []Expression{g.generateTypedObjectConsExpression(e, t, t.IsInputShape())}
		case *schema.OptionalType:
			return g.generateResourceArgumentExpressions(t.ElementType, e)
		default:
			g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
				Severity: hcl.DiagError,
				Summary:  fmt.Sprintf("Resource argument object expression provided for a property with unexpected type %T (%v)", t, e),
				Subject:  e.SyntaxNode().Range().Ptr(),
			})

			return []Expression{NullE}
		}
	case *model.RelativeTraversalExpression:
		return []Expression{g.generateRelativeTraversalExpression(e)}
	case *model.ScopeTraversalExpression:
		return []Expression{g.generateScopeTraversalExpression(e)}
	case *model.TemplateExpression:
		return []Expression{g.generateTemplateExpression(e)}
	case *model.TupleConsExpression:
		switch t := t.(type) {
		case *schema.ArrayType:
			es := []Expression{}
			for _, e := range e.Expressions {
				es = append(
					es,
					g.generateResourceArgumentExpressions(unwrapType(t.ElementType), e)...,
				)
			}
			return es
		default:
			return g.generateExpressions(e.Expressions...)
		}
	case *model.UnaryOpExpression:
		return []Expression{g.generateUnaryOpExpression(e)}
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected resource argument expression of type %T (%v)", e, e),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return []Expression{NullE}
	}
}

func (g *generator) generateResourceOptionExpressions(e model.Expression) []Expression {
	switch e := e.(type) {
	case *model.LiteralValueExpression:
		return []Expression{g.generateLiteralValueExpression(e)}
	case *model.ScopeTraversalExpression:
		return []Expression{g.generateScopeTraversalExpression(e)}
	case *model.TemplateExpression:
		return []Expression{g.generateTemplateExpression(e)}
	case *model.TupleConsExpression:
		return g.generateExpressions(e.Expressions...)
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected resource option expression of type %T (%v)", e, e),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return nil
	}
}

func (g *generator) generateExpressions(es ...model.Expression) []Expression {
	result := make([]Expression, len(es))
	for i, e := range es {
		result[i] = g.generateExpression(e)
	}
	return result
}

func (g *generator) generateExpression(e model.Expression) Expression {
	switch e := e.(type) {
	case *model.AnonymousFunctionExpression:
		return g.generateAnonymousFunctionExpression(e)
	case *model.BinaryOpExpression:
		return g.generateBinaryOpExpression(e)
	case *model.FunctionCallExpression:
		return g.generateFunctionCallExpression(e)
	case *model.IndexExpression:
		return g.generateIndexExpression(e)
	case *model.LiteralValueExpression:
		return g.generateLiteralValueExpression(e)
	case *model.ObjectConsExpression:
		return g.generateMapObjectConsExpression(e)
	case *model.RelativeTraversalExpression:
		return g.generateRelativeTraversalExpression(e)
	case *model.ScopeTraversalExpression:
		return g.generateScopeTraversalExpression(e)
	case *model.SplatExpression:
		return g.generateSplatExpression(e)
	case *model.TemplateExpression:
		return g.generateTemplateExpression(e)
	case *model.TupleConsExpression:
		return g.generateTupleConsExpression(e)
	case *model.UnaryOpExpression:
		return g.generateUnaryOpExpression(e)
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected expression of type %T (%v)", e, e),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}
}

func (g *generator) generateAnonymousFunctionExpression(e *model.AnonymousFunctionExpression) Expression {
	args := []LambdaArgument{}
	for _, p := range e.Signature.Parameters {
		args = append(args, LambdaArgument{Name: p.Name})
	}

	return LambdaEE(args, g.generateExpression(e.Body))
}

func (g *generator) generateBinaryOpExpression(e *model.BinaryOpExpression) Expression {
	switch e.Operation {
	case hclsyntax.OpLogicalAnd:
		return AndE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpLogicalOr:
		return OrE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpAdd:
		return AddE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpSubtract:
		return SubtractE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpMultiply:
		return MultiplyE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpDivide:
		return DivideE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpModulo:
		return ModuloE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpEqual:
		return EqualE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpNotEqual:
		return NotEqualE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpLessThan:
		return LessThanE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpLessThanOrEqual:
		return LessThanOrEqualE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpGreaterThan:
		return GreaterThanE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	case hclsyntax.OpGreaterThanOrEqual:
		return GreaterThanOrEqualE(g.generateExpression(e.LeftOperand), g.generateExpression(e.RightOperand))
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected binary operation %v", e.Operation),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}
}

func (g *generator) generateFunctionCallExpression(e *model.FunctionCallExpression) Expression {
	switch e.Name {
	case pcl.IntrinsicApply:
		args, then := pcl.ParseApplyCall(e)
		if len(args) == 1 {
			return g.generateExpression(args[0]).Method("applyValue", g.generateExpression(then))
		} else {
			return g.compilationUnit.Import(JPulumiOutput).AsExpression().
				Method("tuple", g.generateExpressions(args...)...).
				Method("applyValue", g.generateExpression(then))
		}
	case "fileAsset":
		return NewE(
			g.compilationUnit.Import(JPulumiFileAsset).AsType(),
			g.compilationUnit.Import(JPaths).AsExpression().
				Method("get", g.generateExpressions(e.Args...)...),
		)
	case pcl.IntrinsicConvert:
		return g.generateExpression(e.Args[0])
	case pcl.Invoke:
		return g.generateInvokeFunctionCallExpression(e)
	case "join":
		return g.compilationUnit.Import(JString).AsExpression().
			Method("join", g.generateExpressions(e.Args...)...)
	case "length":
		return g.generateExpression(e.Args[0]).Method("length")
	case "mimeType":
		return g.compilationUnit.Import(JFiles).AsExpression().
			Method("probeContentType", g.generateExpressions(e.Args...)...)
	case "project":
		return g.compilationUnit.Import(JPulumiDeployment).AsExpression().
			Method("getInstance").Method("getProjectName")
	case "readDir":
		return g.compilationUnit.Import(JPulumiFiles).AsExpression().
			Method("readDir", g.generateExpressions(e.Args...)...)
	case "readFile":
		return g.compilationUnit.Import(JFiles).AsExpression().Method(
			"readString",
			g.compilationUnit.Import(JPaths).AsExpression().
				Method("get", g.generateExpressions(e.Args...)...),
		)
	case "secret":
		return g.compilationUnit.Import(JPulumiOutput).AsExpression().
			Method("ofSecret", g.generateExpressions(e.Args...)...)
	case "split":
		return g.generateExpression(e.Args[1]).Method("split", g.generateExpression(e.Args[0]))
	case "stack":
		return g.compilationUnit.Import(JPulumiDeployment).AsExpression().
			Method("getInstance").Method("getStackName")
	case "toJSON":
		return g.generateToJSONFunctionCallExpression(e)
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected function call %v", e.Name),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}
}

func (g *generator) generateIndexExpression(e *model.IndexExpression) Expression {
	target := g.generateExpression(e.Collection)
	key := g.generateExpression(e.Key)
	return target.Method("get", key)
}

func (g *generator) generateLiteralValueExpression(e *model.LiteralValueExpression) Expression {
	t := e.Type()
	if cns, ok := t.(*model.ConstType); ok {
		t = cns.Type
	}

	switch t {
	case model.BoolType:
		if e.Value.True() {
			return TrueE
		}
		return FalseE

	case model.NumberType:
		return NumberE(e.Value.AsBigFloat())
	case model.StringType:
		return StringE(e.Value.AsString())
	case model.NoneType:
		return NullE
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected literal type %v", e.Type()),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}
}

func (g *generator) generateMapObjectConsExpression(e *model.ObjectConsExpression) Expression {
	args := make([]Expression, len(e.Items)*2)
	for i, item := range e.Items {
		args[i*2] = g.generateExpression(item.Key)
		args[i*2+1] = g.generateExpression(item.Value)
	}

	result := g.compilationUnit.Import(JMap).AsExpression().Method("of", args...)
	return result
}

func (g *generator) generateTypedObjectConsExpression(
	e *model.ObjectConsExpression,
	t *schema.ObjectType,
	isInputShape bool,
) Expression {
	sym := g.objectTypeSymbol(t, isInputShape)
	result := sym.AsExpression().Method("builder")
	for _, item := range e.Items {
		key := item.Key.(*model.LiteralValueExpression).Value.AsString()
		keyIdent := LowerCamelCase(MakeValidIdentifier(key))

		var pt schema.Type
		for _, p := range t.Properties {
			if p.Name == key {
				pt = p.Type
			}
		}

		result = result.Method(
			keyIdent,
			g.generateResourceArgumentExpressions(pt, item.Value)...,
		)
	}

	result = result.Method("build")
	return result
}

func (g *generator) generateRelativeTraversalExpression(
	e *model.RelativeTraversalExpression,
) Expression {
	result := g.generateExpression(e.Source)

	for _, part := range e.Traversal {
		switch part := part.(type) {
		case hcl.TraverseAttr:
			result = result.Method(part.Name)
		case hcl.TraverseIndex:
			index, _ := part.Key.AsBigFloat().Int64()
			result = result.Method("get", literalE(fmt.Sprint(index)))
		default:
			contract.Failf("unexpected relative traversal part of type %T (%v)", part, part.SourceRange())
		}
	}

	return result
}

func (g *generator) generateScopeTraversalExpression(
	e *model.ScopeTraversalExpression,
) Expression {
	traversed := func(x Expression) Expression {
		for _, part := range e.Traversal.SimpleSplit().Rel {
			switch part := part.(type) {
			case hcl.TraverseAttr:
				x = x.Method(part.Name)
			case hcl.TraverseIndex:
				switch part.Key.Type() {
				case cty.String:
					index := part.Key.AsString()
					x = x.Method(index)
				case cty.Number:
					index, _ := part.Key.AsBigFloat().Int64()
					x = x.Method("get", IntE(index))
				}
			default:
				g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
					Severity: hcl.DiagError,
					Summary:  fmt.Sprintf("Unexpected scope traversal part of type %T (%v)", part, part.SourceRange()),
					Subject:  part.SourceRange().Ptr(),
				})
			}
		}

		return x
	}

	if s, isInvoke := g.referencesFunctionInvocation(e); isInvoke {
		vSym := NewSymbol("", "v")
		return NewSymbol("", s).AsExpression().Method(
			"applyValue",
			LambdaEE(
				[]LambdaArgument{
					{
						Name: vSym.Name(),
					},
				},
				traversed(vSym.AsExpression()),
			),
		)
	} else {
		return traversed(NewSymbol("", e.RootName).AsExpression())
	}
}

func (g *generator) generateSplatExpression(e *model.SplatExpression) Expression {
	return g.generateExpression(e.Source).
		Method("stream").
		Method(
			"map",
		).
		Method(
			"collect",
		)
}

func (g *generator) generateTemplateExpression(e *model.TemplateExpression) Expression {
	if len(e.Parts) == 1 {
		return g.generateExpression(e.Parts[0])
	}

	formatString := ""
	args := []Expression{NullE}
	for _, part := range e.Parts {
		if s, ok := part.(*model.LiteralValueExpression); ok && model.StringType.AssignableFrom(s.Type()) {
			formatString += s.Value.AsString()
		} else {
			formatString += "%s"
			args = append(args, g.generateExpression(part))
		}
	}

	args[0] = StringE(formatString)
	result := g.compilationUnit.Import(JString).AsExpression().Method("format", args...)
	return result
}

func (g *generator) generateTupleConsExpression(e *model.TupleConsExpression) Expression {
	args := make([]Expression, len(e.Expressions))
	for i, item := range e.Expressions {
		args[i] = g.generateExpression(item)
	}

	result := g.compilationUnit.Import(JList).AsExpression().Method("of", args...)
	return result
}

func (g *generator) generateUnaryOpExpression(e *model.UnaryOpExpression) Expression {
	switch e.Operation {
	case hclsyntax.OpNegate:
		return NegateE(g.generateExpression(e.Operand))
	case hclsyntax.OpLogicalNot:
		return NotE(g.generateExpression(e.Operand))
	default:
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Unexpected unary operation %v", e.Operation),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})

		return NullE
	}
}

func (g *generator) applyPclRewrites(e model.Expression, t model.Type) model.Expression {
	e = pcl.RewritePropertyReferences(e)
	e, diagnostics := pcl.RewriteApplies(e, nameInfo(0), false /*applyPromises*/)
	g.diagnostics = append(g.diagnostics, diagnostics...)
	e, diagnostics = pcl.RewriteConversions(e, t)
	g.diagnostics = append(g.diagnostics, diagnostics...)
	return e
}

type nameInfo int

func (nameInfo) Format(name string) string {
	return MakeValidIdentifier(name)
}
