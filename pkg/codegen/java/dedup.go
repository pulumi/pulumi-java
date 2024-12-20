// Copyright 2024, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"

	"github.com/hashicorp/hcl/v2"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

// DeduplicateTypes detects multiple types in a PackageSpec whose names are the same modulo case, such as
// `azure-native:network:IpAllocationMethod` and `azure-native:network:IPAllocationMethod`, deterministically picks one
// of these names, and rewrites the schema as if there was only one such type.
func DeduplicateTypes(spec *schema.PackageSpec) (*schema.PackageSpec, hcl.Diagnostics, error) {
	diags := hcl.Diagnostics{}

	normalizedTokens := map[string]string{}
	for typeToken := range spec.Types {
		key := strings.ToUpper(typeToken)
		old, conflict := normalizedTokens[key]
		if !conflict || (conflict && typeToken < old) {
			normalizedTokens[key] = typeToken
		}
	}

	renamedTypes := map[string]string{}
	for typeToken := range spec.Types {
		newToken := normalizedTokens[strings.ToUpper(typeToken)]
		if newToken != typeToken {
			renamedTypes[typeToken] = newToken
		}
	}

	// Drop to json-y maps for the rewrite of types and references
	// instead of trying to work with the pschema.PackageSpec
	// representation.

	var buf bytes.Buffer
	if err := json.NewEncoder(&buf).Encode(spec); err != nil {
		return nil, nil, err
	}

	var rawSchema interface{}
	if err := json.NewDecoder(bytes.NewReader(buf.Bytes())).Decode(&rawSchema); err != nil {
		return nil, nil, err
	}

	types := map[string]interface{}{}
	if x, ok := rawSchema.(map[string]interface{}); ok {
		if y, ok2 := x["types"]; ok2 {
			if z, ok3 := y.(map[string]interface{}); ok3 {
				types = z
			}
		}
	}

	for oldToken, newToken := range renamedTypes {
		eq := reflect.DeepEqual(
			transformJSONTree(stripDescription, types[oldToken]),
			transformJSONTree(stripDescription, types[newToken]),
		)
		if eq {
			diags = append(diags, &hcl.Diagnostic{
				Severity: hcl.DiagWarning,
				Summary:  fmt.Sprintf("Renaming '%s' to '%s' in the schema", oldToken, newToken),
			})
			delete(types, oldToken)
		} else {
			diags = append(diags, &hcl.Diagnostic{
				Severity: hcl.DiagWarning,
				Summary: fmt.Sprintf(
					"Not renaming '%s' to '%s' in the schema because they differ structurally",
					oldToken, newToken,
				),
			})
		}
	}

	ref := func(token string) string {
		return fmt.Sprintf("#/types/%s", token)
	}

	renamedRefs := map[string]string{}
	for oldToken, newToken := range renamedTypes {
		renamedRefs[ref(oldToken)] = ref(newToken)
	}

	rewriteRefs := func(node interface{}) interface{} {
		s, isString := node.(string)
		if !isString || !strings.HasPrefix(s, "#/types/") {
			return node
		}
		if r, isRenamed := renamedRefs[s]; isRenamed {
			diags = append(diags, &hcl.Diagnostic{
				Severity: hcl.DiagWarning,
				Summary:  fmt.Sprintf("Rewrote reference '%s' to '%s'", s, r),
			})
			return r
		}
		return node
	}

	rawSchema = transformJSONTree(rewriteRefs, rawSchema)

	buf.Reset()

	if err := json.NewEncoder(&buf).Encode(&rawSchema); err != nil {
		return nil, nil, err
	}

	var fixedSpec schema.PackageSpec
	if err := json.NewDecoder(bytes.NewReader(buf.Bytes())).Decode(&fixedSpec); err != nil {
		return nil, nil, err
	}

	return &fixedSpec, diags, nil
}

func transformJSONTree(t func(interface{}) interface{}, tree interface{}) interface{} {
	m, isMap := tree.(map[string]interface{})
	if isMap {
		n := map[string]interface{}{}
		for k, v := range m {
			n[k] = transformJSONTree(t, v)
		}
		return t(n)
	}
	s, isSlice := tree.([]interface{})
	if isSlice {
		n := []interface{}{}
		for _, e := range s {
			n = append(n, transformJSONTree(t, e))
		}
		return t(n)
	}
	return t(tree)
}

func stripDescription(v interface{}) interface{} {
	m, isMap := v.(map[string]interface{})
	if isMap {
		delete(m, "description")
		return m
	}
	return v
}
