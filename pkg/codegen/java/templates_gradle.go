// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"bytes"
	_ "embed"
	"fmt"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"
)

func genGradleProject(
	pkg *schema.Package,
	packageInfo *PackageInfo,
	files fs,
) error {
	ctx := newGradleTemplateContext(pkg, packageInfo)
	templates := map[string]string{
		"build.gradle":    buildGradleTemplate,
		"settings.gradle": settingsGradleTemplate,
	}
	for fileName, template := range templates {
		var buf bytes.Buffer
		if err := Template(fileName, template).Execute(&buf, ctx); err != nil {
			return err
		}
		files.add(fileName, buf.Bytes())
	}
	return nil
}

//go:embed settings.gradle.template
var settingsGradleTemplate string

//go:embed build.gradle.template
var buildGradleTemplate string

type gradleTemplateContext struct {
	Version                         string
	GroupID                         string
	ArtifactID                      string
	ProjectName                     string
	RootProjectName                 string
	ProjectURL                      string
	ProjectGitURL                   string
	ProjectDescription              string
	ProjectInceptionYear            string
	Dependencies                    map[string]string
	DeveloperID                     string
	DeveloperName                   string
	DeveloperEmail                  string
	LicenceName                     string
	LicenceURL                      string
	GradleNexusPublishPluginEnabled bool
	GradleNexusPublishPluginVersion string
}

func newGradleTemplateContext(
	pkg *schema.Package,
	packageInfo *PackageInfo,
) gradleTemplateContext {

	ctx := gradleTemplateContext{
		ProjectURL:    pkg.Repository,
		ProjectGitURL: formatGitURL(pkg.Repository),
	}

	if packageInfo.GradleNexusPublishPluginVersion != "" {
		ctx.GradleNexusPublishPluginEnabled = true
		ctx.GradleNexusPublishPluginVersion = packageInfo.GradleNexusPublishPluginVersion
	}

	if pkg.Version != nil {
		ctx.Version = pkg.Version.String()
	} else {
		ctx.Version = "0.0.1"
	}

	if packageInfo.Packages != nil {
		ctx.Dependencies = packageInfo.Packages
	} else {
		ctx.Dependencies = map[string]string{}
	}

	// TODO do not depend on Nullable class in generated code
	jsr305 := "com.google.code.findbugs:jsr305"
	if _, got := ctx.Dependencies[jsr305]; !got {
		ctx.Dependencies[jsr305] = "3.0.2"
	}

	if pkg.License == "Apache-2.0" {
		ctx.LicenceName = "The Apache License, Version 2.0"
		ctx.LicenceURL = "http://www.apache.org/licenses/LICENSE-2.0.txt"
	}

	if pkg.Publisher == "Pulumi" ||
		pkg.Homepage == "https://pulumi.com" ||
		pkg.Homepage == "https://pulumi.io" {
		ctx.ArtifactID = pkg.Name
		ctx.GroupID = "com.pulumi"
		ctx.DeveloperID = "pulumi"
		ctx.DeveloperName = "Pulumi"
		ctx.DeveloperEmail = "support@pulumi.com"
		ctx.ProjectDescription = pkg.Description
		ctx.ProjectInceptionYear = "2022"
		ctx.ProjectName = fmt.Sprintf("pulumi-%s", ctx.ArtifactID)
	}

	if ctx.RootProjectName == "" {
		ctx.RootProjectName = packageInfo.BasePackageOrDefault() + pkg.Name
	}

	return ctx
}

func formatGitURL(url string) string {
	if strings.HasPrefix(url, "https://github.com/") {
		return fmt.Sprintf("git@github.com/%s.git",
			strings.TrimPrefix(url, "https://github.com/"))
	}
	return url
}
