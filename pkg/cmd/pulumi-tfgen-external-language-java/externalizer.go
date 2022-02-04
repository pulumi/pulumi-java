// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"

	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

// Inlines tfgen.ExternalLanguageMain from
// "github.com/pulumi/pulumi-terraform-bridge/v3/pkg/tfgen"
//
// Currently we cannot link that in because it yields a go mod issue:
//
// github.com/pulumi/pulumi-java/pkg/cmd/pulumi-tfgen-external-language-java imports
//      github.com/pulumi/pulumi-java/pkg/codegen/jvm imports
//      github.com/pulumi/pulumi/pkg/v3/codegen/hcl2: module github.com/pulumi/pulumi/pkg/v3@latest found (v3.23.2),1
//      but does not contain package github.com/pulumi/pulumi/pkg/v3/codegen/hcl2

func externalLanguageMain(emitFiles func(*pschema.PackageSpec) (map[string][]byte, error)) {
	operation := flag.String("operation", "", "operation to perform, typically emitFiles")
	flag.Parse()
	switch *operation {
	case "emitFiles":
		var pulumiPackageSpec *pschema.PackageSpec
		err := json.NewDecoder(os.Stdin).Decode(&pulumiPackageSpec)
		if err != nil {
			log.Fatal(fmt.Errorf("Cannot parse PackageSpec from JSON: %w", err))
		}
		files, err := emitFiles(pulumiPackageSpec)
		if err != nil {
			log.Fatal(err)
		}
		if err := json.NewEncoder(os.Stdout).Encode(files); err != nil {
			log.Fatal(err)
		}
	default:
		log.Fatal(fmt.Errorf("Required -operation, supported values: [emitFiles]"))
	}
}
