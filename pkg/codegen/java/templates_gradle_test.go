// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"testing"

	"github.com/blang/semver"
	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func TestNewGradleTemplateContext(t *testing.T) {
	pkg, info := eksExample()
	tctx := newGradleTemplateContext(pkg, info)
	assert.Equal(t, "0.37.1", tctx.Version)
	assert.Equal(t, "com.pulumi", tctx.GroupID)
	assert.Equal(t, "eks", tctx.ArtifactID)
	assert.Equal(t, "https://github.com/pulumi/pulumi-eks", tctx.ProjectURL)
	assert.Equal(t, "git@github.com/pulumi/pulumi-eks.git", tctx.ProjectGitURL)
	assert.Equal(t, "Pulumi Amazon Web Services (AWS) EKS Components.", tctx.ProjectDescription)
	assert.Equal(t, "2022", tctx.ProjectInceptionYear)
	assert.Equal(t, "com.pulumi.eks", tctx.RootProjectName)
	assert.Equal(t, "pulumi-eks", tctx.ProjectName)
	assert.Equal(t, "pulumi", tctx.DeveloperID)
	assert.Equal(t, "support@pulumi.com", tctx.DeveloperEmail)
	assert.Equal(t, "The Apache License, Version 2.0", tctx.LicenceName)
	assert.Equal(t, "http://www.apache.org/licenses/LICENSE-2.0.txt", tctx.LicenceURL)
	assert.Equal(t, info.Dependencies, tctx.Dependencies)
}

func TestGenGradleProject(t *testing.T) {
	pkg, info := eksExample()
	files := fs{}
	err := genGradleProject(pkg, info, files)
	if err != nil {
		t.Error(err)
	}
}

func eksExample() (*schema.Package, *PackageInfo) {
	version := semver.MustParse("0.37.1")
	pkg := &schema.Package{
		Name:        "eks",
		Homepage:    "https://pulumi.com",
		License:     "Apache-2.0",
		Repository:  "https://github.com/pulumi/pulumi-eks",
		Description: "Pulumi Amazon Web Services (AWS) EKS Components.",
		Version:     &version,
	}
	deps := map[string]string{
		"com.pulumi:pulumi":     "0.1.0",
		"com.pulumi:aws":        "5.4.0",
		"com.pulumi:kubernetes": "3.19.1",
	}
	info := &PackageInfo{Dependencies: deps}
	return pkg, info
}

func TestIsPublishedByPulumi(t *testing.T) {
	type testCase struct {
		publisher string
		homepage  string
		expected  bool
	}

	testCases := []testCase{
		{"Pulumi", "", true},
		{"pulumi", "", true},
		{"", "https://pulumi.com", true},
		{"", "https://www.pulumi.com", true},
		{"", "http://www.pulumi.com", true},
		{"", "https://www.pulumi.com/registry/packages/xyz/", true},
		{"", "https://pulumi.io", true},
		{"Pulumiverse", "https://example.com", false},
		{"Pulumi fan", "https://pulumi.co", false},
	}

	for _, tc := range testCases {
		pkg := &schema.Package{
			Publisher: tc.publisher,
			Homepage:  tc.homepage,
		}
		result := isPublishedByPulumi(pkg)
		assert.Equal(t, tc.expected, result)
	}
}
