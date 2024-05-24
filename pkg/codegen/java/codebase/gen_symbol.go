package codebase

import (
	"fmt"
	"strings"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func (g *generator) resourceSymbols(r *pcl.Resource) (Symbol, Symbol) {
	pkg, module, member, diagnostics := r.DecomposeToken()
	if len(diagnostics) > 0 {
		g.diagnostics = append(g.diagnostics, diagnostics...)
	}

	if pkg == pulumiToken && module == providersToken {
		member = "Provider"
	}

	javaPkg := getJavaPackage(pkg, module)

	resourceSymbol := g.compilationUnit.Import(NewSymbol(javaPkg, TitleCase(member)))
	resourceArgsSymbol := g.compilationUnit.Import(NewSymbol(javaPkg, fmt.Sprintf("%sArgs", TitleCase(member))))

	return resourceSymbol, resourceArgsSymbol
}

func (g *generator) objectTypeSymbol(
	t *schema.ObjectType,
	isInputShape bool,
) Symbol {
	parts := strings.Split(t.Token, ":")
	if len(parts) != 3 {
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  "Expected object type token to have three parts: %v",
		})
	}

	pkg, module, name := parts[0], parts[1], parts[2]
	javaPkg := getJavaPackage(pkg, module)

	casedName := TitleCase(name)
	if isInputShape {
		if !strings.HasSuffix(name, "Args") {
			casedName += "Args"
		}

		javaPkg = fmt.Sprintf("%s.inputs", javaPkg)
	}

	sym := NewSymbol(javaPkg, casedName)
	return g.compilationUnit.Import(sym)
}

func (g *generator) functionSymbol(e model.Expression) Symbol {
	token, ok := expressionString(e)
	if !ok {
		g.diagnostics = append(g.diagnostics, &hcl.Diagnostic{
			Severity: hcl.DiagError,
			Summary:  fmt.Sprintf("Function symbols must be expressible as strings: %v", e),
			Subject:  e.SyntaxNode().Range().Ptr(),
		})
	}

	pkg, module, member, diagnostics := pcl.DecomposeToken(token, e.SyntaxNode().Range())
	if len(diagnostics) > 0 {
		g.diagnostics = append(g.diagnostics, diagnostics...)
	}

	javaPkg := getJavaPackage(pkg, module)

	var cls string
	if ignoreModule(module) {
		cls = TitleCase(pkg) + "Functions"
	} else {
		cls = TitleCase(module) + "Functions"
	}

	sym := NewSymbol(javaPkg, cls)
	return g.compilationUnit.ImportStatic(sym, member)
}

func getJavaPackage(pkg string, module string) string {
	module = cleanModule(module)
	if ignoreModule(module) {
		return fmt.Sprintf("com.pulumi.%s", sanitizeImport(pkg))
	}
	return fmt.Sprintf("com.pulumi.%s.%s", sanitizeImport(pkg), sanitizeImport(module))
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
