// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"testing"
	"testing/fstest"

	"github.com/stretchr/testify/assert"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

func TestGradleSimple(t *testing.T) {
	fsys := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle":  {},
			"app/build.gradle": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.cmd)
}

func TestGradleKTS(t *testing.T) {
	fsys := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle.kts": {},
			"app/build.gradle":    {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.cmd)
}

func TestGradlew(t *testing.T) {
	fsys := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"app/build.gradle": {},
			"gradlew":          {},
			"settings.gradle":  {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.cmd)
}

func TestGradleMultiProject(t *testing.T) {
	fsys := fsys.TestFS("services/app-cluster",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"services/app-cluster/build.gradle":  {},
			"services/mgmt-cluster/build.gradle": {},
			"gradlew":                            {},
			"settings.gradle":                    {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.cmd)
}

func TestGradleUseExecutor(t *testing.T) {
	fs := fsys.TestFS("app",
		map[string]string{
			"gradle":             "/usr/bin/gradle",
			"/bin/custom-gradle": "/bin/custom-gradle",
		},
		fstest.MapFS{
			"gradlew":          {},
			"custom-gradlew":   {},
			"settings.gradle":  {},
			"app/build.gradle": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-gradlew",
	})
	assert.NoError(t, err)
	assert.Equal(t, "./custom-gradlew", exec.cmd)

	exec, err = ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-gradle",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-gradle", exec.cmd)

	// Even if no marker settings.gradle files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS("app",
		map[string]string{
			"gradle": "/usr/bin/gradle",
		},
		fstest.MapFS{
			"app/hello.txt": {},
		})

	exec, err = ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "gradle",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.cmd)
}

func TestMavenSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"pom.xml": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.cmd)
}

func TestMavenW(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"mvnw":    {},
			"pom.xml": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{WD: fsys})
	assert.NoError(t, err)
	assert.Equal(t, "./mvnw", exec.cmd)
}

func TestMavenUseExecutor(t *testing.T) {
	fs := fsys.TestFS(".",
		map[string]string{
			"mvn":             "/usr/bin/mvn",
			"/bin/custom-mvn": "/bin/custom-mvn",
		},
		fstest.MapFS{
			"mvnw":        {},
			"custom-mvnw": {},
			"pom.xml":     {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-mvnw",
	})
	assert.NoError(t, err)
	assert.Equal(t, "./custom-mvnw", exec.cmd)

	exec, err = ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-mvn",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-mvn", exec.cmd)

	// Even if no marker pom.xml files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS(".",
		map[string]string{
			"mvn": "/usr/bin/mvn",
		},
		fstest.MapFS{
			"hello.txt": {},
		})
	exec, err = ConfigureExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "mvn",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.cmd)
}

func TestJarExecSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"java": "/usr/bin/java"},
		fstest.MapFS{"dist/hello.jar": {}})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		WD:     fsys,
		Binary: "dist/hello.jar",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/java", exec.cmd)
	assert.Equal(t, []string{"-jar", "dist/hello.jar"}, exec.runArgs)
}

func TestJBangSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{
			"src/main.java":    {},
			"jbang.properties": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		WD: fsys,
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/main.java"}, exec.runArgs)
}

func TestJBangCustomMainFile(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{
			"src/custom.java": {},
		})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		WD:     fsys,
		Binary: "src/custom.java",
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/custom.java"}, exec.runArgs)
}

func TestJBangUseExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{})
	exec, err := ConfigureExecutor(JavaExecutorOptions{
		UseExecutor: "jbang",
		WD:          fsys,
	})
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/main.java"}, exec.runArgs)
}
