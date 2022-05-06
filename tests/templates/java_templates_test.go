// Copyright 2016-2022, Pulumi Corporation.  All rights reserved.

package templates

import (
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"regexp"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/engine"
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
	ptesting "github.com/pulumi/pulumi/sdk/v3/go/common/testing"
	"github.com/stretchr/testify/assert"
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

	tDir := templateDir(t, templateName)
	tempFileEditCloser, err := fixupVersions(tDir)
	assert.NoError(t, err)
	if tempFileEditCloser != nil {
		defer tempFileEditCloser.Close()
	}

	cmdArgs := []string{"new", templateDir(t, templateName), "-f", "--yes", "-s", "template-test"}
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
	return filepath.Join(repoRoot, "..", "templates", templateName)
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

func fixupVersions(root string) (io.Closer, error) {
	pom := filepath.Join(root, "pom.xml")
	hasPom, err := fileExists(pom)
	if err != nil {
		return nil, err
	}

	buildGradle := filepath.Join(root, "app", "build.gradle")
	hasBuildGradle, err := fileExists(buildGradle)
	if err != nil {
		return nil, err
	}

	if hasPom {
		return fixupPomVersions(pom)
	} else if hasBuildGradle {
		return fixupGradleVersions(buildGradle)
	}

	return nil, nil
}

func fixupPomVersions(pom string) (io.Closer, error) {
	versions := versionsFromEnv()
	vpat := regexp.MustCompile("<version>[^<]+</version>")
	return editFile(pom, func(bytes []byte) []byte {
		for dep, ver := range versions {
			pat := regexp.MustCompile(fmt.Sprintf("<artifactId>%s</artifactId>\\s*"+
				"<version>[^<]+</version>", dep))
			newVer := []byte(fmt.Sprintf("<version>%s</version>", ver))
			bytes = pat.ReplaceAllFunc(bytes, func(match []byte) []byte {
				return vpat.ReplaceAll(match, newVer)
			})
		}
		return bytes
	})
}

func fixupGradleVersions(buildGradle string) (io.Closer, error) {
	versions := versionsFromEnv()
	return editFile(buildGradle, func(bytes []byte) []byte {
		for dep, ver := range versions {
			newVer := []byte(fmt.Sprintf("com.pulumi:%s:%s", dep, ver))
			pat := regexp.MustCompile(fmt.Sprintf("com.pulumi:%s:[^']+", dep))
			bytes = pat.ReplaceAll(bytes, newVer)
		}
		return bytes
	})
}

func versionsFromEnv() map[string]string {
	vn := map[string]string{}
	vn["pulumi"] = os.Getenv("PULUMI_JAVA_SDK_VERSION")
	vn["aws"] = os.Getenv("PULUMI_AWS_PROVIDER_SDK_VERSION")
	vn["aws-native"] = os.Getenv("PULUMI_AWS_NATIVE_PROVIDER_SDK_VERSION")
	vn["azure-native"] = os.Getenv("PULUMI_AZURE_NATIVE_PROVIDER_SDK_VERSION")
	vn["azuread"] = os.Getenv("PULUMI_AZUREAD_PROVIDER_SDK_VERSION")
	vn["docker"] = os.Getenv("PULUMI_DOCKER_PROVIDER_SDK_VERSION")
	vn["eks"] = os.Getenv("PULUMI_EKS_PROVIDER_SDK_VERSION")
	vn["gcp"] = os.Getenv("PULUMI_GCP_PROVIDER_SDK_VERSION")
	vn["google-native"] = os.Getenv("PULUMI_GOOGLE_NATIVE_PROVIDER_SDK_VERSION")
	vn["kubernetes"] = os.Getenv("PULUMI_KUBERNETES_PROVIDER_SDK_VERSION")
	vn["random"] = os.Getenv("PULUMI_RANDOM_PROVIDER_SDK_VERSION")
	for k, v := range vn {
		if v == "" {
			delete(vn, k)
		}
	}
	return vn
}

func fileExists(path string) (bool, error) {
	_, err := os.Stat(path)

	if err != nil && os.IsNotExist(err) {
		return false, nil
	}

	return true, err
}

// Edits a file and returns a closer to undo the edit.
func editFile(path string, edit func([]byte) []byte) (io.Closer, error) {
	bytes, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}
	err = ioutil.WriteFile(path, edit(bytes), 0600)
	return revertFile{path, bytes}, err
}

type revertFile struct {
	path  string
	bytes []byte
}

func (rf revertFile) Close() error {
	return ioutil.WriteFile(rf.path, rf.bytes, 0600)
}
