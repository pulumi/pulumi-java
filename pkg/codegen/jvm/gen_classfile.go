package jvm

import (
	"bytes"
	"fmt"
	"io"

	"github.com/pulumi/pulumi-java/pkg/codegen/jvm/names"
)

type classFileContext struct {
	writer    io.Writer
	imports   *names.Imports
	pkg       names.FQN
	className names.Ident
}

func (ctx *classFileContext) classFQN() names.FQN {
	return ctx.pkg.Dot(ctx.className)
}

func (ctx *classFileContext) ref(name names.FQN) string {
	return ctx.imports.Ref(name)
}

func genClassFile(
	pkg names.FQN,
	className names.Ident,
	generator func(ctx *classFileContext) error,
) (string, error) {

	imports := names.NewImports(pkg, className)
	var buf bytes.Buffer
	ctx := &classFileContext{&buf, imports, pkg, className}
	err := generator(ctx)
	packageCode := imports.PackageCode()
	importCode := imports.ImportCode()

	code := fmt.Sprintf("%s\n\n%s\n\n%s",
		packageCode,
		importCode,
		buf.String())

	return code, err
}
