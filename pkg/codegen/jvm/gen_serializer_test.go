// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/contract"

	//"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
)

// This test explore generated type mappings and serializers.
//
// For every possible type allowed by PackageSpec, ensure that the
// generated resource Args classes compile, that these Args classes
// can be instantiated at runtime on the JVM, and that they serialize
// to the expected protobuf Structs.
//
// The test needs JVM and Go codegen to cooperate closely and has to
// employ some tricks.
func TestGeneratedSerializers(t *testing.T) {

	tt := test.SDKTest{
		Directory:   "generated-serializers",
		Description: "Sysetmatic serializer tests.",
	}

	testDir := filepath.Join("..", "testing", "test", "testdata")
	dirPath := filepath.Join(testDir, filepath.FromSlash(tt.Directory))

	schemaJsonPath := filepath.Join(dirPath, "schema.json")
	gs := genser{}
	schema := gs.serializerTestSpec()
	if err := gs.writeSpec(schemaJsonPath, schema); err != nil {
		t.Fatal(err)
	}

	test.TestSDKCodegen(t, &test.SDKCodegenOptions{
		GenPackage: GeneratePackage,
		Language:   "jvm",
		TestCases:  []test.SDKTest{tt},
		Checks: map[string]test.CodegenCheck{
			"jvm/compile": compileGeneratedPackage,
			"jvm/test":    testGeneratedPackage,
		},
	})
}

// Helper type to not pollute the package with all the definitions
// required to generate the examples in TestGeneratedSerializers.
type genser struct{}

func (gs genser) serializerTestSpec() *schema.PackageSpec {
	res1 := schema.ResourceSpec{}
	gs.addPropSpecs(&res1)

	pkg := &schema.PackageSpec{
		Name: "generated-serializers",
		Resources: map[string]schema.ResourceSpec{
			"generated-serializers::Res1": res1,
		},
	}

	return pkg
}

func (gs genser) addPropSpecs(res *schema.ResourceSpec) {
	res.InputProperties = map[string]schema.PropertySpec{}

	counter := 0
	next := func() string {
		i := counter
		counter = counter + 1
		return fmt.Sprintf("p%d", i)
	}

	for _, t := range gs.allTypes() {
		// Adding input properties
		for _, plain := range []bool{true, false} {
			for _, required := range []bool{true, false} {
				n := next()
				p := schema.PropertySpec{TypeSpec: t}
				p.Plain = plain
				res.InputProperties[n] = p
				if required {
					res.RequiredInputs = append(res.Required, n)
				}
			}
		}
	}
}

func (gs genser) allTypes() []schema.TypeSpec {
	types := []schema.TypeSpec{}

	// add height=1 types
	for _, t := range gs.baseTypes() {
		types = append(types, t)
	}

	// add height=2 types
	for _, tt := range gs.typeTransformers() {
		for _, t := range types {
			types = append(types, tt(t))
		}
	}

	// add height=3 types
	for _, tt := range gs.typeTransformers() {
		for _, t := range types {
			types = append(types, tt(t))
		}
	}

	return types
}

func (genser) baseTypes() []schema.TypeSpec {
	return []schema.TypeSpec{
		{Type: "boolean"},
		{Type: "integer"},
		{Type: "number"},
		{Type: "string"},
		{Ref: "pulumi.json#/Any"},
		{Ref: "pulumi.json#/Archive"},
		{Ref: "pulumi.json#/Asset"},
		{Ref: "pulumi.json#/Json"},
	}
}

func (genser) typeTransformers() []func(schema.TypeSpec) schema.TypeSpec {
	makeArray := func(t schema.TypeSpec) schema.TypeSpec {
		element := t
		return schema.TypeSpec{
			Type:  "array",
			Items: &element,
		}
	}
	// TODO a lot more forms here
	return []func(schema.TypeSpec) schema.TypeSpec{
		makeArray,
	}
}

func (genser) writeSpec(path string, spec *schema.PackageSpec) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	w := bufio.NewWriter(f)
	defer func() {
		contract.IgnoreError(w.Flush())
		contract.IgnoreClose(f)
	}()
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	return enc.Encode(spec)
}
