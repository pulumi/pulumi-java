// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"

	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

// Detects cases when identical types have similar names modulo case
// such as `azure-native:network:IpAllocationMethod` vs
// `azure-native:network:IPAllocationMethod`, deterministically picks
// one of these names, and rewrites the schema as if there was only
// one such type.
func DedupTypes(spec *pschema.PackageSpec) (*pschema.PackageSpec, error) {
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
		return nil, err
	}

	var rawSchema interface{}
	if err := json.NewDecoder(bytes.NewReader(buf.Bytes())).Decode(&rawSchema); err != nil {
		return nil, err
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
			fmt.Printf("WARN renaming %s to %s in the schema\n",
				oldToken, newToken)
			delete(types, oldToken)
		} else {
			fmt.Printf("WARN not renaming %s to %s in the schema "+
				"because they differ structurally\n",
				oldToken, newToken)
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
			fmt.Printf("Rewritten %s to %s\n", s, r)
			return r
		}
		return node
	}

	rawSchema = transformJSONTree(rewriteRefs, rawSchema)

	buf.Reset()

	if err := json.NewEncoder(&buf).Encode(&rawSchema); err != nil {
		return nil, err
	}

	var fixedSpec pschema.PackageSpec
	if err := json.NewDecoder(bytes.NewReader(buf.Bytes())).Decode(&fixedSpec); err != nil {
		return nil, err
	}

	return &fixedSpec, nil
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
