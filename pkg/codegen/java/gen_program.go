// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	"fmt"
	"io"
	"os"
	"path"
	"strings"

	"github.com/zclconf/go-cty/cty"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model/format"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/encoding"
	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"
	"github.com/pulumi/pulumi/sdk/v3/go/common/workspace"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

const (
	pulumiToken    = "pulumi"
	providersToken = "providers"
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
		switch node := node.(type) {
		case *pcl.Resource:
			resource := node
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
	pcl.MapProvidersAsResources(program)
	// Linearize the nodes into an order appropriate for procedural code generation.
	nodes := pcl.Linearize(program)

	// Import Java-specific schema info.
	// FIX ME: not sure what this is doing...
	packages, err := program.PackageSnapshots()
	if err != nil {
		return nil, nil, err
	}
	for _, p := range packages {
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

	filesWithPackages := make(map[string][]byte)

	filesWithPackages["Pulumi.yaml"] = projectBytes

	for fileName, fileContents := range files {
		if fileName == "MyStack.java" {
			fileName = "App.java"
		}
		fileWithPackage := fmt.Sprintf("src/main/java/generated_program/%s", fileName)
		filesWithPackages[fileWithPackage] = fileContents
	}

	var mavenDependenciesXML bytes.Buffer
	packages, err := program.PackageSnapshots()
	if err != nil {
		return err
	}
	for _, p := range packages {
		packageName := p.Name
		version := p.Version
		if version != nil {
			dependencySection := fmt.Sprintf(
				`<dependency>
            		<groupId>com.pulumi</groupId>
            		<artifactId>%s</artifactId>
            		<version>%s</version>
        		</dependency>`,
				packageName, version.String(),
			)
			mavenDependenciesXML.WriteString(dependencySection)
		}
	}

	mavenPomXML := bytes.NewBufferString(fmt.Sprintf(
		`<?xml version="1.0" encoding="UTF-8"?>
		<project xmlns="http://maven.apache.org/POM/4.0.0"
				 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
			<modelVersion>4.0.0</modelVersion>

			<groupId>com.pulumi</groupId>
			<artifactId>%s</artifactId>
			<version>1.0-SNAPSHOT</version>

			<properties>
				<encoding>UTF-8</encoding>
				<maven.compiler.source>11</maven.compiler.source>
				<maven.compiler.target>11</maven.compiler.target>
				<maven.compiler.release>11</maven.compiler.release>
				<mainClass>generated_program.App</mainClass>
				<mainArgs/>
			</properties>

			<dependencies>
				<dependency>
					<groupId>com.pulumi</groupId>
					<artifactId>pulumi</artifactId>
					<version>(,1.0]</version>
				</dependency>
				%s
			</dependencies>

			<build>
				<plugins>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-jar-plugin</artifactId>
						<version>3.2.2</version>
						<configuration>
							<archive>
								<manifest>
									<addClasspath>true</addClasspath>
									<mainClass>${mainClass}</mainClass>
								</manifest>
							</archive>
						</configuration>
					</plugin>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-assembly-plugin</artifactId>
						<version>3.4.2</version>
						<configuration>
							<archive>
								<manifest>
									<addClasspath>true</addClasspath>
									<mainClass>${mainClass}</mainClass>
								</manifest>
							</archive>
							<descriptorRefs>
								<descriptorRef>jar-with-dependencies</descriptorRef>
							</descriptorRefs>
						</configuration>
						<executions>
							<execution>
								<id>make-my-jar-with-dependencies</id>
								<phase>package</phase>
								<goals>
									<goal>single</goal>
								</goals>
							</execution>
						</executions>
					</plugin>
					<plugin>
						<groupId>org.codehaus.mojo</groupId>
						<artifactId>exec-maven-plugin</artifactId>
						<version>3.1.0</version>
						<configuration>
							<mainClass>${mainClass}</mainClass>
							<commandlineArgs>${mainArgs}</commandlineArgs>
						</configuration>
					</plugin>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-wrapper-plugin</artifactId>
						<version>3.1.1</version>
						<configuration>
							<mavenVersion>3.8.5</mavenVersion>
						</configuration>
					</plugin>
				</plugins>
			</build>
		</project>`,
		project.Name.String(), mavenDependenciesXML.String(),
	))
	filesWithPackages["pom.xml"] = mavenPomXML.Bytes()

	for filePath, data := range filesWithPackages {
		outPath := path.Join(directory, filePath)
		err := os.MkdirAll(path.Dir(outPath), os.ModePerm)
		if err != nil {
			return fmt.Errorf("could not write output program: %w", err)
		}
		err = os.WriteFile(outPath, data, 0o600)
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

func cleanModule(module string) string {
	if strings.Contains(module, "/") {
		// if it is a function module
		// e.g. index/getAvailabilityZones
		moduleParts := strings.Split(module, "/")
		// unless it is just a version e.g. core/v1
		// then return it as is
		if len(moduleParts) == 2 && strings.HasPrefix(moduleParts[1], "v") {
			return module
		}
		return moduleParts[0]
	}

	return module
}

// Decides when to ignore a module in import definition
func ignoreModule(module string) bool {
	return module == "" || module == "index"
}

// Removes dashes and replaces slash with underscore
func sanitizeImport(name string) string {
	// TODO to be revised when https://github.com/pulumi/pulumi-java/issues/381 is resolved
	// e.g. azure-native becomes azurenative
	withoutDash := strings.ReplaceAll(name, "-", "")
	// e.g. kubernetes.core/v1 becomes kubernetes.core_v1
	replacedSlash := strings.ReplaceAll(withoutDash, "/", "_")
	return replacedSlash
}

func pulumiImport(pkg string, module string, member string) string {
	module = cleanModule(module)
	if ignoreModule(module) {
		return "com.pulumi." + sanitizeImport(pkg) + "." + member
	} else if module == "" {
		return "com.pulumi." + sanitizeImport(pkg)
	}
	return "com.pulumi." + sanitizeImport(pkg) + "." + sanitizeImport(module) + "." + member
}

func pulumiInputImport(pkg string, module string, member string) string {
	module = cleanModule(module)
	if ignoreModule(module) {
		return "com.pulumi." + sanitizeImport(pkg) + ".inputs." + member
	}
	return "com.pulumi." + sanitizeImport(pkg) + "." + sanitizeImport(module) + ".inputs." + member
}

// Recursively derives imports from object by using its property type and
// any nested object property that it instantiates
func collectObjectImports(object *model.ObjectConsExpression, objectType *schema.ObjectType) []string {
	imports := make([]string, 0)
	// add imports of the type itself
	fullyQualifiedTypeName := objectType.Token
	nameParts := strings.Split(fullyQualifiedTypeName, ":")
	objectTypeName := names.Title(nameParts[len(nameParts)-1])
	pkg, module := nameParts[0], nameParts[1]

	if objectType.IsInputShape() {
		if !strings.HasSuffix(objectTypeName, "Args") {
			objectTypeName = objectTypeName + "Args"
		}

		imports = append(imports, pulumiInputImport(pkg, module, objectTypeName))
	} else {
		imports = append(imports, pulumiImport(pkg, module, objectTypeName))
	}

	// then check whether one of the properties of this object is an object too
	// in which case, we call this function recursively
	for _, property := range object.Items {
		switch property.Value.(type) {
		case *model.ObjectConsExpression:
			innerObject := property.Value.(*model.ObjectConsExpression)
			innerObjectKey := property.Key.(*model.LiteralValueExpression).Value.AsString()
			objectProperty, found := objectType.Property(innerObjectKey)
			if found {
				objectPropertyType := codegen.UnwrapType(objectProperty.Type)
				switch objectPropertyType := objectPropertyType.(type) {
				case *schema.ObjectType:
					innerObjectType := objectPropertyType
					// recurse into nested object
					imports = append(imports, collectObjectImports(innerObject, innerObjectType)...)
				}
			}
		}
	}

	return imports
}

// reduces Array<Input<T>> to just Array<T>
func reduceInputTypeFromArray(arrayType *schema.ArrayType) *schema.ArrayType {
	elementType := arrayType.ElementType
	switch elementType := elementType.(type) {
	case *schema.InputType:
		inputType := elementType
		return &schema.ArrayType{ElementType: inputType.ElementType}
	default:
		// return as-is
		return arrayType
	}
}

func collectResourceImports(resource *pcl.Resource) []string {
	imports := make([]string, 0)
	pkg, module, name, _ := resource.DecomposeToken()
	resourceImport := pulumiImport(pkg, module, name)
	imports = append(imports, resourceImport)
	if len(resource.Inputs) > 0 || hasCustomResourceOptions(resource) {
		// import args type name
		argsTypeName := resourceArgsTypeName(resource)
		resourceArgsImport := pulumiImport(pkg, module, argsTypeName)
		imports = append(imports, resourceArgsImport)
		resourceProperties := typedResourceProperties(resource)
		for _, inputProperty := range resource.Inputs {
			inputType := resourceProperties[inputProperty.Name]
			switch inputType.(type) {
			case *schema.ObjectType:
				objectType := inputType.(*schema.ObjectType)
				switch inputProperty.Value.(type) {
				case *model.ObjectConsExpression:
					object := inputProperty.Value.(*model.ObjectConsExpression)
					imports = append(imports, collectObjectImports(object, objectType)...)
				}
			case *schema.ArrayType:
				arrayType := reduceInputTypeFromArray(inputType.(*schema.ArrayType))
				innerArrayType := arrayType.ElementType
				switch innerArrayType.(type) {
				case *schema.ObjectType:
					// found an Array<ElementType> type where ElementType is Object
					// loop through each element of the array
					// assume each element is of ElementType
					arrayInnerTypeAsObject := innerArrayType.(*schema.ObjectType)
					switch inputProperty.Value.(type) {
					case *model.TupleConsExpression:
						objects := inputProperty.Value.(*model.TupleConsExpression)
						for _, arrayObject := range objects.Expressions {
							switch arrayObject := arrayObject.(type) {
							case *model.ObjectConsExpression:
								object := arrayObject
								imports = append(imports, collectObjectImports(object, arrayInnerTypeAsObject)...)
							}
						}
					}
				}
			}
		}
	}

	return imports
}

func (g *generator) functionImportDef(tokenArg model.Expression) (string, string) {
	token := tokenArg.(*model.TemplateExpression).Parts[0].(*model.LiteralValueExpression).Value.AsString()
	tokenRange := tokenArg.SyntaxNode().Range()

	// Compute the resource type from the Pulumi type token.
	pkg, module, member, _ := pcl.DecomposeToken(token, tokenRange)
	pkg = sanitizeImport(pkg)
	module = sanitizeImport(module)
	member = sanitizeImport(member)
	if ignoreModule(module) {
		importDef := "com.pulumi." + pkg + "." + names.Title(pkg) + "Functions"
		return importDef, member
	}

	return pulumiImport(pkg, module, names.Title(module)+"Functions"), member
}

func (g *generator) collectFunctionCallImports(functionCall *model.FunctionCallExpression) []string {
	imports := make([]string, 0)
	switch functionCall.Name {
	case pcl.Invoke:
		fullyQualifiedFunctionImport, funcName := g.functionImportDef(functionCall.Args[0])
		imports = append(imports, fullyQualifiedFunctionImport)
		functionSchema, foundFunction := g.findFunctionSchema(functionCall.Args[0])
		if foundFunction {
			invokeArgumentsExpr := functionCall.Args[1]
			switch invokeArgumentsExpr := invokeArgumentsExpr.(type) {
			case *model.ObjectConsExpression:
				argumentsExpr := invokeArgumentsExpr
				if functionSchema.Inputs == nil {
					g.warnf(functionCall.Args[1].SyntaxNode().Range().Ptr(),
						"cannot determine invoke argument type: the schema for %q has no inputs",
						funcName)
					return imports
				}
				argumentExprType := functionSchema.Inputs.InputShape
				imports = append(imports, collectObjectImports(argumentsExpr, argumentExprType)...)
			}
		}
	}

	return imports
}

// Removes duplicate strings. Useful when collecting a distinct set of imports
func removeDuplicates(inputs []string) []string {
	distinctInputs := make([]string, 0)
	seenTexts := make(map[string]bool)
	for _, input := range inputs {
		if _, seen := seenTexts[input]; !seen {
			seenTexts[input] = true
			distinctInputs = append(distinctInputs, input)
		}
	}

	return distinctInputs
}

// Collects fully qualified imports from resource or variable definitions.
// It recursively searches for imports from nested object definitions used in resource
// configuration or used in function calls
func (g *generator) collectImports(nodes []pcl.Node) []string {
	imports := make([]string, 0)
	for _, node := range nodes {
		switch node := node.(type) {
		case *pcl.Resource:
			// collect resource imports
			resource := node
			imports = append(imports, collectResourceImports(resource)...)
		case *pcl.LocalVariable:
			localVariable := node
			switch localVariable.Definition.Value.(type) {
			case *model.FunctionCallExpression:
				// collect function invoke imports
				// traverse the args and inner objects recursively
				functionCall := localVariable.Definition.Value.(*model.FunctionCallExpression)
				_, isInvokeCall := g.isFunctionInvoke(localVariable)
				if isInvokeCall {
					imports = append(imports, g.collectFunctionCallImports(functionCall)...)
				}
			}
		}
	}

	return removeDuplicates(imports)
}

// genPreamble generates import statements, main class and stack definition.
func (g *generator) genPreamble(w io.Writer, nodes []pcl.Node) {
	g.Fgen(w, "package generated_program;")
	g.genNewline(w)
	g.genNewline(w)
	g.genImport(w, "com.pulumi.Context")
	g.genImport(w, "com.pulumi.Pulumi")
	g.genImport(w, "com.pulumi.core.Output")
	// Write out the specific imports from used nodes
	for _, importDef := range g.collectImports(nodes) {
		g.genImport(w, importDef)
	}

	if containsFunctionCall("toJSON", nodes) {
		// import static functions from the Serialization class
		g.Fgen(w, "import static com.pulumi.codegen.internal.Serialization.*;\n")
	}

	if containsRangeExpr(nodes) {
		// import the KeyedValue<T> class
		g.genImport(w, "com.pulumi.codegen.internal.KeyedValue")
	}

	if requiresImportingCustomResourceOptions(nodes) {
		g.genImport(w, "com.pulumi.resources.CustomResourceOptions")
	}

	if containsFunctionCall("readDir", nodes) {
		g.genImport(w, "static com.pulumi.codegen.internal.Files.readDir")
	}

	if containsFunctionCall("fileAsset", nodes) {
		g.genImport(w, "com.pulumi.asset.FileAsset")
	}

	if containsFunctionCall("fileArchive", nodes) {
		g.genImport(w, "com.pulumi.asset.FileArchive")
	}

	g.genImport(w, "java.util.List")
	g.genImport(w, "java.util.ArrayList")
	g.genImport(w, "java.util.Map")
	g.genImport(w, "java.io.File")
	g.genImport(w, "java.nio.file.Files")
	g.genImport(w, "java.nio.file.Paths")

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
	if pkg == pulumiToken && module == providersToken {
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

func (g *generator) findFunctionSchema(token model.Expression) (*schema.Function, bool) {
	tk := token.(*model.TemplateExpression).Parts[0].(*model.LiteralValueExpression).Value.AsString()
	for _, pkg := range g.program.PackageReferences() {
		fn, ok, err := pcl.LookupFunction(pkg, tk)
		if !ok {
			continue
		}
		if err != nil {
			g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
				Severity: hcl.DiagWarning,
				Summary:  fmt.Sprintf("Could not find function schema for '%s'", token),
				Detail:   err.Error(),
				Subject:  token.SyntaxNode().Range().Ptr(),
			})
			return nil, false
		}
		return fn, true
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

// Returns whether a resource has a custom resource option (other than range)
func hasCustomResourceOptions(resource *pcl.Resource) bool {
	if resource.Options == nil {
		return false
	}

	return resource.Options.IgnoreChanges != nil ||
		resource.Options.DependsOn != nil ||
		resource.Options.Parent != nil ||
		resource.Options.Protect != nil ||
		resource.Options.Provider != nil
}

// Checks whether any resource within the program nodes has a custom resource option
// in which case an import should be emitted
func requiresImportingCustomResourceOptions(programNodes []pcl.Node) bool {
	for _, node := range programNodes {
		switch node := node.(type) {
		case *pcl.Resource:
			resource := node
			if hasCustomResourceOptions(resource) {
				return true
			}
		}
	}

	return false
}

func (g *generator) genCustomResourceOptions(w io.Writer, resource *pcl.Resource) {
	g.Fgen(w, "CustomResourceOptions.builder()")
	g.Indented(func() {
		g.genNewline(w)
		if resource.Options.Provider != nil {
			g.genIndent(w)
			g.Fgenf(w, ".provider(%v)", resource.Options.Provider)
			g.genNewline(w)
		}
		if resource.Options.Protect != nil {
			g.genIndent(w)
			g.Fgenf(w, ".protect(%v)", resource.Options.Protect)
			g.genNewline(w)
		}
		if resource.Options.Parent != nil {
			g.genIndent(w)
			g.Fgenf(w, ".parent(%v)", resource.Options.Parent)
			g.genNewline(w)
		}
		if resource.Options.DependsOn != nil {
			g.genIndent(w)
			g.Fgenf(w, ".dependsOn(%v)", resource.Options.DependsOn)
			g.genNewline(w)
		}
		if resource.Options.IgnoreChanges != nil {
			g.genIndent(w)
			g.Fgen(w, ".ignoreChanges(")
			switch resource.Options.IgnoreChanges.(type) {
			case *model.TupleConsExpression:
				// when we have a list of expressions
				// write each one of them between quotes
				ignoredChanges := resource.Options.IgnoreChanges.(*model.TupleConsExpression)
				for index, ignoredChange := range ignoredChanges.Expressions {
					g.Fgenf(w, "\"%v\"", ignoredChange)

					// write a comma between elements
					// if we did not reach last expression
					if index != len(ignoredChanges.Expressions)-1 {
						g.Fgen(w, ", ")
					}
				}
			default:
				// ignored changes expression was NOT a list
				// which is not really expected here
				// we will write the expression as-is anyway
				g.Fgenf(w, "\"%v\"", resource.Options.IgnoreChanges)
			}

			g.Fgen(w, ")")
			g.genNewline(w)
		}
		g.genIndent(w)
		g.Fgen(w, ".build()")
	})
}

func typedResourceProperties(resource *pcl.Resource) map[string]schema.Type {
	resourceProperties := map[string]schema.Type{}
	resourceSchema := resource.Schema
	if resourceSchema != nil && resourceSchema.InputProperties != nil {
		for _, property := range resourceSchema.InputProperties {
			if property != nil && property.Type != nil {
				resourceProperties[property.Name] = codegen.UnwrapType(property.Type)
			}
		}
	}

	return resourceProperties
}

func (g *generator) genResource(w io.Writer, resource *pcl.Resource) {
	resourceTypeName := resourceTypeName(resource)
	resourceArgs := resourceArgsTypeName(resource)
	variableName := names.LowerCamelCase(names.MakeValidIdentifier(resource.Name()))
	instantiate := func(resName string) {
		resourceProperties := typedResourceProperties(resource)
		if len(resource.Inputs) == 0 && !hasCustomResourceOptions(resource) {
			g.Fgenf(w, "new %s(%s)", resourceTypeName, resName)
		} else if len(resource.Inputs) == 0 && hasCustomResourceOptions(resource) {
			// generate empty resource args in this case
			emptyArgs := resourceArgs + ".Empty"
			g.Fgenf(w, "new %s(%s, %s, ", resourceTypeName, resName, emptyArgs)
			g.genCustomResourceOptions(w, resource)
			g.Fgen(w, ")")
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

				if !hasCustomResourceOptions(resource) {
					g.Fgenf(w, "%s.build())", g.Indent)
				} else {
					g.Fgenf(w, "%s.build(), ", g.Indent)
					g.genCustomResourceOptions(w, resource)
					g.Fgen(w, ")")
				}
			})
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
					switch firstArg := firstArg.(type) {
					case *model.ScopeTraversalExpression:
						traversalExpr := firstArg
						if len(traversalExpr.Parts) == 2 {
							// Meaning here we have {root}.{part} expression which the most common
							// check whether {root} is actually a variable name that holds the result
							// of a function invoke
							if functionSchema, isInvoke := g.functionInvokes[traversalExpr.RootName]; isInvoke {
								resultTypeName := names.LowerCamelCase(typeName(functionSchema.Outputs))
								part := getTraversalKey(traversalExpr.Traversal.SimpleSplit().Rel)
								g.genIndent(w)
								g.Fgenf(w, "final var %s = ", resource.Name())
								g.Fgenf(w, "%s.applyValue(%s -> {\n", traversalExpr.RootName, resultTypeName)
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
	switch value := value.(type) {
	case *model.FunctionCallExpression:
		call := value
		switch call.Name {
		case pcl.Invoke:
			functionSchema, foundFunction := g.findFunctionSchema(call.Args[0])
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
		// TODO: lowerExpression isn't what we expect: function call should extract outputs into .apply(...) calls
		// functionDefinitionWithApplies := g.lowerExpression(functionDefinition, localVariable.Definition.Value.Type())
		g.Fgenf(w, "final var %s = %v;\n", variableName, localVariable.Definition.Value)
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

func (g *generator) warnf(location *hcl.Range, reason string, args ...interface{}) {
	g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
		Severity: hcl.DiagWarning,
		Summary:  fmt.Sprintf(reason, args...),
		Subject:  location,
	})
}
