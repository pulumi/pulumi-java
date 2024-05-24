package codebase

import (
	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/syntax"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

const (
	pulumiToken    = "pulumi"
	providersToken = "providers"
)

func GenerateProgram(program *pcl.Program) (map[string][]byte, hcl.Diagnostics, error) {
	g := NewGenerator(program)
	return g.GenerateProgram()
}

type generator struct {
	program             *pcl.Program
	compilationUnit     CompilationUnit
	functionInvocations map[string]*schema.Function
	diagnostics         hcl.Diagnostics
}

func NewGenerator(program *pcl.Program) *generator {
	return &generator{
		program:             program,
		compilationUnit:     NewCodebase().Package("generated_program").CompilationUnit("App"),
		functionInvocations: map[string]*schema.Function{},
		diagnostics:         hcl.Diagnostics{},
	}
}

func (g *generator) GenerateProgram() (map[string][]byte, hcl.Diagnostics, error) {
	pcl.MapProvidersAsResources(g.program)
	nodes := pcl.Linearize(g.program)

	ss := []Statement{}
	for _, n := range nodes {
		ss = append(ss, GroupS(Compact, g.generateNode(n)...))
	}

	g.compilationUnit.PublicClass().Method(
		Public,
		[]Modifier{Static},
		VoidT,
		"main",
		[]Argument{
			{
				Type: ArrayT(StringT),
				Name: "args",
			},
		},
		[]Statement{
			ExprS(
				g.compilationUnit.Import(JPulumi).AsExpression().Method(
					"run",
					g.compilationUnit.PublicClass().Symbol().AsExpression().MethodReference("stack"),
				),
			),
		},
	)

	g.compilationUnit.PublicClass().Method(
		Public,
		[]Modifier{Static},
		VoidT,
		"stack",
		[]Argument{
			{
				Type: g.compilationUnit.Import(JPulumiContext).AsType(),
				Name: "ctx",
			},
		},
		[]Statement{
			GroupS(Spaced, ss...),
		},
	)

	files := g.compilationUnit.Instantiate()
	return files, g.diagnostics, nil
}

func (g *generator) generateNode(n pcl.Node) []Statement {
	switch n := n.(type) {
	case *pcl.ConfigVariable:
		return g.generateConfigVariable(n)
	case *pcl.LocalVariable:
		return g.generateLocalVariable(n)
	case *pcl.OutputVariable:
		return g.generateOutputVariable(n)
	case *pcl.Resource:
		return g.generateResource(n)
	default:
		return nil
	}
}

func (g *generator) generateConfigVariable(v *pcl.ConfigVariable) []Statement {
	ctx := NewSymbol("", "ctx")
	e := ctx.AsExpression().Method("config").Method("get", StringE(v.Name()))

	if v.DefaultValue != nil {
		e = e.Method("orElse", g.generateExpression(v.DefaultValue))
	}

	return []Statement{
		VarS(
			[]Modifier{Final},
			LowerCamelCase(MakeValidIdentifier(v.Name())),
			e,
		),
	}
}

func (g *generator) generateLocalVariable(v *pcl.LocalVariable) []Statement {
	ss := generateTriviaComments(v.Definition.Tokens.Name)

	name := v.Name()
	if fnSchema, isInvoke := g.isFunctionInvocation(v); isInvoke {
		g.functionInvocations[name] = fnSchema
	}

	ss = append(
		ss,
		VarS(
			[]Modifier{Final},
			LowerCamelCase(MakeValidIdentifier(name)),
			g.generateExpression(v.Definition.Value),
		),
	)

	return ss
}

func (g *generator) generateOutputVariable(v *pcl.OutputVariable) []Statement {
	ctx := NewSymbol("", "ctx")
	e := g.applyPclRewrites(v.Value, v.Type())

	return []Statement{
		ExprS(
			ctx.AsExpression().Method(
				"export",
				StringE(v.Name()),
				g.generateExpression(e),
			),
		),
	}
}

func (g *generator) generateResource(r *pcl.Resource) []Statement {
	rSym, rArgsSym := g.resourceSymbols(r)

	hasOpts := hasCustomResourceOptions(r)

	var inputs Expression
	if len(r.Inputs) > 0 {
		inputTypes := resourceInputTypes(r)
		inputs = rArgsSym.AsExpression().Method("builder")

		for _, input := range r.Inputs {
			inputs = inputs.Method(
				LowerCamelCase(MakeValidIdentifier(input.Name)),
				g.generateResourceArgumentExpressions(
					inputTypes[input.Name],
					input.Value,
				)...,
			)
		}

		inputs = inputs.Method("build")
	} else if hasOpts {
		inputs = rArgsSym.AsExpression().Property("Empty")
	}

	var options Expression
	if hasOpts {
		options = g.compilationUnit.Import(JPulumiCustomResourceOptions).AsExpression().Method("builder")
		if r.Options.Provider != nil {
			options = options.Method(
				"provider",
				g.generateResourceOptionExpressions(r.Options.Provider)...,
			)
		}

		if r.Options.Protect != nil {
			options = options.Method(
				"protect",
				g.generateResourceOptionExpressions(r.Options.Protect)...,
			)
		}

		if r.Options.RetainOnDelete != nil {
			options = options.Method(
				"retainOnDelete",
				g.generateResourceOptionExpressions(r.Options.RetainOnDelete)...,
			)
		}

		if r.Options.Parent != nil {
			options = options.Method(
				"parent",
				g.generateResourceOptionExpressions(r.Options.Parent)...,
			)
		}

		if r.Options.DependsOn != nil {
			options = options.Method(
				"dependsOn",
				g.generateResourceOptionExpressions(r.Options.DependsOn)...,
			)
		}

		if r.Options.IgnoreChanges != nil {
			options = options.Method(
				"ignoreChanges",
				quoted(g.generateResourceOptionExpressions(r.Options.IgnoreChanges))...,
			)
		}

		options = options.Method("build")
	}

	leadingTokens := []syntax.Token{r.Definition.Tokens.GetType("")}
	leadingTokens = append(leadingTokens, r.Definition.Tokens.GetLabels(nil)...)
	leadingTokens = append(leadingTokens, r.Definition.Tokens.GetOpenBrace())

	ss := generateTriviaComments(leadingTokens...)

	instanceSym := NewSymbol("", LowerCamelCase(MakeValidIdentifier(r.Name())))
	declareInstance := func(makeLogicalName func(name string) Expression) Statement {
		return VarS(
			[]Modifier{Final},
			instanceSym.Name(),
			NewE(
				rSym.AsType(),
				makeLogicalName(instanceSym.Name()),
				inputs,
				options,
			),
		)
	}

	if r.Options != nil && r.Options.Range != nil {
		rangeT := model.ResolveOutputs(r.Options.Range.Type())
		rangeE := g.applyPclRewrites(r.Options.Range, rangeT)

		generateLoopStatements := func(listE Expression, genRangeE Expression) []Statement {
			if isNumericType(rangeT) {
				i := NewSymbol("", "i")

				return []Statement{
					ForS(
						[]Statement{
							VarS(
								[]Modifier{},
								i.Name(),
								IntE(0),
							),
						},
						LessThanE(i.AsExpression(), genRangeE),
						[]Statement{
							ExprS(PostfixIncrementE(i.AsExpression())),
						},
						[]Statement{
							declareInstance(func(name string) Expression {
								return AddE(StringE(name+"-"), i.AsExpression())
							}),
							ExprS(listE.Method("add", instanceSym.AsExpression())),
						},
					),
				}
			}

			// We have to name this "range" to match references that will be in the PCL expression.
			each := NewSymbol("", "range")

			return []Statement{
				ForEachVarS(
					[]Modifier{},
					each.Name(),
					g.compilationUnit.Import(JPulumiKeyedValue).AsExpression().
						Method("of", genRangeE),
					[]Statement{
						declareInstance(func(name string) Expression {
							return AddE(StringE(name+"-"), each.AsExpression().Method("key"))
						}),
						ExprS(listE.Method("add", instanceSym.AsExpression())),
					},
				),
			}
		}

		if ref, isInvoke := g.referencesFunctionInvocation(rangeE); isInvoke {
			// TODO: Comments showing the code we want to generate!
			listSym := NewSymbol("", "resources")
			lambdaStatements := []Statement{
				VarS(
					[]Modifier{Final},
					listSym.Name(),
					NewE(
						g.compilationUnit.Import(JArrayList).AsType().
							Apply(rSym.AsType()),
					),
				),
			}

			lambdaStatements = append(
				lambdaStatements,
				generateLoopStatements(
					listSym.AsExpression(),
					NewSymbol("", ref).AsExpression(),
				)...,
			)

			lambdaStatements = append(lambdaStatements, ReturnS(listSym.AsExpression()))

			ss = append(
				ss,
				VarS(
					[]Modifier{Final},
					instanceSym.Name(),
					g.generateExpression(rangeE).Method(
						"applyValue",
						LambdaSE(
							[]LambdaArgument{
								{
									Name: ref,
								},
							},
							lambdaStatements,
						),
					),
				),
			)
		} else {
			ss = append(
				ss,
				VarS(
					[]Modifier{Final},
					instanceSym.Name(),
					NewE(
						g.compilationUnit.Import(JArrayList).AsType().
							Apply(rSym.AsType()),
					),
				),
			)

			ss = append(
				ss,
				generateLoopStatements(
					instanceSym.AsExpression(),
					g.generateExpression(rangeE),
				)...,
			)
		}
	} else {
		ss = append(ss, declareInstance(func(name string) Expression {
			return StringE(name)
		}))
	}

	ss = append(ss, generateTriviaComments(r.Definition.Tokens.GetCloseBrace())...)
	return ss
}

func generateTriviaComments(tokens ...syntax.Token) []Statement {
	var comments []Statement

	for _, token := range tokens {
		for _, trivia := range token.LeadingTrivia {
			if c, ok := trivia.(syntax.Comment); ok {
				for _, line := range c.Lines {
					comments = append(comments, CommentS(line))
				}
			}
		}

		for _, trivia := range token.TrailingTrivia {
			if c, ok := trivia.(syntax.Comment); ok {
				for _, line := range c.Lines {
					comments = append(comments, CommentS(line))
				}
			}
		}
	}

	return comments
}

func resourceInputTypes(r *pcl.Resource) map[string]schema.Type {
	ps := map[string]schema.Type{}
	if r.Schema != nil && r.Schema.InputProperties != nil {
		for _, p := range r.Schema.InputProperties {
			if p != nil && p.Type != nil {
				ps[p.Name] = unwrapType(p.Type)
			}
		}
	}

	return ps
}

func hasCustomResourceOptions(r *pcl.Resource) bool {
	if r.Options == nil {
		return false
	}

	return r.Options.IgnoreChanges != nil ||
		r.Options.DependsOn != nil ||
		r.Options.Parent != nil ||
		r.Options.Protect != nil ||
		r.Options.RetainOnDelete != nil ||
		r.Options.Provider != nil
}

func expressionString(e model.Expression) (string, bool) {
	if l, ok := e.(*model.LiteralValueExpression); ok {
		if l.Value.IsNull() {
			return "", false
		}

		return l.Value.AsString(), true
	}

	if t, ok := e.(*model.TemplateExpression); ok {
		if len(t.Parts) == 1 {
			return expressionString(t.Parts[0])
		}
	}

	return "", false
}

func isNumericType(t model.Type) bool {
	return model.InputType(model.NumberType).ConversionFrom(t) != model.NoConversion
}

func (g *generator) referencesFunctionInvocation(e model.Expression) (string, bool) {
	switch e := e.(type) {
	case *model.FunctionCallExpression:
		if e.Name == pcl.IntrinsicConvert {
			return g.referencesFunctionInvocation(e.Args[0])
		}
	case *model.ScopeTraversalExpression:
		if _, isInvoke := g.functionInvocations[e.RootName]; isInvoke {
			return e.RootName, true
		}
	}

	return "", false
}
