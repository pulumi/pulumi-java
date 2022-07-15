package java

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
	"github.com/stretchr/testify/assert"
)

type PclTestFile struct {
	FileName string
	FilePath string
}

func pclTestFilePaths(dir string) []PclTestFile {
	var paths []PclTestFile
	files, err := ioutil.ReadDir(dir)
	if err != nil {
		return paths
	}

	for _, file := range files {
		if strings.HasSuffix(file.Name(), ".pp") {
			fullPath, _ := filepath.Abs(filepath.Join(dir, file.Name()))
			pclTestFile := PclTestFile{FileName: file.Name(), FilePath: fullPath}
			paths = append(paths, pclTestFile)
		}
	}

	return paths
}

// Returns whether the input directory path already exists
func directoryExists(dir string) bool {
	info, err := os.Stat(dir)
	return err == nil && info.IsDir()
}

var testdataPath = filepath.Join("..", "testing", "test", "testdata")

func TestGenerateJavaProgram(t *testing.T) {
	t.Parallel()

	files, err := ioutil.ReadDir(testdataPath)
	assert.NoError(t, err)
	tests := make([]test.ProgramTest, 0, len(files))
	for _, f := range files {
		name := f.Name()
		if !strings.HasSuffix(name, "-pp") {
			continue
		}
		t.Logf("Testing %s: %q", f.Name(), strings.TrimSuffix(name, "-pp"))
		tests = append(tests, test.ProgramTest{
			Directory: strings.TrimSuffix(name, "-pp"),
		})
	}
	test.TestProgramCodegen(t, test.ProgramCodegenOptions{
		Language:   "java",
		Extension:  "java",
		OutputFile: "MyStack.java",
		GenProgram: GenerateProgram,
		TestCases:  tests,
	})
}

func runSingleProgramGenTest(t *testing.T, name string) {
	test.TestProgramCodegen(t, test.ProgramCodegenOptions{
		Language:   "java",
		Extension:  "java",
		OutputFile: "MyStack.java",
		GenProgram: GenerateProgram,
		TestCases:  []test.ProgramTest{{Directory: name}},
	})
}

func TestAwsStaticWebsite(t *testing.T) {
	runSingleProgramGenTest(t, "aws-s3-folder")
}

func TestAwsFargate(t *testing.T) {
	runSingleProgramGenTest(t, "aws-fargate")
}

func TestAwsWebserver(t *testing.T) {
	runSingleProgramGenTest(t, "aws-webserver")
}

func TestSimpleInvokeWithRange(t *testing.T) {
	runSingleProgramGenTest(t, "simple-invoke-with-range")
}

func TestAzureNativeExample(t *testing.T) {
	runSingleProgramGenTest(t, "azure-native")
}
