// Copyright 2024, Pulumi Corporation.  All rights reserved.

package java

import (
	"testing"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/stretchr/testify/require"
)

func TestDeduplicateTypes(t *testing.T) {
	cases := []struct {
		name          string
		input         *schema.PackageSpec
		expectedSpec  *schema.PackageSpec
		expectedDiags hcl.Diagnostics
	}{
		{
			name:          "no duplicates",
			input:         &schema.PackageSpec{},
			expectedSpec:  &schema.PackageSpec{},
			expectedDiags: hcl.Diagnostics{},
		},
		{
			name: "duplicates, lowercase",
			input: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:ipallocationmethod": {},
					"azure-native:network:IpAllocationMethod": {},
				},
			},
			expectedSpec: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:IpAllocationMethod": {},
				},
			},
			expectedDiags: hcl.Diagnostics{
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:ipallocationmethod' to " +
						"'azure-native:network:IpAllocationMethod' in the schema",
				},
			},
		},
		{
			name: "duplicates, uppercase",
			input: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:IPAllocationMethod": {},
					"azure-native:network:IpAllocationMethod": {},
				},
			},
			expectedSpec: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:IPAllocationMethod": {},
				},
			},
			expectedDiags: hcl.Diagnostics{
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:IpAllocationMethod' to " +
						"'azure-native:network:IPAllocationMethod' in the schema",
				},
			},
		},
		{
			name: "multiple duplicates",
			input: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:ipallocationmethod": {},
					"azure-native:network:IPAllocationMethod": {},
					"azure-native:network:IpAllocationMethod": {},
				},
			},
			expectedSpec: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:IPAllocationMethod": {},
				},
			},
			expectedDiags: hcl.Diagnostics{
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:ipallocationmethod' to " +
						"'azure-native:network:IPAllocationMethod' in the schema",
				},
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:IpAllocationMethod' to " +
						"'azure-native:network:IPAllocationMethod' in the schema",
				},
			},
		},
		{
			name: "multiple duplicates and non-duplicates",
			input: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:other":               {},
					"azure-native:network:ipsallocationmethod": {},
					"azure-native:network:ip_allocationmethod": {},
					"azure-native:network:ipallocationmethod":  {},
					"azure-native:network:IPAllocationMethod":  {},
					"azure-native:network:IpAllocationMethod":  {},
				},
			},
			expectedSpec: &schema.PackageSpec{
				Types: map[string]schema.ComplexTypeSpec{
					"azure-native:network:other":               {},
					"azure-native:network:ipsallocationmethod": {},
					"azure-native:network:ip_allocationmethod": {},
					"azure-native:network:IPAllocationMethod":  {},
				},
			},
			expectedDiags: hcl.Diagnostics{
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:ipallocationmethod' to " +
						"'azure-native:network:IPAllocationMethod' in the schema",
				},
				&hcl.Diagnostic{
					Severity: hcl.DiagWarning,
					Summary: "Renaming 'azure-native:network:IpAllocationMethod' to " +
						"'azure-native:network:IPAllocationMethod' in the schema",
				},
			},
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			actualSpec, actualDiags, err := DeduplicateTypes(c.input)
			require.NoError(t, err)
			require.ElementsMatch(t, c.expectedDiags, actualDiags)
			require.Equal(t, c.expectedSpec, actualSpec)
		})
	}
}
