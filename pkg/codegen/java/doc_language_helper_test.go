// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"encoding/json"
	"fmt"
	"os"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/stretchr/testify/assert"
)

func TestGetLanguageTypeString(t *testing.T) {
	schemaJSON := "../testing/test/testdata/mini-awsclassic/schema.json"
	pkg, err := readPackage(schemaJSON)
	assert.NoError(t, err)
	dlh := DocLanguageHelper{}
	for _, input := range []bool{false, true} {
		ts := dlh.GetTypeName(pkg.Reference(),
			findPlainType(pkg, "aws:config/endpoints:endpoints"),
			input, "")
		assert.Equal(t, "Endpoints", ts)
		ts2 := dlh.GetTypeName(pkg.Reference(),
			findInputType(pkg, "aws:config/endpoints:endpoints"),
			input, "")
		assert.Equal(t, "EndpointsArgs", ts2)
	}
}

func readPackage(schemaJSON string) (*schema.Package, error) {
	f, err := os.Open(schemaJSON)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	var spec schema.PackageSpec

	if err := json.NewDecoder(f).Decode(&spec); err != nil {
		return nil, err
	}

	pkg, diag, err := schema.BindSpec(spec, nil, schema.ValidationOptions{
		AllowDanglingReferences: true,
	})
	if err != nil {
		return nil, err
	}

	if diag.HasErrors() {
		return nil, fmt.Errorf("%s", diag.Error())
	}

	return pkg, nil
}

func findPlainType(pkg *schema.Package, token string) *schema.ObjectType {
	return findType(pkg, token, true)
}

func findInputType(pkg *schema.Package, token string) *schema.ObjectType {
	return findType(pkg, token, false)
}

func findType(pkg *schema.Package, token string, plainShape bool) *schema.ObjectType {
	for _, ty := range pkg.Types {
		if ot, ok := ty.(*schema.ObjectType); ok {
			if ot.Token == token && ot.IsPlainShape() == plainShape {
				return ot
			}
		}
	}
	panic("type not found")
}
