// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package generative

import (
	"testing"

	"pgregory.net/rapid"

	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func TestUniqueTokens(t *testing.T) {
	gen := &schemaGenerators{}
	packageSpecGen := gen.PackageSpec()
	rapid.Check(t, func(t *rapid.T) {
		packageSpec := packageSpecGen.Draw(t, "packageSpec").(pschema.PackageSpec)
		all := map[string]bool{}
		for k := range packageSpec.Resources {
			_, dup := all[k]
			if dup {
				t.Fatalf("Duplicate token: %s", k)
			}
			all[k] = true
		}

	})
}
