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
	if err := gradleValidatePackage(pkg); err != nil {
		return err
	}

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

func gradleValidatePackage(pkg *schema.Package) error {
	validationErrors := []string{}

	if pkg.Description == "" {
		v := `"description" needs to be non-empty to satisfy POM validation rules`
		validationErrors = append(validationErrors, v)
	}

	if pkg.Repository == "" {
		v := `"repository" needs to be non-empty to satisfy POM validation rules; ` +
			`a valid example is "https://github.com/mycorp/mypkg"`
		validationErrors = append(validationErrors, v)
	}

	if len(validationErrors) != 0 {
		msg := "Pulumi Package Schema has %d issues that obstruct generating a valid Gradle project:\n- %s"
		return fmt.Errorf(msg, len(validationErrors), strings.Join(validationErrors, "\n- "))
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
	GradleTestJUnitPlatformEnabled  bool
}

func newGradleTemplateContext(
	pkg *schema.Package,
	packageInfo *PackageInfo,
) gradleTemplateContext {

	ctx := gradleTemplateContext{
		ProjectURL:                     pkg.Repository,
		ProjectGitURL:                  formatGitURL(pkg.Repository),
		GradleTestJUnitPlatformEnabled: packageInfo.GradleTest == "JUnitPlatform",
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

	if packageInfo.Dependencies != nil {
		ctx.Dependencies = packageInfo.Dependencies
	} else {
		ctx.Dependencies = map[string]string{}
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
