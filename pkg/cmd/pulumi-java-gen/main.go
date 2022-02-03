package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

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
	var stream io.ReadCloser
	if strings.HasPrefix(path, "http") {
		resp, err := http.Get(path)
		if err != nil {
			return nil, err
		}
		stream = resp.Body
	} else {
		jsonFile, err := os.Open(path)
		if err != nil {
			return nil, err
		}
		stream = jsonFile
	}
	defer stream.Close()
	dec := json.NewDecoder(stream)
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
