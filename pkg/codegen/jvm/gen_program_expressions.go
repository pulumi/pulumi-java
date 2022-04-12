// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"io"
	"math/big"
	"strings"

	"github.com/hashicorp/hcl/v2"
	"github.com/hashicorp/hcl/v2/hclsyntax"
	"github.com/zclconf/go-cty/cty"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
)

type nameInfo int

func (nameInfo) Format(name string) string {
	return makeValidIdentifier(name)
}

// newAwaitCall creates a new call to the await intrinsic.
func newAwaitCall(promise model.Expression) model.Expression {
	// TODO(pdg): unions
	promiseType, ok := promise.Type().(*model.PromiseType)
	if !ok {
		return promise
	}

	return &model.FunctionCallExpression{
		Name: intrinsicAwait,
		Signature: model.StaticFunctionSignature{
			Parameters: []model.Parameter{{
				Name: "promise",
				Type: promiseType,
			}},
			ReturnType: promiseType.ElementType,
		},
		Args: []model.Expression{promise},
	}
}

// lowerExpression amends the expression with intrinsics for C# generation.
func (g *generator) lowerExpression(expr model.Expression, typ model.Type) model.Expression {
	expr = pcl.RewritePropertyReferences(expr)
	expr, diags := pcl.RewriteApplies(expr, nameInfo(0), !g.asyncInit)
	contract.Assert(len(diags) == 0)
	expr = pcl.RewriteConversions(expr, typ)
	if g.asyncInit {
		expr = g.awaitInvokes(expr)
	} else {
		expr = g.outputInvokes(expr)
	}
	return expr
}

// awaitInvokes wraps each call to `invoke` with a call to the `await` intrinsic. This rewrite should only be used
// if we are generating an async Initialize, in which case the apply rewriter should also be configured not to treat
// promises as eventuals. Note that this depends on the fact that invokes are the only way to introduce promises
// in to a Pulumi program; if this changes in the future, this transform will need to be applied in a more general way
// (e.g. by the apply rewriter).
func (g *generator) awaitInvokes(x model.Expression) model.Expression {
	contract.Assert(g.asyncInit)

	rewriter := func(x model.Expression) (model.Expression, hcl.Diagnostics) {
		// Ignore the node if it is not a call to invoke.
		call, ok := x.(*model.FunctionCallExpression)
		if !ok || call.Name != pcl.Invoke {
			return x, nil
		}

		_, isPromise := call.Type().(*model.PromiseType)
		contract.Assert(isPromise)

		return newAwaitCall(call), nil
	}
	x, diags := model.VisitExpression(x, model.IdentityVisitor, rewriter)
	contract.Assert(len(diags) == 0)
	return x
}

// newOutputCall creates a new call to the output intrinsic.
func newOutputCall(promise model.Expression) model.Expression {
	promiseType, ok := promise.Type().(*model.PromiseType)
	if !ok {
		return promise
	}

	return &model.FunctionCallExpression{
		Name: intrinsicOutput,
		Signature: model.StaticFunctionSignature{
			Parameters: []model.Parameter{{
				Name: "promise",
				Type: promiseType,
			}},
			ReturnType: model.NewOutputType(promiseType.ElementType),
		},
		Args: []model.Expression{promise},
	}
}

// outputInvokes wraps each call to `invoke` with a call to the `output` intrinsic. This rewrite should only be used if
// resources are instantiated within a stack constructor, where `await` operator is not available. We want to avoid the
// nastiness of working with raw `Task` and wrap it into Pulumi's Output immediately to be able to `Apply` on it.
// Note that this depends on the fact that invokes are the only way to introduce promises
// in to a Pulumi program; if this changes in the future, this transform will need to be applied in a more general way
// (e.g. by the apply rewriter).
func (g *generator) outputInvokes(x model.Expression) model.Expression {
	rewriter := func(x model.Expression) (model.Expression, hcl.Diagnostics) {
		// Ignore the node if it is not a call to invoke.
		call, ok := x.(*model.FunctionCallExpression)
		if !ok || call.Name != pcl.Invoke {
			return x, nil
		}

		_, isOutput := call.Type().(*model.OutputType)
		if isOutput {
			return x, nil
		}

		_, isPromise := call.Type().(*model.PromiseType)
		contract.Assert(isPromise)

		return newOutputCall(call), nil
	}
	x, diags := model.VisitExpression(x, model.IdentityVisitor, rewriter)
	contract.Assert(len(diags) == 0)
	return x
}

func (g *generator) GetPrecedence(expr model.Expression) int {
	// TODO: Current values copied from C# that was copied from Node ;)
	// TODO(msh): Current values copied from Node, update based on
	// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/operators/
	switch expr := expr.(type) {
	case *model.ConditionalExpression:
		return 4
	case *model.BinaryOpExpression:
		switch expr.Operation {
		case hclsyntax.OpLogicalOr:
			return 5
		case hclsyntax.OpLogicalAnd:
			return 6
		case hclsyntax.OpEqual, hclsyntax.OpNotEqual:
			return 11
		case hclsyntax.OpGreaterThan, hclsyntax.OpGreaterThanOrEqual, hclsyntax.OpLessThan,
			hclsyntax.OpLessThanOrEqual:
			return 12
		case hclsyntax.OpAdd, hclsyntax.OpSubtract:
			return 14
		case hclsyntax.OpMultiply, hclsyntax.OpDivide, hclsyntax.OpModulo:
			return 15
		default:
			contract.Failf("unexpected binary expression %v", expr)
		}
	case *model.UnaryOpExpression:
		return 17
	case *model.FunctionCallExpression:
		switch expr.Name {
		case intrinsicAwait:
			return 17
		default:
			return 20
		}
	case *model.ForExpression, *model.IndexExpression, *model.RelativeTraversalExpression, *model.SplatExpression,
		*model.TemplateJoinExpression:
		return 20
	case *model.AnonymousFunctionExpression, *model.LiteralValueExpression, *model.ObjectConsExpression,
		*model.ScopeTraversalExpression, *model.TemplateExpression, *model.TupleConsExpression:
		return 22
	default:
		contract.Failf("unexpected expression %v of type %T", expr, expr)
	}
	return 0
}

func (g *generator) GenAnonymousFunctionExpression(w io.Writer, expr *model.AnonymousFunctionExpression) {
	switch len(expr.Signature.Parameters) {
	case 0:
		g.Fgen(w, "()")
	case 1:
		g.Fgenf(w, "%s", expr.Signature.Parameters[0].Name)
		g.Fgenf(w, " -> %v", expr.Body)
	default:
		g.Fgen(w, "values ->\n")
		g.Fgenf(w, "%s{\n", g.Indent)
		g.Indented(func() {
			for i, p := range expr.Signature.Parameters {
				g.Fgenf(w, "%svar %s = values.Item%d;\n", g.Indent, p.Name, i+1)
			}
			g.Fgenf(w, "%sreturn %v;\n", g.Indent, expr.Body)
		})
		g.Fgenf(w, "%s}", g.Indent)
	}
}

func (g *generator) GenBinaryOpExpression(w io.Writer, expr *model.BinaryOpExpression) {
	opstr, precedence := "", g.GetPrecedence(expr)
	switch expr.Operation {
	case hclsyntax.OpAdd:
		opstr = "+"
	case hclsyntax.OpDivide:
		opstr = "/"
	case hclsyntax.OpEqual:
		opstr = "=="
	case hclsyntax.OpGreaterThan:
		opstr = ">"
	case hclsyntax.OpGreaterThanOrEqual:
		opstr = ">="
	case hclsyntax.OpLessThan:
		opstr = "<"
	case hclsyntax.OpLessThanOrEqual:
		opstr = "<="
	case hclsyntax.OpLogicalAnd:
		opstr = "&&"
	case hclsyntax.OpLogicalOr:
		opstr = "||"
	case hclsyntax.OpModulo:
		opstr = "%"
	case hclsyntax.OpMultiply:
		opstr = "*"
	case hclsyntax.OpNotEqual:
		opstr = "!="
	case hclsyntax.OpSubtract:
		opstr = "-"
	default:
		opstr, precedence = ",", 1
	}

	g.Fgenf(w, "%.[1]*[2]v %[3]v %.[1]*[4]o", precedence, expr.LeftOperand, opstr, expr.RightOperand)
}

func (g *generator) GenConditionalExpression(w io.Writer, expr *model.ConditionalExpression) {
	g.Fgenf(w, "%.4v ? %.4v : %.4v", expr.Condition, expr.TrueResult, expr.FalseResult)
}

func (g *generator) GenForExpression(w io.Writer, expr *model.ForExpression) {
	g.genNYI(w, "ForExpression") // TODO
}

func (g *generator) genApply(w io.Writer, expr *model.FunctionCallExpression) {
	// Extract the list of outputs and the continuation expression from the `__apply` arguments.
	applyArgs, then := pcl.ParseApplyCall(expr)

	if len(applyArgs) == 1 {
		// If we only have a single output, just generate a normal `.Apply`
		g.Fgenf(w, "%.v.Apply(%.v)", applyArgs[0], then)
	} else {
		// Otherwise, generate a call to `Output.Tuple().Apply()`.
		g.Fgen(w, "Output.Tuple(")
		for i, o := range applyArgs {
			if i > 0 {
				g.Fgen(w, ", ")
			}
			g.Fgenf(w, "%.v", o)
		}

		g.Fgenf(w, ").Apply(%.v)", then)
	}
}

func (g *generator) genRange(w io.Writer, call *model.FunctionCallExpression, entries bool) {
	g.genNYI(w, "Range %.v %.v", call, entries)
}

// checks whether an expression is a template string making up a path
// for example "${siteDir}/${range.value}" which can be detected as input
// for fileAsset or archiveAsset and be converted to Paths.get(siteDir, range.value)
func isTemplatePathString(expr model.Expression) (bool, []model.Expression) {
	switch expr.(type) {
	case *model.TemplateExpression:
		allTextPartsAreSlashed := true
		var exprParts []model.Expression
		for _, part := range expr.(*model.TemplateExpression).Parts {
			lit, ok := part.(*model.LiteralValueExpression)
			if ok {
				// literal expression, check that it is a slash
				isTextLiteral := model.StringType.AssignableFrom(lit.Type())
				slash := isTextLiteral && (lit.Value.AsString() == "./" || lit.Value.AsString() == "/")
				allTextPartsAreSlashed = allTextPartsAreSlashed && slash
			} else {
				// any other expression
				exprParts = append(exprParts, part)
			}
		}
		return allTextPartsAreSlashed, exprParts
	default:
		return false, make([]model.Expression, 0)
	}
}

// functionName computes the C# namespace and class name for the given function token.
func (g *generator) functionName(tokenArg model.Expression) string {
	token := tokenArg.(*model.TemplateExpression).Parts[0].(*model.LiteralValueExpression).Value.AsString()
	tokenRange := tokenArg.SyntaxNode().Range()

	// Compute the resource type from the Pulumi type token.
	_, _, member, _ := pcl.DecomposeToken(token, tokenRange)
	return toUpperCase(member)
}

func (g *generator) GenFunctionCallExpression(w io.Writer, expr *model.FunctionCallExpression) {
	switch expr.Name {
	case pcl.IntrinsicConvert:
		switch arg := expr.Args[0].(type) {
		case *model.ObjectConsExpression:
			g.genObjectConsExpression(w, arg, &schema.MapType{ElementType: schema.StringType})
		default:
			g.Fgenf(w, "%.v", expr.Args[0]) // <- probably wrong w.r.t. precedence
		}
	case pcl.IntrinsicApply:
		g.genApply(w, expr)
	case intrinsicAwait:
		g.Fgenf(w, "await %.17v", expr.Args[0])
	case intrinsicOutput:
		g.Fgenf(w, "Output.of(%.v)", expr.Args[0])
	case "element":
		g.Fgenf(w, "%.20v[%.v]", expr.Args[0], expr.Args[1])
	case "entries":
		switch model.ResolveOutputs(expr.Args[0].Type()).(type) {
		case *model.ListType, *model.TupleType:
			if call, ok := expr.Args[0].(*model.FunctionCallExpression); ok && call.Name == "range" {
				g.genRange(w, call, true)
				return
			}
			g.Fgenf(w, "%.20v.Select((v, k)", expr.Args[0])
		case *model.MapType, *model.ObjectType:
			g.genNYI(w, "MapOrObjectEntries")
		}
		g.Fgenf(w, " => new { Key = k, Value = v })")
	case "fileArchive":
		isTemplate, templateParts := isTemplatePathString(expr.Args[0])
		if isTemplate {
			// emit Paths.get(part1, part2 ... partN)
			g.Fgen(w, "new FileArchive(Paths.get(")
			for index, part := range templateParts {
				if index == len(templateParts)-1 {
					// last element, no trailing comma
					g.Fgenf(w, "%.v", part)
				} else {
					g.Fgenf(w, "%.v, ", part)
				}
			}
			g.Fgen(w, "))")
		} else {
			g.Fgenf(w, "new FileArchive(%.v)", expr.Args[0])
		}
	case "fileAsset":
		isTemplate, templateParts := isTemplatePathString(expr.Args[0])
		if isTemplate {
			// emit Paths.get(part1, part2 ... partN)
			g.Fgen(w, "new FileAsset(Paths.get(")
			for index, part := range templateParts {
				if index == len(templateParts)-1 {
					// last element, no trailing comma
					g.Fgenf(w, "%.v", part)
				} else {
					g.Fgenf(w, "%.v, ", part)
				}
			}
			g.Fgen(w, "))")
		} else {
			g.Fgenf(w, "new FileAsset(%.v)", expr.Args[0])
		}
	case "filebase64":
		// Assuming the existence of the following helper method located earlier in the preamble
		g.Fgenf(w, "ReadFileBase64(%v)", expr.Args[0])
	case "filebase64sha256":
		// Assuming the existence of the following helper method located earlier in the preamble
		g.Fgenf(w, "ComputeFileBase64Sha256(%v)", expr.Args[0])
	case pcl.Invoke:
		name := g.functionName(expr.Args[0])
		foundFunction, functionSchema := g.findFunctionSchema(w, name)
		if foundFunction {
			g.Fprintf(w, "%s.invokeAsync(", name)
			invokeArgumentsExpr := expr.Args[1]
			switch invokeArgumentsExpr.(type) {
			case *model.ObjectConsExpression:
				argumentsExpr := invokeArgumentsExpr.(*model.ObjectConsExpression)
				g.genObjectConsExpressionWithTypeName(w, argumentsExpr, functionSchema.Inputs)
			}
			g.Fprint(w, ")")
			return

		}
		g.Fprintf(w, "%s.invokeAsync(", name)
		isOutput, outArgs, _ := pcl.RecognizeOutputVersionedInvoke(expr)
		if isOutput {
			//typeName := g.argumentTypeNameWithSuffix(expr, outArgsTy, "Args")
			g.genObjectConsExpressionWithTypeName(w, outArgs, &schema.MapType{ElementType: schema.StringType})
		} else {
			invokeArgumentsExpr := expr.Args[1]
			switch invokeArgumentsExpr.(type) {
			case *model.ObjectConsExpression:
				argumentsExpr := invokeArgumentsExpr.(*model.ObjectConsExpression)
				g.genObjectConsExpressionWithTypeName(w, argumentsExpr, &schema.MapType{ElementType: schema.StringType})
			}
		}

		g.Fprint(w, ")")
	case "join":
		g.Fgenf(w, "string.Join(%v, %v)", expr.Args[0], expr.Args[1])
	case "length":
		g.Fgenf(w, "%.20v.Length", expr.Args[0])
	case "lookup":
		g.Fgenf(w, "%v[%v]", expr.Args[0], expr.Args[1])
		if len(expr.Args) == 3 {
			g.Fgenf(w, " ?? %v", expr.Args[2])
		}
	case "range":
		g.genRange(w, expr, false)
	case "readFile":
		g.Fgenf(w, "File.ReadAllText(%v)", expr.Args[0])
	case "readDir":
		g.Fgenf(w, "ReadDir(%.v)", expr.Args[0])
	case "secret":
		g.Fgenf(w, "Output.CreateSecret(%v)", expr.Args[0])
	case "split":
		g.Fgenf(w, "%.20v.Split(%v)", expr.Args[1], expr.Args[0])
	case "mimeType":
		g.Fgenf(w, "Files.probeContentType(%v)", expr.Args[0])
	case "toBase64":
		g.Fgenf(w, "Convert.ToBase64String(System.Text.UTF8.GetBytes(%v))", expr.Args[0])
	case "toJSON":
		g.Fgen(w, "ToJson(")
		g.newline(w)
		g.Indented(func() {
			g.makeIndent(w)
			g.genJson(w, expr.Args[0])
		})
		g.Fgen(w, ")")
	case "sha1":
		// Assuming the existence of the following helper method located earlier in the preamble
		g.Fgenf(w, "ComputeSHA1(%v)", expr.Args[0])
	case "stack":
		g.Fgen(w, "Deployment.Instance.StackName")
	case "project":
		g.Fgen(w, "Deployment.Instance.ProjectName")
	case "cwd":
		g.Fgenf(w, "Directory.GetCurrentDirectory()")
	default:
		g.genNYI(w, "call %v", expr.Name)
	}
}

func (g *generator) genJson(w io.Writer, expr model.Expression) {
	switch expr := expr.(type) {
	case *model.ObjectConsExpression:
		g.Fgen(w, "JObject(")
		g.newline(w)
		g.Indented(func() {
			for index, item := range expr.Items {

				g.makeIndent(w)
				g.Fgenf(w, "JProperty(%s, ", item.Key)
				g.genJson(w, item.Value)
				g.Fgen(w, ")")
				if index < len(expr.Items)-1 {
					// elements
					g.Fgen(w, ",")
					g.newline(w)
				}
			}
		})
		g.newline(w)
		g.Fgenf(w, "%s)", g.Indent)
	case *model.TupleConsExpression:
		if len(expr.Expressions) == 1 {
			g.Fgen(w, "JArray(")
			g.genJson(w, expr.Expressions[0])
			g.Fgen(w, ")")
			return
		}
		g.Fgen(w, "JArray(")
		g.newline(w)
		g.Indented(func() {
			for index, value := range expr.Expressions {
				g.makeIndent(w)
				if index == len(expr.Expressions)-1 {
					// last element: no trailing comma
					g.genJson(w, value)
					g.newline(w)
				} else {
					g.genJson(w, value)
					g.Fgen(w, ", ")
					g.newline(w)
				}
			}
		})
		g.Fgenf(w, "%s)", g.Indent)

	default:
		g.Fgenf(w, "%.v", expr)
	}
}

func (g *generator) GenIndexExpression(w io.Writer, expr *model.IndexExpression) {
	g.Fgenf(w, "%v[%v]", expr.Collection, expr.Key)
}

func (g *generator) escapeString(v string, verbatim, expressions bool) string {
	builder := strings.Builder{}
	for _, c := range v {
		if verbatim {
			if c == '"' {
				builder.WriteRune('"')
			}
		} else {
			if c == '"' || c == '\\' {
				builder.WriteRune('\\')
			}
		}
		if expressions && (c == '{' || c == '}') {
			builder.WriteRune(c)
		}
		builder.WriteRune(c)
	}
	return builder.String()
}

func (g *generator) genStringLiteral(w io.Writer, v string) {
	newlines := strings.Contains(v, "\n")
	if !newlines {
		// This string does not contain newlines so we'll generate a regular string literal. Quotes and backslashes
		// will be escaped in conformance with
		// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure
		g.Fgen(w, "\"")
		g.Fgen(w, g.escapeString(v, false, false))
		g.Fgen(w, "\"")
	} else {
		// This string does contain newlines, so we'll generate a verbatim string literal. Quotes will be escaped
		// in conformance with
		// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/lexical-structure
		g.Fgen(w, "@\"")
		g.Fgen(w, g.escapeString(v, true, false))
		g.Fgen(w, "\"")
	}
}

func (g *generator) GenLiteralValueExpression(w io.Writer, expr *model.LiteralValueExpression) {
	typ := expr.Type()
	if cns, ok := typ.(*model.ConstType); ok {
		typ = cns.Type
	}

	switch typ {
	case model.BoolType:
		g.Fgenf(w, "%v", expr.Value.True())
	case model.NoneType:
		g.Fgen(w, "null")
	case model.NumberType:
		bf := expr.Value.AsBigFloat()
		if i, acc := bf.Int64(); acc == big.Exact {
			g.Fgenf(w, "%d", i)
		} else {
			f, _ := bf.Float64()
			g.Fgenf(w, "%g", f)
		}
	case model.StringType:
		g.genStringLiteral(w, expr.Value.AsString())
	default:
		contract.Failf("unexpected literal type in GenLiteralValueExpression: %v (%v)", expr.Type(),
			expr.SyntaxNode().Range())
	}
}

func (g *generator) GenObjectConsExpression(w io.Writer, expr *model.ObjectConsExpression) {
	g.genObjectConsExpression(w, expr, g.currentResourcePropertyType)
}

func (g *generator) argumentTypeName(expr model.Expression, destType model.Type) string {
	suffix := "Args"
	return g.argumentTypeNameWithSuffix(expr, destType, suffix)
}

func (g *generator) toSchemaType(destType model.Type) (schema.Type, bool) {
	schemaType, ok := pcl.GetSchemaForType(destType)
	if !ok {
		return nil, false
	}
	return codegen.UnwrapType(schemaType), true
}

func (g *generator) argumentTypeNameWithSuffix(expr model.Expression, destType model.Type, suffix string) string {
	schemaType, ok := g.toSchemaType(destType)
	if !ok {
		// TODO: why is this always returned?
		return ""
	}

	objType, ok := schemaType.(*schema.ObjectType)
	if !ok {
		return ""
	}

	token := objType.Token
	tokenRange := expr.SyntaxNode().Range()
	_, _, member, _ := pcl.DecomposeToken(token, tokenRange)
	member = member + suffix

	return toUpperCase(member)
}

func (g *generator) genObjectConsExpression(w io.Writer, expr *model.ObjectConsExpression, destType schema.Type) {
	if len(expr.Items) == 0 {
		return
	}

	g.genObjectConsExpressionWithTypeName(w, expr, destType)
}

func typeName(schemaType schema.Type) string {
	fullyQualifiedTypeName := schemaType.String()
	nameParts := strings.Split(fullyQualifiedTypeName, ":")
	return toUpperCase(nameParts[len(nameParts)-1])
}

func isObjectType(schemaType schema.Type) bool {
	switch schemaType.(type) {
	case *schema.ObjectType:
		return true
	default:
		return false
	}
}

func isArrayType(schemaType schema.Type) bool {
	switch schemaType.(type) {
	case *schema.ArrayType:
		return true
	default:
		return false
	}
}

func (g *generator) typedObjectExprScope(innerType schema.Type, exec func()) {
	reservedType := g.currentResourcePropertyType
	g.currentResourcePropertyType = innerType
	exec()
	g.currentResourcePropertyType = reservedType
}

func (g *generator) readStringAttribute(w io.Writer, key string, expr *model.ObjectConsExpression) (bool, string) {
	for _, item := range expr.Items {
		if key == item.Key.(*model.LiteralValueExpression).Value.AsString() {
			switch item.Value.(type) {
			case *model.LiteralValueExpression:
				return true, item.Value.(*model.LiteralValueExpression).Value.AsString()
			case *model.TemplateExpression:
				template := item.Value.(*model.TemplateExpression)
				if len(template.Parts) == 1 {
					firstPart := template.Parts[0]
					switch firstPart.(type) {
					case *model.LiteralValueExpression:
						return true, firstPart.(*model.LiteralValueExpression).Value.AsString()
					}
				}
			}
		}
	}

	return false, ""
}

func (g *generator) pickTypeFromUnion(w io.Writer, union *schema.UnionType, expr *model.ObjectConsExpression) schema.Type {
	foundDiscriminator, discriminator := g.readStringAttribute(w, union.Discriminator, expr)
	if foundDiscriminator {
		for _, unionType := range union.ElementTypes {
			if strings.Contains(typeName(unionType), discriminator) {
				return unionType
			}
		}
	}

	// we did what we can, return the default
	return union.ElementTypes[0]
}

func (g *generator) genObjectConsExpressionWithTypeName(
	w io.Writer, expr *model.ObjectConsExpression, destType schema.Type) {

	if len(expr.Items) == 0 {
		return
	}

	switch destType.(type) {
	case *schema.ObjectType:
		objectProperties := make(map[string]schema.Type)
		for _, property := range destType.(*schema.ObjectType).Properties {
			objectProperties[property.Name] = codegen.UnwrapType(property.Type)
		}
		g.Fgenf(w, "%s.builder()", typeName(destType))
		g.newline(w)
		g.Indented(func() {
			for _, item := range expr.Items {
				lit := item.Key.(*model.LiteralValueExpression)
				key := toLowerCase(lit.Value.AsString())
				attributeType := objectProperties[key]
				g.makeIndent(w)
				g.typedObjectExprScope(attributeType, func() {
					g.Fgenf(w, ".%s(%.v)", key, item.Value)
				})
				g.newline(w)
			}
			g.makeIndent(w)
			g.Fgenf(w, ".build()")
		})
	case *schema.ArrayType:
		// recurse into inner type
		innerType := destType.(*schema.ArrayType).ElementType
		g.genObjectConsExpressionWithTypeName(w, expr, innerType)
	case *schema.UnionType:
		union := destType.(*schema.UnionType)
		innerType := g.pickTypeFromUnion(w, union, expr)
		g.genObjectConsExpressionWithTypeName(w, expr, innerType)
	default:
		// generate map
		g.Fgen(w, "Map.ofEntries(\n")
		g.Indented(func() {
			for index, item := range expr.Items {
				if index == len(expr.Items)-1 {
					// Last item, no trailing comma
					g.Fgenf(w, "%sMap.entry(%.v, %.v)\n", g.Indent, item.Key, item.Value)
				} else {
					g.Fgenf(w, "%sMap.entry(%.v, %.v),\n", g.Indent, item.Key, item.Value)
				}
			}
		})
		g.Fgenf(w, "%s)", g.Indent)
	}
}

func (g *generator) genRelativeTraversal(w io.Writer,
	traversal hcl.Traversal, parts []model.Traversable, objType *schema.ObjectType) {

	for i, part := range traversal {
		var key cty.Value
		switch part := part.(type) {
		case hcl.TraverseAttr:
			key = cty.StringVal(part.Name)
		case hcl.TraverseIndex:
			key = part.Key
		default:
			contract.Failf("unexpected traversal part of type %T (%v)", part, part.SourceRange())
		}

		// TODO: what do we do with optionals in Java
		if model.IsOptionalType(model.GetTraversableType(parts[i])) {
			//g.Fgen(w, "?")
		}

		switch key.Type() {
		case cty.String:
			g.Fgenf(w, ".get%s()", toUpperCase(key.AsString()))
		case cty.Number:
			idx, _ := key.AsBigFloat().Int64()
			g.Fgenf(w, "[%d]", idx)
		default:
			contract.Failf("unexpected traversal key of type %T (%v)", key, key.AsString())
		}
	}
}

func (g *generator) GenRelativeTraversalExpression(w io.Writer, expr *model.RelativeTraversalExpression) {
	g.Fgenf(w, "%.20v", expr.Source)
	g.genRelativeTraversal(w, expr.Traversal, expr.Parts, nil)
}

func (g *generator) GenScopeTraversalExpression(w io.Writer, expr *model.ScopeTraversalExpression) {
	rootName := makeValidIdentifier(expr.RootName)

	g.Fgen(w, rootName)

	invokedFunctionSchema, isFunctionInvoke := g.functionInvokes[rootName]

	if isFunctionInvoke {
		lambdaArg := toLowerCase(typeName(invokedFunctionSchema.Outputs))
		g.Fgenf(w, ".thenApply(%s -> %s", lambdaArg, lambdaArg)
	}

	var objType *schema.ObjectType
	if resource, ok := expr.Parts[0].(*pcl.Resource); ok {
		if schemaType, ok := pcl.GetSchemaForType(resource.InputType); ok {
			objType, _ = schemaType.(*schema.ObjectType)
		}
	}
	g.genRelativeTraversal(w, expr.Traversal.SimpleSplit().Rel, expr.Parts, objType)

	if isFunctionInvoke {
		g.Fgenf(w, ")")
	}
}

func (g *generator) GenSplatExpression(w io.Writer, expr *model.SplatExpression) {
	g.genNYI(w, "GenSplatExpression") // TODO
}

func (g *generator) GenTemplateJoinExpression(w io.Writer, expr *model.TemplateJoinExpression) {
	g.genNYI(w, "TemplateJoinExpression")
}

// Generates an array of elements where each element is of type Union<Object1, Object2, ..., ObjectN>
func (g *generator) genArrayOfUnion(w io.Writer, union *schema.UnionType, exprs []*model.ObjectConsExpression) {
	if len(exprs) > 0 {
		if len(exprs) == 1 {
			// simple case, just write the first element
			objectType := g.pickTypeFromUnion(w, union, exprs[0])
			g.genObjectConsExpression(w, exprs[0], objectType)
			return
		}

		// Write multiple list elements
		g.Fgenf(w, "%s\n", g.Indent)
		g.Indented(func() {
			for index, expr := range exprs {
				objectType := g.pickTypeFromUnion(w, union, expr)
				if index == 0 {
					// first expression, no need for a new line
					g.makeIndent(w)
					g.genObjectConsExpression(w, expr, objectType)
					g.Fgen(w, ",")
				} else if index == len(exprs)-1 {
					// last element, no trailing comma
					g.newline(w)
					g.makeIndent(w)
					g.genObjectConsExpression(w, expr, objectType)
				} else {
					// elements in between: new line and trailing comma
					g.newline(w)
					g.makeIndent(w)
					g.genObjectConsExpression(w, expr, objectType)
					g.Fgen(w, ",")
				}
			}
		})
	}
}

// Returns whether the Union type has elements only of type Object
func unionOfObjectTypes(union *schema.UnionType) bool {
	for _, unionType := range union.ElementTypes {
		if !isObjectType(unionType) {
			return false
		}
	}
	return true
}

// GenTupleConsExpression generates a list of expressions
func (g *generator) GenTupleConsExpression(w io.Writer, expr *model.TupleConsExpression) {
	// Make special case for Array<Union<ObjectType1, ObjectType2, ..., ObjectTypeN>>
	switch g.currentResourcePropertyType.(type) {
	case *schema.ArrayType:
		arrayType := g.currentResourcePropertyType.(*schema.ArrayType)
		elementType := arrayType.ElementType
		switch elementType.(type) {
		case *schema.UnionType:
			union := elementType.(*schema.UnionType)
			if unionOfObjectTypes(union) {
				var objectExprs []*model.ObjectConsExpression
				for _, item := range expr.Expressions {
					objectExprs = append(objectExprs, item.(*model.ObjectConsExpression))
				}
				g.genArrayOfUnion(w, union, objectExprs)
				return
			}
		}
	}

	if len(expr.Expressions) > 0 {
		if len(expr.Expressions) == 1 {
			// simple case, just write the first element
			g.Fgenf(w, "%.v", expr.Expressions[0])
			return
		}

		// Write multiple list elements
		g.Fgenf(w, "%s\n", g.Indent)
		g.Indented(func() {
			for index, value := range expr.Expressions {
				if index == 0 {
					// first expression, no need for a new line
					g.Fgenf(w, "%s%.v,", g.Indent, value)
				} else if index == len(expr.Expressions)-1 {
					// last element, no trailing comma
					g.Fgenf(w, "\n%s%.v", g.Indent, value)
				} else {
					// elements in between: new line and trailing comma
					g.Fgenf(w, "\n%s%.v,", g.Indent, value)
				}
			}
		})
	}
}

func (g *generator) GenUnaryOpExpression(w io.Writer, expr *model.UnaryOpExpression) {
	g.genNYI(w, "GenUnaryOpExpression") // TODO
}
