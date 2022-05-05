// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"path"
	"strings"

	"github.com/zclconf/go-cty/cty"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model/format"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/syntax"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/utils"
	"github.com/pulumi/pulumi/sdk/v3/go/common/encoding"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/pulumi/pulumi/sdk/v3/go/common/workspace"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

type generator struct {
	// The formatter to use when generating code.
	*format.Formatter
	program *pcl.Program
	// TODO
	diagnostics                 hcl.Diagnostics
	currentResourcePropertyType schema.Type
	// keep track of variable identifiers which are the result of an invoke
	// for example "var resourceGroup = GetResourceGroup.invokeAsync(...)"
	// we will keep track of "resourceGroup" -> schema(GetResourceGroup)
	//
	// later on when apply a traversal sush as resourceGroup.getName(),
	// we should rewrite it as resourceGroup.thenApply(getResourceGroupResult -> getResourceGroupResult.getName())
	functionInvokes map[string]*schema.Function
}

func (g *generator) GenTemplateExpression(w io.Writer, expr *model.TemplateExpression) {
	multiLine := false
	expressions := false
	for _, expr := range expr.Parts {
		if lit, ok := expr.(*model.LiteralValueExpression); ok && model.StringType.AssignableFrom(lit.Type()) {
			if strings.Contains(lit.Value.AsString(), "\n") {
				multiLine = true
			}
		} else {
			expressions = true
		}
	}

	if multiLine {
		g.Fgen(w, "\"\"\"\n")
	} else if expressions {
		g.Fgen(w, "String.format(\"")
	} else {
		g.Fgen(w, "\"")
	}
	var args []model.Expression
	for _, expr := range expr.Parts {
		if lit, ok := expr.(*model.LiteralValueExpression); ok && model.StringType.AssignableFrom(lit.Type()) {
			if multiLine {
				// no need to escape
				g.Fgen(w, lit.Value.AsString())
			} else {
				g.Fgen(w, g.escapeString(lit.Value.AsString(), false, expressions))
			}

		} else {
			args = append(args, expr)
			g.Fgen(w, g.escapeString("%s", false, expressions))
		}
	}

	if expressions {
		// at the end of the interpolated string, append the quote
		g.Fgen(w, "\"")
		// emit the interpolated values, one at a time
		g.Fgen(w, ", ")
		for index, arg := range args {
			if index == len(args)-1 {
				// last element gets a closing parentheses
				g.Fgenf(w, "%.v)", arg)
			} else {
				g.Fgenf(w, "%.v,", arg)
			}
		}
	} else if multiLine {
		g.Fgenf(w, "%s\"\"\"", g.Indent)
	} else {
		g.Fgen(w, "\"")
	}
}

func containsFunctionCall(functionName string, nodes []pcl.Node) bool {
	foundRangeCall := false
	for _, node := range nodes {
		diags := node.VisitExpressions(model.IdentityVisitor, func(x model.Expression) (model.Expression, hcl.Diagnostics) {
			// Ignore the node if it is not a call to invoke.
			call, ok := x.(*model.FunctionCallExpression)
			if !ok {
				return x, nil
			}

			if call.Name == functionName {
				foundRangeCall = true
			}

			return x, nil
		})

		contract.Assert(len(diags) == 0)
	}

	return foundRangeCall
}

func hasIterableResources(nodes []pcl.Node) bool {
	for _, node := range nodes {
		switch node.(type) {
		case *pcl.Resource:
			resource := node.(*pcl.Resource)
			if resource.Options != nil && resource.Options.Range != nil {
				return true
			}
		}
	}

	return false
}

func containsRangeExpr(nodes []pcl.Node) bool {
	return hasIterableResources(nodes) || containsFunctionCall("readDir", nodes)
}

func GenerateProgram(program *pcl.Program) (map[string][]byte, hcl.Diagnostics, error) {
	// Linearize the nodes into an order appropriate for procedural code generation.
	nodes := pcl.Linearize(program)

	// Import Java-specific schema info.
	// FIX ME: not sure what this is doing...
	for _, p := range program.Packages() {
		if err := p.ImportLanguages(map[string]schema.Language{"java": Importer}); err != nil {
			return nil, nil, err
		}
	}

	g := &generator{
		program:         program,
		functionInvokes: map[string]*schema.Function{},
	}

	g.Formatter = format.NewFormatter(g)

	var index bytes.Buffer
	g.genPreamble(&index, nodes)

	g.Indented(func() {
		g.Indented(func() {
			for _, n := range nodes {
				g.genNode(&index, n)
			}
		})
	})

	g.genPostamble(&index, nodes)

	files := map[string][]byte{
		"MyStack.java": index.Bytes(),
	}
	return files, g.diagnostics, nil
}

func GenerateProject(directory string, project workspace.Project, program *pcl.Program) error {
	files, diagnostics, err := GenerateProgram(program)
	if err != nil {
		return err
	}
	if diagnostics.HasErrors() {
		return diagnostics
	}

	// Set the runtime to "java" then marshal to Pulumi.yaml
	project.Runtime = workspace.NewProjectRuntimeInfo("java", nil)
	projectBytes, err := encoding.YAML.Marshal(project)
	if err != nil {
		return err
	}
	files["Pulumi.yaml"] = projectBytes

	for filename, data := range files {
		outPath := path.Join(directory, filename)
		err := ioutil.WriteFile(outPath, data, 0600)
		if err != nil {
			return fmt.Errorf("could not write output program: %w", err)
		}
	}

	return nil
}

func (g *generator) genImport(w io.Writer, qualifiedName string) {
	g.Fprintf(w, "import %s;\n", qualifiedName)
}

func (g *generator) genIndent(w io.Writer) {
	g.Fgenf(w, "%s", g.Indent)
}
func (g *generator) genNewline(w io.Writer) {
	g.Fgen(w, "\n")
}

// Checks whether any of the input nodes is a configuration variable
func containConfigVariables(nodes []pcl.Node) bool {
	for _, node := range nodes {
		switch node.(type) {
		case *pcl.ConfigVariable:
			return true
		}
	}

	return false
}

// genPreamble generates import statements, class definition and constructor.
func (g *generator) genPreamble(w io.Writer, nodes []pcl.Node) {
	g.Fgen(w, "package generated_program;")
	g.genNewline(w)
	g.genNewline(w)
	g.genImport(w, "java.util.*")
	g.genImport(w, "java.io.*")
	g.genImport(w, "java.nio.*")
	g.genImport(w, "com.pulumi.*")
	if containsFunctionCall("toJSON", nodes) {
		// import static functions from the Serialization class
		g.Fgen(w, "import static com.pulumi.codegen.internal.Serialization.*;\n")
	}

	if containsRangeExpr(nodes) {
		// import the KeyedValue<T> class
		g.Fgen(w, "import com.pulumi.codegen.internal.KeyedValue;\n")
	}

	if containsFunctionCall("readDir", nodes) {
		g.Fgen(w, "import static com.pulumi.codegen.internal.Files.readDir;\n")
	}

	g.genNewline(w)
	// Emit Stack class signature
	g.Fprint(w, "public class App {")
	g.genNewline(w)
	g.Fprint(w, "    public static void main(String[] args) {\n")
	g.Fgen(w, "        Pulumi.run(App::stack);\n")
	g.Fgen(w, "    }\n")
	g.genNewline(w)
	g.Fprint(w, "    public static void stack(Context ctx) {\n")
	if containConfigVariables(nodes) {
		g.Fprint(w, "        final var config = ctx.config();\n")
	}
}

// genPostamble closes the method and the class and declares stack output statements.
func (g *generator) genPostamble(w io.Writer, nodes []pcl.Node) {
	g.Indented(func() {
		g.genIndent(w)
		g.Fgen(w, "}\n")
	})
	g.Fprint(w, "}\n")
}

// resourceTypeName computes the Java resource class name for the given resource.
func resourceTypeName(resource *pcl.Resource) string {
	// Compute the resource type from the Pulumi type token.
	pkg, module, member, diags := resource.DecomposeToken()
	contract.Assert(len(diags) == 0)
	if pkg == "pulumi" && module == "providers" {
		member = "Provider"
	}

	return names.Title(member)
}

// resourceArgsTypeName computes the Java arguments class name for the given resource.
func resourceArgsTypeName(r *pcl.Resource) string {
	return fmt.Sprintf("%sArgs", resourceTypeName(r))
}

// Returns the expression that should be emitted for a resource's "name" parameter given its base name
func makeResourceName(baseName string, suffix string) string {
	if suffix == "" {
		return fmt.Sprintf(`"%s"`, baseName)
	}
	return fmt.Sprintf(`"%s-"`, baseName) + " + " + suffix
}

func (g *generator) findFunctionSchema(function string) (*schema.Function, bool) {
	for _, pkg := range g.program.Packages() {
		if pkg.Functions != nil {
			for _, functionSchema := range pkg.Functions {
				if functionSchema != nil && strings.HasSuffix(functionSchema.Token, names.LowerCamelCase(function)) {
					return functionSchema, true
				}
			}
		}
	}

	return nil, false
}

func getTraversalKey(traversal hcl.Traversal) string {
	for _, part := range traversal {
		switch part := part.(type) {
		case hcl.TraverseAttr:
			return cty.StringVal(part.Name).AsString()
		case hcl.TraverseIndex:
			return part.Key.AsString()
		default:
			contract.Failf("unexpected traversal part of type %T (%v)", part, part.SourceRange())
		}
	}

	return ""
}

func (g *generator) genResource(w io.Writer, resource *pcl.Resource) {
	resourceTypeName := resourceTypeName(resource)
	resourceArgs := resourceArgsTypeName(resource)
	variableName := names.LowerCamelCase(names.MakeValidIdentifier(resource.Name()))
	instantiate := func(resName string) {
		resourceProperties := map[string]schema.Type{}
		resourceSchema := resource.Schema
		if resourceSchema != nil && resourceSchema.InputProperties != nil {
			for _, property := range resourceSchema.InputProperties {
				if property != nil && property.Type != nil {
					resourceProperties[property.Name] = codegen.UnwrapType(property.Type)
				}
			}
		}

		if len(resource.Inputs) == 0 {
			g.Fgenf(w, "new %s(%s)", resourceTypeName, resName)
		} else {
			g.Fgenf(w, "new %s(%s, %s.builder()", resourceTypeName, resName, resourceArgs)
			g.Fgenf(w, "%s\n", g.Indent)
			g.Indented(func() {
				for _, attr := range resource.Inputs {
					attributeIdent := names.MakeValidIdentifier(attr.Name)
					attributeSchemaType := resourceProperties[attr.Name]
					g.currentResourcePropertyType = attributeSchemaType
					g.Fgenf(w, "%s.%s(%.v)\n", g.Indent, attributeIdent, g.lowerExpression(attr.Value, attr.Type()))
				}

				g.Fgenf(w, "%s.build())", g.Indent)
			})
			// resource options: TODO
			//g.Fgenf(w, "%s}%s)", g.Indent, g.genResourceOptions(r.Options))
		}
	}

	if resource.Options != nil && resource.Options.Range != nil {
		// generate list of resources
		rangeType := model.ResolveOutputs(resource.Options.Range.Type())
		rangeExpr := g.lowerExpression(resource.Options.Range, rangeType)
		isNumericRange := model.InputType(model.NumberType).ConversionFrom(rangeExpr.Type()) != model.NoConversion
		if isNumericRange {
			// numeric range
			g.Fgenf(w, "%sfor (var i = 0; i < %.12o; i++) {\n", g.Indent, rangeExpr)
			g.Indented(func() {
				// register each resource
				g.Fgenf(w, "%s", g.Indent)
				instantiate(makeResourceName(resource.Name(), "i"))
				g.Fgenf(w, ";\n\n")
			})
			g.Fgenf(w, "%s\n}\n", g.Indent)

		} else {
			// for each-loop through the elements to creates a resource from each one
			switch rangeExpr.(type) {
			case *model.FunctionCallExpression:
				funcCall := rangeExpr.(*model.FunctionCallExpression)
				switch funcCall.Name {
				case pcl.IntrinsicConvert:
					firstArg := funcCall.Args[0]
					switch firstArg.(type) {
					case *model.ScopeTraversalExpression:
						traversalExpr := firstArg.(*model.ScopeTraversalExpression)
						if len(traversalExpr.Parts) == 2 {
							// Meaning here we have {root}.{part} expression which the most common
							// check whether {root} is actually a variable name that holds the result
							// of a function invoke
							if functionSchema, isInvoke := g.functionInvokes[traversalExpr.RootName]; isInvoke {
								resultTypeName := names.LowerCamelCase(typeName(functionSchema.Outputs))
								part := getTraversalKey(traversalExpr.Traversal.SimpleSplit().Rel)
								g.genIndent(w)
								g.Fgenf(w, "final var %s = ", resource.Name())
								g.Fgenf(w, "%s.apply(%s -> {\n", traversalExpr.RootName, resultTypeName)
								g.Indented(func() {
									g.Fgenf(w, "%sfinal var resources = new ArrayList<%s>();\n", g.Indent, resourceTypeName)
									g.Fgenf(w, "%sfor (var range : KeyedValue.of(%s.%s()) {\n", g.Indent, resultTypeName, part)
									g.Indented(func() {
										suffix := "range.key()"
										g.Fgenf(w, "%svar resource = ", g.Indent)
										instantiate(makeResourceName(resource.Name(), suffix))
										g.Fgenf(w, ";\n\n")
										g.Fgenf(w, "%sresources.add(resource);\n", g.Indent)
									})
									g.Fgenf(w, "%s}\n\n", g.Indent)
									g.Fgenf(w, "%sreturn resources;\n", g.Indent)
								})
								g.Fgenf(w, "%s});\n\n", g.Indent)
								return
							}
							// not an async function invoke
							// wrap into range collection
							g.Fgenf(w, "%sfor (var range : KeyedValue.of(%.12o)) {\n", g.Indent, rangeExpr)
						} else {
							// wrap into range collection
							g.Fgenf(w, "%sfor (var range : KeyedValue.of(%.12o)) {\n", g.Indent, rangeExpr)
						}
					}
					// wrap into range collection
					g.Fgenf(w, "%sfor (var range : KeyedValue.of(%.12o)) {\n", g.Indent, rangeExpr)
				default:
					// assume function call returns a Range<T>
					g.Fgenf(w, "%sfor (var range : %.12o) {\n", g.Indent, rangeExpr)
				}

			default:
				// wrap into range collection
				g.Fgenf(w, "%sfor (var range : KeyedValue.of(%.12o)) {\n", g.Indent, rangeExpr)
			}

			g.Indented(func() {
				suffix := "range.key()"
				g.Fgenf(w, "%s", g.Indent)
				instantiate(makeResourceName(resource.Name(), suffix))
				g.Fgenf(w, ";\n")
			})
			g.Fgenf(w, "%s}\n\n", g.Indent)
		}

	} else {
		// generate single resource
		g.Fgenf(w, "%svar %s = ", g.Indent, variableName)
		suffix := ""
		instantiate(makeResourceName(resource.Name(), suffix))
		g.Fgenf(w, ";\n\n")
	}
}

func (g *generator) genConfigVariable(w io.Writer, configVariable *pcl.ConfigVariable) {
	g.genIndent(w)
	if configVariable.DefaultValue != nil {
		g.Fgenf(w, "final var %s = config.get(\"%s\").orElse(%v);",
			names.MakeValidIdentifier(configVariable.Name()),
			configVariable.Name(),
			configVariable.DefaultValue)
	} else {
		g.Fgenf(w, "final var %s = config.get(\"%s\");",
			names.MakeValidIdentifier(configVariable.Name()),
			configVariable.Name())
	}
	g.genNewline(w)
}

func (g *generator) isFunctionInvoke(localVariable *pcl.LocalVariable) (*schema.Function, bool) {
	value := localVariable.Definition.Value
	switch value.(type) {
	case *model.FunctionCallExpression:
		call := value.(*model.FunctionCallExpression)
		switch call.Name {
		case pcl.Invoke:
			args := call.Args[0]
			_, schemaName := g.functionName(args)
			functionSchema, foundFunction := g.findFunctionSchema(schemaName)
			if foundFunction {
				return functionSchema, true
			}
		}
	}

	return nil, false
}

func (g *generator) genLocalVariable(w io.Writer, localVariable *pcl.LocalVariable) {
	variableName := localVariable.Name()
	functionSchema, isInvokeCall := g.isFunctionInvoke(localVariable)
	g.genIndent(w)
	if isInvokeCall {
		g.functionInvokes[variableName] = functionSchema
		// convert CompletableFuture<T> to Output<T> using Output.of
		// TODO: call the Output<T>-version of invokes when the SDK allows it.
		functionDefinition := outputOf(localVariable.Definition.Value)
		// TODO: lowerExpression isn't what we expect: function call should extract outputs into .apply(...) calls
		//functionDefinitionWithApplies := g.lowerExpression(functionDefinition, localVariable.Definition.Value.Type())
		g.Fgenf(w, "final var %s = %v;\n", variableName, functionDefinition)
	} else {
		variable := localVariable.Definition.Value
		g.Fgenf(w, "final var %s = %v;\n", variableName, g.lowerExpression(variable, variable.Type()))
	}
	g.genNewline(w)
}

func (g *generator) genOutputAssignment(w io.Writer, outputVariable *pcl.OutputVariable) {
	g.genIndent(w)
	rewrittenOutVar := g.lowerExpression(outputVariable.Value, outputVariable.Type())
	g.Fgenf(w, "ctx.export(\"%s\", %v);", outputVariable.Name(), rewrittenOutVar)
	g.genNewline(w)
}

func (g *generator) genNode(w io.Writer, n pcl.Node) {
	switch n := n.(type) {
	case *pcl.Resource:
		g.genResource(w, n)
	case *pcl.ConfigVariable:
		g.genConfigVariable(w, n)
	case *pcl.LocalVariable:
		g.genLocalVariable(w, n)
	case *pcl.OutputVariable:
		g.genOutputAssignment(w, n)
	}
}

// TODO

func (g *generator) genNYI(w io.Writer, reason string, vs ...interface{}) {
	message := fmt.Sprintf("not yet implemented: %s", fmt.Sprintf(reason, vs...))
	g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
		Severity: hcl.DiagError,
		Summary:  message,
		Detail:   message,
	})
	g.Fgenf(w, "\"TODO: %s\"", fmt.Sprintf(reason, vs...))
}

func compilePclToJava(source []byte, schemaPath string) ([]byte, hcl.Diagnostics, error) {
	parser := syntax.NewParser()
	err := parser.ParseFile(bytes.NewReader(source), "")
	if err != nil {
		return nil, nil, err
	}
	program, programDiags, err := pcl.BindProgram(parser.Files, pcl.PluginHost(utils.NewHost(schemaPath)))
	if err != nil {
		return nil, nil, err
	}

	if programDiags != nil && programDiags.HasErrors() {
		for _, diagnosticError := range programDiags.Errs() {
			panic(diagnosticError.Error())
		}
	}

	javaPrograms, diagnostics, err := GenerateProgram(program)
	if err != nil {
		return nil, nil, err
	}

	return javaPrograms["MyStack.java"], diagnostics, nil
}
