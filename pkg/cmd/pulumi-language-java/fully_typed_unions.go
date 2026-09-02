// Copyright 2026, Pulumi Corporation.  All rights reserved.

package main

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/blang/semver"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	codegen "github.com/pulumi/pulumi-java/pkg/codegen/java"
)

// fullyTypedUnionsLoader turns on the fullyTypedUnions Java option for every package it loads.
// The conformance tests run the language host with it, so the SDKs and programs they generate
// exercise typed union interfaces.
type fullyTypedUnionsLoader struct {
	schema.ReferenceLoader
}

func (l fullyTypedUnionsLoader) LoadPackage(pkg string, version *semver.Version) (*schema.Package, error) {
	//nolint:staticcheck // the interface still requires it
	p, err := l.ReferenceLoader.LoadPackage(pkg, version)
	if err != nil {
		return nil, err
	}
	return p, enableFullyTypedUnions(p)
}

func (l fullyTypedUnionsLoader) LoadPackageV2(
	ctx context.Context, descriptor *schema.PackageDescriptor,
) (*schema.Package, error) {
	p, err := l.ReferenceLoader.LoadPackageV2(ctx, descriptor)
	if err != nil {
		return nil, err
	}
	return p, enableFullyTypedUnions(p)
}

func (l fullyTypedUnionsLoader) LoadPackageReference(
	pkg string, version *semver.Version,
) (schema.PackageReference, error) {
	//nolint:staticcheck // the interface still requires it
	ref, err := l.ReferenceLoader.LoadPackageReference(pkg, version)
	if err != nil {
		return nil, err
	}
	return ref, enableFullyTypedUnionsRef(ref)
}

func (l fullyTypedUnionsLoader) LoadPackageReferenceV2(
	ctx context.Context, descriptor *schema.PackageDescriptor,
) (schema.PackageReference, error) {
	ref, err := l.ReferenceLoader.LoadPackageReferenceV2(ctx, descriptor)
	if err != nil {
		return nil, err
	}
	return ref, enableFullyTypedUnionsRef(ref)
}

func enableFullyTypedUnionsRef(ref schema.PackageReference) error {
	def, err := ref.Definition()
	if err != nil {
		return err
	}
	return enableFullyTypedUnions(def)
}

// enableFullyTypedUnions sets the option on a bound package, whether or not its Java language
// section has been imported yet.
func enableFullyTypedUnions(pkg *schema.Package) error {
	if pkg.Language == nil {
		pkg.Language = map[string]any{}
	}
	switch info := pkg.Language["java"].(type) {
	case codegen.PackageInfo:
		pkg.Language["java"] = info.With(codegen.PackageInfo{FullyTypedUnions: true})
		return nil
	case json.RawMessage:
		raw, err := withFullyTypedUnions(info)
		if err != nil {
			return err
		}
		pkg.Language["java"] = json.RawMessage(raw)
		return nil
	case nil:
		raw, err := withFullyTypedUnions(nil)
		if err != nil {
			return err
		}
		pkg.Language["java"] = json.RawMessage(raw)
		return nil
	default:
		return fmt.Errorf("unexpected java language section of type %T", info)
	}
}

// withFullyTypedUnions returns the raw Java language section with fullyTypedUnions turned on.
func withFullyTypedUnions(raw []byte) ([]byte, error) {
	options := map[string]any{}
	if len(raw) > 0 {
		if err := json.Unmarshal(raw, &options); err != nil {
			return nil, fmt.Errorf("parsing the java language section: %w", err)
		}
	}
	options["fullyTypedUnions"] = true
	return json.Marshal(options)
}
