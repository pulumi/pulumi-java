package main

import (
	"fmt"

	jvmgen "github.com/pulumi/pulumi-java/pkg/codegen/jvm"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func main() {
	externalLanguageMain(emitFiles)
}

func emitFiles(pkgSpec *pschema.PackageSpec) (map[string][]byte, error) {
	extraFiles := map[string][]byte{}

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return map[string][]byte{}, fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	// TODO overlays
	tfgen := "the Pulumi Terraform Bridge (tfgen) Tool"
	return jvmgen.GeneratePackage(tfgen, pkg, extraFiles)
}
