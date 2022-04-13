// Copyright 2016-2022, Pulumi Corporation.  All rights reserved.

package templates

import (
	"path/filepath"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	ptesting "github.com/pulumi/pulumi/sdk/v3/go/common/testing"
)

type templateConfig struct {
	name   string
	config map[string]string
}

func TestTemplates(t *testing.T) {
	templates := []templateConfig{
		{
			name: "aws-java",
			config: map[string]string{
				"aws:region": "us-west-2",
			},
		},
		{
			name: "azure-java",
			config: map[string]string{
				"azure-native:location": "westus",
			},
		},
		{name: "gcp-java"},
		{name: "java"},
		{name: "java-gradle"},
		{name: "kubernetes-java"},
	}

	for _, tpl := range templates {
		tpl := tpl
		t.Run(tpl.name, func(t *testing.T) {
			checkTemplate(t, tpl)
		})
	}
}

func checkTemplate(t *testing.T, templateCfg templateConfig) {
	templateName := templateCfg.name
	e := ptesting.NewEnvironment(t)
	defer deleteIfNotFailed(e)
	cmdArgs := []string{"new", templateDir(t, templateName), "-f", "--yes", "-s", "template-test"}
	e.RunCommand("pulumi", cmdArgs...)
	if t.Failed() {
		t.FailNow()
	}
	opts := integration.ProgramTestOptions{
		Dir:    e.RootPath,
		Config: templateCfg.config,
		PrepareProject: func(*engine.Projinfo) error {
			return nil
		},
	}.With(previewOnlyOptions())
	integration.ProgramTest(t, &opts)
}

func templateDir(t *testing.T, templateName string) string {
	repoRoot, err := filepath.Abs(filepath.Join("..", ".."))
	if err != nil {
		t.Error(err)
		t.FailNow()
	}
	return filepath.Join(repoRoot, "templates", templateName)
}

func previewOnlyOptions() integration.ProgramTestOptions {
	return integration.ProgramTestOptions{
		SkipRefresh:            true,
		SkipEmptyPreviewUpdate: true,
		SkipExportImport:       true,
		SkipUpdate:             true,
	}
}

// deleteIfNotFailed deletes the files in the testing environment if
// the testcase has not failed. (Otherwise they are left to aid
// debugging.)
func deleteIfNotFailed(e *ptesting.Environment) {
	if !e.T.Failed() {
		e.DeleteEnvironment()
	}
}
