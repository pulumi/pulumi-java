package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"

	jvmgen "github.com/pulumi/pulumi-java/pkg/codegen/jvm"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func main() {
	schema := flag.String("schema", "", "path to schema.json")
	out := flag.String("out", "", "output directory")
	flag.Parse()

	if *schema == "" {
		fmt.Printf("-schema is parameter required")
		flag.Usage()
		os.Exit(1)
	}

	if *out == "" {
		fmt.Printf("-out is parameter required")
		flag.Usage()
		os.Exit(1)
	}

	if err := generateJava(*out, *schema); err != nil {
		log.Fatal(err)
	}
}

func readPackageSchema(path string) (*pschema.PackageSpec, error) {
	jsonFile, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer jsonFile.Close()

	dec := json.NewDecoder(jsonFile)

	var result pschema.PackageSpec
	if err := dec.Decode(&result); err != nil {
		return nil, err
	}
	return &result, nil
}

func generateJava(rootDir, schemaFile string) error {
	pkgSpec, err := readPackageSchema(schemaFile)
	if err != nil {
		return fmt.Errorf("failed to read schema from %s: %w", schemaFile, err)
	}

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	// TODO handle overlays here?
	extraFiles := map[string][]byte{}
	files, err := jvmgen.GeneratePackage("pulumi-java-gen", pkg, extraFiles)
	if err != nil {
		return err
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

	return nil
}
