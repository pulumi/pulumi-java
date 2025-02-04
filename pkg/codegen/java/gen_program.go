// Copyright 2022, Pulumi Corporation.  All rights reserved.

//nolint:goconst
package java

import (
	"bytes"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"slices"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/syntax"
	"golang.org/x/exp/maps"

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
	functionInvokes          map[string]*schema.Function
	emittedTypeImportSymbols codegen.StringSet
}

// genComment generates a comment into the output.
func (g *generator) genComment(w io.Writer, comment syntax.Comment) {
	for _, l := range comment.Lines {
		g.Fgenf(w, "%s//%s\n", g.Indent, l)
	}
}

// genTrivia generates the list of trivia associated with a given token.
func (g *generator) genTrivia(w io.Writer, token syntax.Token) {
	for _, t := range token.LeadingTrivia {
		if c, ok := t.(syntax.Comment); ok {
			g.genComment(w, c)
		}
	}
	for _, t := range token.TrailingTrivia {
		if c, ok := t.(syntax.Comment); ok {
			g.genComment(w, c)
		}
	}
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

		contract.Assertf(len(diags) == 0, "unexpected diagnostics: %v", diags)
	}

	return foundRangeCall
}

// inspectFunctionCall visits the provided nodes and calls the inspect function
// for every function call expression it encounters.
func inspectFunctionCall(nodes []pcl.Node, inspect func(*model.FunctionCallExpression)) {
	for _, node := range nodes {
		diags := node.VisitExpressions(model.IdentityVisitor, func(x model.Expression) (model.Expression, hcl.Diagnostics) {
			// Ignore the node if it is not a function call.
			call, ok := x.(*model.FunctionCallExpression)
			if !ok {
				return x, nil
			}

			inspect(call)
			return x, nil
		})

		contract.Assertf(len(diags) == 0, "unexpected diagnostics: %v", diags)
	}
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
		program:                  program,
		functionInvokes:          map[string]*schema.Function{},
		emittedTypeImportSymbols: codegen.NewStringSet(),
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

func GenerateProject(
	directory string,
	project workspace.Project,
	program *pcl.Program,
	localDependencies map[string]string,
) error {
	files, diagnostics, err := GenerateProgram(program)
	if err != nil {
		return err
	}
	if diagnostics.HasErrors() {
		return diagnostics
	}

	rootDirectory := directory
	if project.Main != "" {
		directory = filepath.Join(rootDirectory, project.Main)
		// mkdir -p the subdirectory
		err = os.MkdirAll(directory, 0o700)
		if err != nil {
			return fmt.Errorf("create main directory: %w", err)
		}
	}

	// Set the runtime to "java" then marshal to Pulumi.yaml
	project.Runtime = workspace.NewProjectRuntimeInfo("java", nil)
	projectBytes, err := encoding.YAML.Marshal(project)
	if err != nil {
		return err
	}

	filesWithPackages := make(map[string][]byte)

	filesWithPackages[filepath.Join(rootDirectory, "Pulumi.yaml")] = projectBytes

	for fileName, fileContents := range files {
		if fileName == "MyStack.java" {
			fileName = "App.java"
		}
		fileWithPackage := filepath.Join(directory, "src", "main", "java", "generated_program", fileName)
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

		// Skip the pulumi package itself, as that is already included by default
		// and listing it twice leads to build errors.
		if packageName == "pulumi" {
			continue
		}

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

	repositories := make(map[string]bool)

	// If no version is specified for the pulumi package, use the default SDK version. In either case, presently we emit
	// a "soft" dependency requirement, which essentially means that it will be used if no other preference is expressed
	// in the dependency tree. See https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification for
	// more information.
	coreSDKVersion := DefaultSdkVersion.String()
	for name, dep := range localDependencies {
		parts := strings.Split(dep, ":")
		if len(parts) < 3 {
			return fmt.Errorf(
				"invalid dependency for %s %s; must be of the form groupId:artifactId:version[:repositoryPath]",
				name, dep,
			)
		}

		if name == "pulumi" {
			coreSDKVersion = parts[2]
		}

		if len(parts) == 4 {
			repositories[parts[3]] = true
		}
	}

	repositoryURLs := maps.Keys(repositories)
	slices.Sort(repositoryURLs)

	var repositoriesXML bytes.Buffer
	if len(repositoryURLs) > 0 {
		repositoriesXML.WriteString(`
			<repositories>`)

		for id, url := range repositoryURLs {
			repositoriesXML.WriteString(fmt.Sprintf(`
				<repository>
					<id>repository-%d</id>
					<url>file://%s</url>
				</repository>`, id, url))
		}

		repositoriesXML.WriteString(`
			</repositories>
`)
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
			%s
			<dependencies>
				<dependency>
					<groupId>com.pulumi</groupId>
					<artifactId>pulumi</artifactId>
					<version>%s</version>
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
		project.Name.String(),
		repositoriesXML.String(),
		coreSDKVersion,
		mavenDependenciesXML.String(),
	))
	filesWithPackages[filepath.Join(directory, "pom.xml")] = mavenPomXML.Bytes()

	for filePath, data := range filesWithPackages {
		dir := filepath.Dir(filePath)
		err := os.MkdirAll(dir, os.ModePerm)
		if err != nil {
			return fmt.Errorf("could not create output directory %s: %w", dir, err)
		}
		err = os.WriteFile(filePath, data, 0o600)
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
	if pkg == "pulumi" && module == "pulumi" && (member == "StackReference" || member == "StackReferenceArgs") {
		return "com.pulumi.resources." + member
	}
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

func literalExprText(expr model.Expression) (string, bool) {
	if lit, ok := expr.(*model.LiteralValueExpression); ok {
		if lit.Value.IsNull() {
			return "", false
		}
		return lit.Value.AsString(), true
	}

	if templateExpr, ok := expr.(*model.TemplateExpression); ok {
		if len(templateExpr.Parts) == 1 {
			return literalExprText(templateExpr.Parts[0])
		}
	}

	return "", false
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
		switch innerObject := property.Value.(type) {
		case *model.ObjectConsExpression:
			if innerObjectKey, ok := literalExprText(property.Key); ok {
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

func (g *generator) collectResourceImports(resource *pcl.Resource) []string {
	imports := make([]string, 0)
	pkg, module, name, _ := resource.DecomposeToken()
	resourceImport := pulumiImport(pkg, module, name)
	imports = append(imports, resourceImport)
	if len(resource.Inputs) > 0 || hasCustomResourceOptions(resource) {
		// import args type name
		argsTypeName := g.resourceArgsTypeName(resource)
		alreadyFullyQualified := strings.Contains(argsTypeName, ".")
		if !alreadyFullyQualified {
			resourceArgsImport := pulumiImport(pkg, module, argsTypeName)
			imports = append(imports, resourceArgsImport)
		} else {
			imports = append(imports, argsTypeName)
		}
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
	token, ok := literalExprText(tokenArg)
	if !ok {
		return "", ""
	}
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
	case "stack", "project", "organization":
		// stack(), project(), and organization() functions are pulumi built-ins
		// they require the Deployment class
		imports = append(imports, "com.pulumi.deployment.Deployment")
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

	visitFunctionCalls := func(expr model.Expression) (model.Expression, hcl.Diagnostics) {
		switch expr := expr.(type) {
		case *model.FunctionCallExpression:
			imports = append(imports, g.collectFunctionCallImports(expr)...)
		}
		return expr, nil
	}

	for _, node := range nodes {
		switch node := node.(type) {
		case *pcl.Resource:
			// collect resource imports
			resource := node
			imports = append(imports, g.collectResourceImports(resource)...)
			for _, prop := range resource.Inputs {
				_, diags := model.VisitExpression(prop.Value, model.IdentityVisitor, visitFunctionCalls)
				g.diagnostics = append(g.diagnostics, diags...)
			}
		case *pcl.LocalVariable:
			localVariable := node
			_, diags := model.VisitExpression(localVariable.Definition.Value, model.IdentityVisitor, visitFunctionCalls)
			g.diagnostics = append(g.diagnostics, diags...)
		case *pcl.OutputVariable:
			outputVariable := node
			_, diags := model.VisitExpression(outputVariable.Value, model.IdentityVisitor, visitFunctionCalls)
			g.diagnostics = append(g.diagnostics, diags...)
		}
	}

	return removeDuplicates(imports)
}

// checks whether the input expression is an object that has a property "dependsOn"
func containsDependsOnInvokeOption(expr model.Expression) bool {
	if invokeOptions, ok := expr.(*model.ObjectConsExpression); ok {
		for _, item := range invokeOptions.Items {
			if key, ok := literalExprText(item.Key); ok && key == "dependsOn" {
				return true
			}
		}
	}

	return false
}

// genPreamble generates import statements, main class and stack definition.
func (g *generator) genPreamble(w io.Writer, nodes []pcl.Node) {
	javaStringType := "String"

	g.Fgen(w, "package generated_program;")
	g.genNewline(w)
	g.genNewline(w)
	g.genImport(w, "com.pulumi.Context")
	g.genImport(w, "com.pulumi.Pulumi")
	g.genImport(w, "com.pulumi.core.Output")

	imports := g.collectImports(nodes)

	importMember := func(importDef string) string {
		return importDef[strings.LastIndex(importDef, ".")+1:]
	}

	// a map to keep track of member names of imports
	// e.g. ResourceType -> [
	//     com.pulumi.my_package.ResourceType
	//     com.pulumi.your_package.ResourceType
	// ]
	importsByMember := map[string]codegen.StringSet{}
	for _, importDef := range imports {
		importMember := importMember(importDef)
		if _, ok := importsByMember[importMember]; !ok {
			// initialize the set
			importsByMember[importMember] = codegen.NewStringSet()
		}

		importsByMember[importMember].Add(importDef)
	}

	// if we have two imports such as the following:
	// - com.pulumi.my_package.ResourceType
	// - com.pulumi.your_package.ResourceType
	// then we shouldn't generate import statements for them
	// instead, we use the fully qualified name at the usage site
	importsWithDuplicateName := codegen.NewStringSet()
	emittedTypeImportSymbols := codegen.NewStringSet()
	for _, importDef := range imports {
		if len(importsByMember[importMember(importDef)]) > 1 {
			importsWithDuplicateName.Add(importDef)
		} else {
			emittedTypeImportSymbols.Add(importDef)
		}
	}

	// Write out the specific imports from used nodes
	for _, importDef := range imports {
		if strings.HasSuffix(importDef, ".String") {
			// A type named `String` is being imported so we need to fully qualify our
			// use of the built-in `java.lang.String` type to avoid the conflict.
			javaStringType = "java.lang.String"
		}

		if emittedTypeImportSymbols.Has(importDef) {
			// do not generate import statements for symbols with duplicate members
			// because those will require full qualification at the usage site
			g.genImport(w, importDef)
		}
	}

	g.emittedTypeImportSymbols = emittedTypeImportSymbols

	functionImports := codegen.NewStringSet()

	inspectFunctionCall(nodes, func(call *model.FunctionCallExpression) {
		switch call.Name {
		case "toJSON":
			functionImports.Add("static com.pulumi.codegen.internal.Serialization.*")
		case "readDir":
			functionImports.Add("static com.pulumi.codegen.internal.Files.readDir")
		case "fileAsset":
			functionImports.Add("com.pulumi.asset.FileAsset")
		case "fileArchive":
			functionImports.Add("com.pulumi.asset.FileArchive")
		case "stringAsset":
			functionImports.Add("com.pulumi.asset.StringAsset")
		case "remoteAsset":
			functionImports.Add("com.pulumi.asset.RemoteAsset")
		case "assetArchive":
			functionImports.Add("com.pulumi.asset.AssetArchive")
		case pcl.Invoke:
			hasInvokeOptions := len(call.Args) == 3
			if hasInvokeOptions {
				if containsDependsOnInvokeOption(call.Args[2]) {
					// for invoke output options, instantiate the builder from its parent class
					// i.e. (new InvokeOutputOptions.Builder()).dependsOn(resource).build()
					functionImports.Add("com.pulumi.deployment.InvokeOutputOptions")
				} else {
					functionImports.Add("com.pulumi.deployment.InvokeOptions")
				}
			}
		}
	})

	for _, functionImport := range functionImports.SortedValues() {
		g.genImport(w, functionImport)
	}

	if containsRangeExpr(nodes) {
		// import the KeyedValue<T> class
		g.genImport(w, "com.pulumi.codegen.internal.KeyedValue")
	}

	if requiresImportingCustomResourceOptions(nodes) {
		g.genImport(w, "com.pulumi.resources.CustomResourceOptions")
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
	g.Fprintf(w, "    public static void main(%s[] args) {\n", javaStringType)
	g.Fgen(w, "        Pulumi.run(App::stack);\n")
	g.Fgen(w, "    }\n")
	g.genNewline(w)
	g.Fprint(w, "    public static void stack(Context ctx) {\n")
	if containConfigVariables(nodes) {
		g.Fprint(w, "        final var config = ctx.config();\n")
	}
}

// genPostamble closes the method and the class and declares stack output statements.
func (g *generator) genPostamble(w io.Writer, _ []pcl.Node) {
	g.Indented(func() {
		g.genIndent(w)
		g.Fgen(w, "}\n")
	})
	g.Fprint(w, "}\n")
}

// resourceTypeName computes the Java resource class name for the given resource.
func (g *generator) resourceTypeName(resource *pcl.Resource) string {
	// Compute the resource type from the Pulumi type token.
	pkg, module, member, diags := resource.DecomposeToken()
	contract.Assertf(len(diags) == 0, "failed to decompose resource token: %v", diags)
	if pkg == pulumiToken && module == providersToken {
		member = "Provider"
	}

	if !g.emittedTypeImportSymbols.Has(pulumiImport(pkg, module, member)) {
		// if we didn't emit an import statement for this symbol
		// it means that there was a duplicate member name in the imports
		// so return the fully qualified resource name
		return pulumiImport(pkg, module, member)
	}

	return names.Title(member)
}

// resourceArgsTypeName computes the Java arguments class name for the given resource.
func (g *generator) resourceArgsTypeName(r *pcl.Resource) string {
	return fmt.Sprintf("%sArgs", g.resourceTypeName(r))
}

// Returns the expression that should be emitted for a resource's "name" parameter given its base name
func makeResourceName(baseName string, suffix string) string {
	if suffix == "" {
		return fmt.Sprintf(`"%s"`, baseName)
	}
	return fmt.Sprintf(`"%s-"`, baseName) + " + " + suffix
}

func (g *generator) findFunctionSchema(token model.Expression) (*schema.Function, bool) {
	tk, validToken := literalExprText(token)
	if !validToken {
		return nil, false
	}

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
		resource.Options.RetainOnDelete != nil ||
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
		if resource.Options.RetainOnDelete != nil {
			g.genIndent(w)
			g.Fgenf(w, ".retainOnDelete(%v)", resource.Options.RetainOnDelete)
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
	resourceTypeName := g.resourceTypeName(resource)
	resourceArgs := g.resourceArgsTypeName(resource)
	variableName := names.LowerCamelCase(names.MakeValidIdentifier(resource.Name()))
	g.genTrivia(w, resource.Definition.Tokens.GetType(""))
	for _, l := range resource.Definition.Tokens.GetLabels(nil) {
		g.genTrivia(w, l)
	}
	g.genTrivia(w, resource.Definition.Tokens.GetOpenBrace())

	if resource.Schema != nil {
		for _, input := range resource.Inputs {
			// We traverse the set of resource input types to make sure that this attribute appears in the schema.
			// However, we'll use the type we've already computed rather than the result of the traversal since the
			// latter will typically be a union of the type we've computed and one or more output types. This may result
			// in inaccurate code generation later on. Arguably this is a bug in the generator, but this will have to do
			// for now.
			targetType, diagnostics := resource.InputType.Traverse(hcl.TraverseAttr{Name: input.Name})
			g.diagnostics = append(g.diagnostics, diagnostics...)
			value := g.lowerExpression(input.Value, targetType.(model.Type))
			input.Value = value
		}
	}

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
			g.Fgen(w, "\n")
			g.Indented(func() {
				for _, attr := range resource.Inputs {
					attributeIdent := names.MakeValidIdentifier(attr.Name)
					attributeSchemaType := resourceProperties[attr.Name]
					g.currentResourcePropertyType = attributeSchemaType
					g.Fgenf(w, "%s.%s(%.v)\n", g.Indent, attributeIdent, attr.Value)
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
			switch expr := resource.Options.Range.(type) {
			case *model.ScopeTraversalExpression:
				traversalExpr := expr
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
							g.Fgenf(w, "%sfor (var range : KeyedValue.of(%s.%s())) {\n", g.Indent, resultTypeName, part)
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

	g.genTrivia(w, resource.Definition.Tokens.GetCloseBrace())
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
	g.genTrivia(w, localVariable.Definition.Tokens.Name)
	variableName := localVariable.Name()
	functionSchema, isInvokeCall := g.isFunctionInvoke(localVariable)
	g.genIndent(w)
	if isInvokeCall {
		g.functionInvokes[variableName] = functionSchema
		invokeCall := localVariable.Definition.Value.(*model.FunctionCallExpression)
		functionDefinitionWithApplies := g.lowerExpression(invokeCall, invokeCall.Signature.ReturnType)
		g.Fgenf(w, "final var %s = %v;\n", variableName, functionDefinitionWithApplies)
	} else {
		variable := localVariable.Definition.Value
		g.Fgenf(w, "final var %s = %v;\n", variableName, g.lowerExpression(variable, variable.Type()))
	}
	g.genNewline(w)
}

func (g *generator) genOutputAssignment(w io.Writer, outputVariable *pcl.OutputVariable) {
	g.genIndent(w)
	targetType := outputVariable.Value.Type()
	if _, ok := targetType.(*model.OutputType); !ok {
		targetType = model.NewOutputType(targetType)
	}
	rewrittenOutVar := g.lowerExpression(outputVariable.Value, targetType)
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
		Severity: hcl.DiagWarning,
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
