// Copyright 2016-2022, Pulumi Corporation.  All rights reserved.

package tests

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

// Represents a temporary file edit that can be reverted.
type FileEdit interface {
	Revert() error
}

// Logger such as a testing.T instance.
type Logger interface {
	Logf(string, ...interface{})
}

// Edits dependencies in Maven or Gradle projects.
//
// This helps reuse examples and templates against release candidates
// of new SDK and provider code.
//
// javaProjectDir is a path to a folder with Java code.
//
// Expecting deps to map artifact IDs to versions, assuming
// "com.pulumi" org.
func Pin(javaProjectDir string, deps map[string]string) (FileEdit, error) {
	pom := filepath.Join(javaProjectDir, "pom.xml")
	hasPom, err := fileExists(pom)
	if err != nil {
		return nil, err
	}

	buildGradle := ""
	for _, d := range []string{"infra", "app"} {
		f := filepath.Join(javaProjectDir, d, "build.gradle")
		exists, err := fileExists(f)
		if err != nil {
			return nil, err
		}
		if exists {
			buildGradle = f
			break
		}
	}

	if hasPom {
		return fixupPomVersions(pom, deps)
	} else if buildGradle != "" {
		return fixupGradleVersions(buildGradle, deps)
	} else {
		return nil, fmt.Errorf("Pin cannot find pom.xml or build.gradle in %s",
			javaProjectDir)
	}
}

// Parses env vars used in CI context to find the desired pin versions.
func ParsePinVersionsFromEnv(t Logger, providers []string) (map[string]string, error) {
	deps := map[string]string{}

	// Always pin com.pulumi:pulumi dependency.
	deps["pulumi"] = os.Getenv("PULUMI_JAVA_SDK_VERSION")
	if deps["pulumi"] == "" {
		// In local test scenarios, make sure the test is
		// pinned to the default unpublished version produced
		// by sdk/java, so we test the local version, not a
		// released that may be different.
		deps["pulumi"] = "0.0.1"
	}
	t.Logf("Pinned com.pulumi:pulumi to %s", deps["pulumi"])

	envVarNames := providerToEnvVarMap()

	// Optionally pin providers if needed for a template.
	for _, provider := range providers {
		envVarName := envVarNames[provider]
		if envVarName == "" {
			return nil, fmt.Errorf("Unknown provider: %s", provider)
		}
		pinnedVer := os.Getenv(envVarName)
		if pinnedVer == "" {
			t.Logf("Not pinning %s provider since %s env var is not set",
				provider, envVarName)
			continue
		}
		deps[provider] = pinnedVer
		t.Logf("Pinned com.pulumi:%s to %s", provider, pinnedVer)
	}

	return deps, nil
}

func providerToEnvVarMap() map[string]string {
	m := map[string]string{}
	m["aws"] = "PULUMI_AWS_PROVIDER_SDK_VERSION"
	m["aws-native"] = "PULUMI_AWS_NATIVE_PROVIDER_SDK_VERSION"
	m["azure-native"] = "PULUMI_AZURE_NATIVE_PROVIDER_SDK_VERSION"
	m["azuread"] = "PULUMI_AZUREAD_PROVIDER_SDK_VERSION"
	m["docker"] = "PULUMI_DOCKER_PROVIDER_SDK_VERSION"
	m["eks"] = "PULUMI_EKS_PROVIDER_SDK_VERSION"
	m["gcp"] = "PULUMI_GCP_PROVIDER_SDK_VERSION"
	m["google-native"] = "PULUMI_GOOGLE_NATIVE_PROVIDER_SDK_VERSION"
	m["kubernetes"] = "PULUMI_KUBERNETES_PROVIDER_SDK_VERSION"
	m["random"] = "PULUMI_RANDOM_PROVIDER_SDK_VERSION"
	return m
}

func fixupPomVersions(pom string, deps map[string]string) (FileEdit, error) {
	vpat := regexp.MustCompile("<version>[^<]+</version>")
	return fixupVersionsInFile(pom, deps, func(dep, ver string, codeBytes []byte) []byte {
		pat := regexp.MustCompile(fmt.Sprintf("<artifactId>%s</artifactId>\\s*"+
			"<version>[^<]+</version>", dep))
		newVer := []byte(fmt.Sprintf("<version>%s</version>", ver))
		return pat.ReplaceAllFunc(codeBytes, func(match []byte) []byte {
			return vpat.ReplaceAll(match, newVer)
		})
	})
}

func fixupGradleVersions(buildGradle string, deps map[string]string) (FileEdit, error) {
	return fixupVersionsInFile(buildGradle, deps, fixupGradleVersion)
}

func fixupGradleVersion(dep, ver string, codeBytes []byte) []byte {
	newVer := fmt.Sprintf(`("com.pulumi:%s") { version { strictly "%s" }}`, dep, ver)
	pat := regexp.MustCompile(fmt.Sprintf("[ ]+[\"']com.pulumi:%s:[^\"']+[\"']", dep))
	return pat.ReplaceAll(codeBytes, []byte(newVer))
}

func fixupVersionsInFile(
	filePath string,
	deps map[string]string,
	editDependency func(dep, ver string, codeBytes []byte) []byte,
) (FileEdit, error) {
	missingDeps := []string{}
	edit, err := editFile(filePath, func(codeBytes []byte) []byte {
		for dep, ver := range deps {
			newBytes := editDependency(dep, ver, codeBytes)
			if bytes.Equal(codeBytes, newBytes) {
				missingDeps = append(missingDeps, dep)
			}
			codeBytes = newBytes
		}
		return codeBytes
	})
	if err != nil {
		return nil, err
	}
	if len(missingDeps) > 0 {
		return nil, fmt.Errorf("Failed to pin dependencies for: %s",
			strings.Join(missingDeps, ", "))
	}
	return edit, nil
}

func fileExists(path string) (bool, error) {
	_, err := os.Stat(path)
	if err != nil && os.IsNotExist(err) {
		return false, nil
	}
	return true, err
}

func editFile(path string, edit func([]byte) []byte) (FileEdit, error) {
	bytes, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}
	err = ioutil.WriteFile(path, edit(bytes), 0o600)
	return revertFile{path, bytes}, err
}

type revertFile struct {
	path  string
	bytes []byte
}

func (rf revertFile) Revert() error {
	return ioutil.WriteFile(rf.path, rf.bytes, 0o600)
}
