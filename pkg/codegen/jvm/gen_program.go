package jvm

import (
	"bytes"
	"fmt"
	"io"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model"
	"github.com/pulumi/pulumi/pkg/v3/codegen/hcl2/model/format"
)

type generator struct {
	// The formatter to use when generating code.
	*format.Formatter
	program *hcl2.Program
	// Whether awaits are needed, and therefore an async Initialize method should be declared.
	asyncInit bool
	// TODO
	diagnostics hcl.Diagnostics
}

func GenerateProgram(program *hcl2.Program) (map[string][]byte, hcl.Diagnostics, error) {
	// Linearize the nodes into an order appropriate for procedural code generation.
	nodes := hcl2.Linearize(program)

	// Import Java-specific schema info.
	// FIXME

	g := &generator{
		program: program,
	}
	g.Formatter = format.NewFormatter(g)

	for _, n := range nodes {
		if r, ok := n.(*hcl2.Resource); ok && requiresAsyncInit(r) {
			g.asyncInit = true
			break
		}
	}

	var index bytes.Buffer
	g.genPreamble(&index, program)

	g.Indented(func() {
		// Emit async Initialize if needed
		if g.asyncInit {
			g.genInitialize(&index, nodes)
		}

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

// TODO

// genPreamble generates using statements, class definition and constructor.
func (g *generator) genPreamble(w io.Writer, program *hcl2.Program) {
	// TODO

	g.Fprint(w, "\n")

	// Emit Stack class signature
	g.Fprint(w, "public class MyStack extends Stack\n")
	g.Fprint(w, "{\n")
	g.Fprint(w, "    public MyStack()\n")
	g.Fprint(w, "    {\n")
}

// genInitialize generates the declaration and the call to the async Initialize method, and also fills stack
// outputs from the initialization result.
func (g *generator) genInitialize(w io.Writer, nodes []hcl2.Node) {
	// TODO
}

// genPostamble closes the method and the class and declares stack output statements.
func (g *generator) genPostamble(w io.Writer, nodes []hcl2.Node) {
	// TODO
	g.Fprint(w, "}\n")
}

func (g *generator) genNode(w io.Writer, n hcl2.Node) {
	// TODO
}

// requiresAsyncInit returns true if the program requires awaits in the code, and therefore an asynchronous
// method must be declared.
func requiresAsyncInit(r *hcl2.Resource) bool {
	if r.Options == nil || r.Options.Range == nil {
		return false
	}

	return model.ContainsPromises(r.Options.Range.Type())
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
