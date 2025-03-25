// Copyright 2016-2022, Pulumi Corporation.  All rights reserved.

package templates

import (
	"path/filepath"
	"testing"

	jtests "github.com/pulumi/pulumi-java/tests/internal"
	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	ptesting "github.com/pulumi/pulumi/sdk/v3/go/common/testing"
	"github.com/stretchr/testify/assert"
)

type templateConfig struct {
	name      string
	config    map[string]string
	providers []string
}

func TestTemplates(t *testing.T) {
	templates := []templateConfig{
		{name: "java"},
		{name: "java-gradle"},
		{
			name: "aws-java",
			config: map[string]string{
				"aws:region": "us-west-2",
			},
			providers: []string{"aws"},
		},
		{
			name: "azure-java",
			config: map[string]string{
				"azure-native:location": "westus",
			},
			providers: []string{"azure-native"},
		},
		{
			name:      "gcp-java",
			providers: []string{"gcp"},
		},
		{
			name:      "kubernetes-java",
			providers: []string{"kubernetes"},
		},
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

	tDir := templateDir(t, templateName)
	pins, err := jtests.ParsePinVersionsFromEnv(t, templateCfg.providers)
	assert.NoError(t, err)
	fileEdit, err := jtests.Pin(tDir, pins)
	assert.NoError(t, err)
	if fileEdit != nil {
		defer fileEdit.Revert()
	}

	stackName := ptesting.RandomStackName()

	cmdArgs := []string{"new", templateDir(t, templateName), "-f", "--yes", "-s", stackName}
	e.RunCommand("pulumi", cmdArgs...)
	if t.Failed() {
		t.FailNow()
	}
	opts := integration.ProgramTestOptions{
		Dir:    e.RootPath,
		Config: templateCfg.config,
		// Note: must override PrepareProject to support Java,
		// even if the override is a no-op.
		PrepareProject: func(info *engine.Projinfo) error {
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
