// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/blang/semver"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func TestNewGradleTemplateContextLegacy(t *testing.T) {
	pkg, info := eksExample()
	tctx := newGradleTemplateContext(pkg, info, true /*legacyBuildFiles*/)
	assert.Equal(t, "0.37.1", tctx.Version)
	assert.Equal(t, "com.pulumi", tctx.GroupID)
	assert.Equal(t, "eks", tctx.Name)
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
	assert.Equal(t, "", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, false, tctx.GradleNexusPublishPluginEnabled)
}

func TestNewGradleTemplateContext(t *testing.T) {
	pkg, info := eksExample()
	tctx := newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "0.37.1", tctx.Version)
	assert.Equal(t, "com.pulumi", tctx.GroupID)
	assert.Equal(t, "eks", tctx.Name)
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
	assert.Equal(t, "2.0.0", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)
}

func TestNewGradleTemplateContextBuildFiles(t *testing.T) {
	pkg, _ := eksExample()

	// Legacy build files: false

	// We default to the behavior of `gradle-nexus`.
	info := &PackageInfo{BuildFiles: ""}
	tctx := newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "2.0.0", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle-nexus"}
	tctx = newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "2.0.0", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle-nexus", GradleNexusPublishPluginVersion: "1.2.3"}
	tctx = newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "1.2.3", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle"}
	tctx = newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, false, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle", GradleNexusPublishPluginVersion: "1.2.3"}
	tctx = newGradleTemplateContext(pkg, info, false /*legacyBuildFiles*/)
	assert.Equal(t, "1.2.3", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)

	// Legacy build files: true

	info = &PackageInfo{BuildFiles: ""}
	tctx = newGradleTemplateContext(pkg, info, true /*legacyBuildFiles*/)
	assert.Equal(t, "", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, false, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle"}
	tctx = newGradleTemplateContext(pkg, info, true /*legacyBuildFiles*/)
	assert.Equal(t, "", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, false, tctx.GradleNexusPublishPluginEnabled)

	info = &PackageInfo{BuildFiles: "gradle", GradleNexusPublishPluginVersion: "1.2.3"}
	tctx = newGradleTemplateContext(pkg, info, true /*legacyBuildFiles*/)
	assert.Equal(t, "1.2.3", tctx.GradleNexusPublishPluginVersion)
	assert.Equal(t, true, tctx.GradleNexusPublishPluginEnabled)
}

func TestGenGradleProjectLegacyTrue(t *testing.T) {
	pkg, info := eksExample()
	files := fs{}
	err := genGradleProject(pkg, info, files, true /*legacyBuildFiles*/)
	if err != nil {
		t.Error(err)
	}
	gradleFile := files["build.gradle"]
	snapshot, err := os.ReadFile(filepath.Join("testdata", "gen-gradle-project-legacy-true", "build.gradle"))
	require.NoError(t, err)
	require.Equal(t, string(snapshot), string(gradleFile))
}

func TestGenGradleProjectLegacyFalse(t *testing.T) {
	pkg, info := eksExample()
	files := fs{}
	err := genGradleProject(pkg, info, files, false /*legacyBuildFiles*/)
	if err != nil {
		t.Error(err)
	}
	gradleFile := files["build.gradle"]
	snapshot, err := os.ReadFile(filepath.Join("testdata", "gen-gradle-project-legacy-false", "build.gradle"))
	require.NoError(t, err)
	require.Equal(t, string(snapshot), string(gradleFile))
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
		{"Acmecorp", "invalid url!", false},
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
