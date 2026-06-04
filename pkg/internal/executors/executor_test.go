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
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{":app:run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleCurrentDir(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle": {},
			"build.gradle":    {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{"run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleKTS(t *testing.T) {
	fsys := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"settings.gradle.kts":  {},
			"app/build.gradle.kts": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
}

func TestGradlew(t *testing.T) {
	fsys := fsys.TestFS("app",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"app/build.gradle": {},
			"gradlew":          {},
			"settings.gradle":  {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.Cmd)
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
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{":services:app-cluster:run", "--console=plain"},
		exec.RunArgs)
}

func TestGradleKTSMultiProject(t *testing.T) {
	fsys := fsys.TestFS("services/app-cluster",
		map[string]string{"gradle": "/usr/bin/gradle"},
		fstest.MapFS{
			"services/app-cluster/build.gradle.kts":  {},
			"services/mgmt-cluster/build.gradle.kts": {},
			"gradlew":                                {},
			"settings.gradle.kts":                    {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./gradlew", exec.Cmd)
	assert.Equal(t, ".", exec.Dir)
	assert.Equal(t,
		[]string{":services:app-cluster:run", "--console=plain"},
		exec.RunArgs)
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
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-gradlew",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./custom-gradlew", exec.Cmd)

	exec, err = NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-gradle",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-gradle", exec.Cmd)

	// Even if no marker settings.gradle files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS("app",
		map[string]string{
			"gradle": "/usr/bin/gradle",
		},
		fstest.MapFS{
			"app/hello.txt": {},
		})

	exec, err = NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "gradle",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/gradle", exec.Cmd)
}

func TestMavenSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"pom.xml": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.Cmd)
}

func TestMavenW(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mvn": "/usr/bin/mvn"},
		fstest.MapFS{
			"mvnw":    {},
			"pom.xml": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./mvnw", exec.Cmd)
}

func TestDebugExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{
			"mvn":      "/usr/bin/mvn",
			"mvnDebug": "/usr/bin/mvnDebug",
		},
		fstest.MapFS{})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, true)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvnDebug", exec.Cmd)
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
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "./custom-mvnw",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./custom-mvnw", exec.Cmd)

	exec, err = NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "/bin/custom-mvn",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-mvn", exec.Cmd)

	// Even if no marker pom.xml files are found,
	// UseExecutor forces the use of gradle.
	fs = fsys.TestFS(".",
		map[string]string{
			"mvn": "/usr/bin/mvn",
		},
		fstest.MapFS{
			"hello.txt": {},
		})
	exec, err = NewJavaExecutor(JavaExecutorOptions{
		WD:          fs,
		UseExecutor: "mvn",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mvn", exec.Cmd)
}

func TestJarExecSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"java": "/usr/bin/java"},
		fstest.MapFS{"dist/hello.jar": {}})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:     fsys,
		Binary: "dist/hello.jar",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/java", exec.Cmd)
	assert.Equal(t, []string{"-jar", "dist/hello.jar"}, exec.RunArgs)
}

func TestJBangSimple(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{
			"src/main.java":    {},
			"jbang.properties": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD: fsys,
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.Cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/main.java"}, exec.RunArgs)
}

func TestJBangCustomMainFile(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{
			"src/custom.java": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:     fsys,
		Binary: "src/custom.java",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.Cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/custom.java"}, exec.RunArgs)
}

func TestJBangUseExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"jbang": "/usr/bin/jbang"},
		fstest.MapFS{})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		UseExecutor: "jbang",
		WD:          fsys,
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/jbang", exec.Cmd)
	assert.Equal(t, []string{"--quiet", "run", "src/main.java"}, exec.RunArgs)
}

func TestSBTExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"sbt": "/usr/bin/sbt"},
		fstest.MapFS{
			"build.sbt": {},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD: fsys,
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/sbt", exec.Cmd)
	assert.Equal(t, []string{"-batch", "run"}, exec.RunArgs)
}

const millBuildMill = `package build
import mill._
import mill.scalalib._

object app extends ScalaModule {
  def scalaVersion = "3.8.3"
}
`

func TestMillExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.mill": {Data: []byte(millBuildMill)},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:         fsys,
		MillModule: "app",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mill", exec.Cmd)
	assert.Equal(t, []string{"app.run"}, exec.RunArgs)
	assert.Equal(t, []string{"app.compile"}, exec.BuildArgs)
	assert.Equal(t,
		[]string{
			"--ticker", "false",
			"app.runMain", "com.pulumi.bootstrap.internal.Main", "packages",
		},
		exec.PluginArgs)
}

func TestMillBuildMillScalaVariant(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.mill.scala": {Data: []byte(`object pulumi extends ScalaModule {}`)},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:         fsys,
		MillModule: "pulumi",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mill", exec.Cmd)
	assert.Equal(t, []string{"pulumi.run"}, exec.RunArgs)
}

func TestMillBuildMillYaml(t *testing.T) {
	// Mill 1.1+'s declarative single-root-module form. The YAML file IS the
	// module, so Mill tasks are invoked without a module prefix.
	src := `extends: ScalaModule
scalaVersion: 3.7.3
mvnDeps:
- com.pulumi:pulumi:1.28.0
`
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.mill.yaml": {Data: []byte(src)},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/usr/bin/mill", exec.Cmd)
	assert.Equal(t, []string{"compile"}, exec.BuildArgs)
	assert.Equal(t, []string{"run"}, exec.RunArgs)
	assert.Equal(t,
		[]string{
			"--ticker", "false",
			"runMain", "com.pulumi.bootstrap.internal.Main", "packages",
		},
		exec.PluginArgs)
}

func TestMillBuildScIgnored(t *testing.T) {
	// Mill 0.x's build.sc is not a marker for the 1.x executor.
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.sc": {Data: []byte(`object pulumi extends ScalaModule {}`)},
		})
	_, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.Error(t, err)
}

func TestMillModuleRequired(t *testing.T) {
	// A programmable build.mill with no mill-module option is an error: the
	// executor refuses to guess which module holds the Pulumi program.
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.mill": {Data: []byte(millBuildMill)},
		})
	_, err := NewJavaExecutor(JavaExecutorOptions{WD: fsys}, false)
	assert.ErrorContains(t, err, "mill-module")
}

func TestMillUseExecutor(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{
			"mill":             "/usr/bin/mill",
			"/bin/custom-mill": "/bin/custom-mill",
		},
		fstest.MapFS{
			"build.mill": {Data: []byte(millBuildMill)},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:          fsys,
		UseExecutor: "/bin/custom-mill",
		MillModule:  "app",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "/bin/custom-mill", exec.Cmd)
}

func TestMillMillw(t *testing.T) {
	fsys := fsys.TestFS(".",
		map[string]string{"mill": "/usr/bin/mill"},
		fstest.MapFS{
			"build.mill": {Data: []byte(millBuildMill)},
			"mill":       {Mode: 0o755},
		})
	exec, err := NewJavaExecutor(JavaExecutorOptions{
		WD:         fsys,
		MillModule: "app",
	}, false)
	assert.NoError(t, err)
	assert.Equal(t, "./mill", exec.Cmd)
}
