// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package generative

import (
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"pgregory.net/rapid"

	jvmgen "github.com/pulumi/pulumi-java/pkg/codegen/jvm"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func generatesCompilingJavaCode(rootDir string, pkgSpec pschema.PackageSpec) error {
	pkg, err := pschema.ImportSpec(pkgSpec, nil /*languages*/)
	if err != nil {
		return fmt.Errorf("pchema.ImportSpec failed: %w", err)
	}

	// TODO overlays
	tfgen := "the Pulumi Terraform Bridge (tfgen) Tool"
	files, err := jvmgen.GeneratePackage(tfgen, pkg, nil)
	if err != nil {
		return fmt.Errorf("jvmgen.GeneratePackage failed: %w", err)
	}

	for f, bytes := range files {
		path := filepath.Join(rootDir, f)

		if err := os.MkdirAll(filepath.Dir(path), os.ModePerm); err != nil {
			return fmt.Errorf("os.MkdirAll failed: %w", err)
		}

		if err := ioutil.WriteFile(path, bytes, 0600); err != nil {
			return fmt.Errorf("ioutil.WriteFile failed: %w", err)
		}
	}

	cmd := exec.Command("gradle", "build")
	cmd.Dir = rootDir

	if err := cmd.Run(); err != nil {
		return fmt.Errorf("gradle build failed: %w", err)
	}

	// for f := range files {
	// 	fmt.Printf("Generated file %s", f)
	// }
	return nil
}

func TestRandomSchemasGenerateCompilingJavaCode(t *testing.T) {
	tempdir := "tmp"

	gen := &schemaGenerators{}
	packageSpecGen := gen.PackageSpec()
	n := 0
	rapid.Check(t, func(t *rapid.T) {
		n = n + 1
		dir := filepath.Join(tempdir, fmt.Sprintf("%d", n))
		t.Logf("dir = %s", dir)
		packageSpec := packageSpecGen.Draw(t, "packageSpec").(pschema.PackageSpec)
		err := generatesCompilingJavaCode(dir, packageSpec)
		if err != nil {
			t.Fatalf("failed to compile java code: %v", err)
		}
	})
}
