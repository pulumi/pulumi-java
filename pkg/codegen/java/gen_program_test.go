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

func compileSingleFile(name string, t *testing.T) {
	pclFiles := pclTestFilePaths(testdataPath)
	assert.NotEmpty(t, pclFiles)
	for _, pclFile := range pclFiles {
		if pclFile.FileName == name {
			t.Logf("Compiling %s", pclFile)
			pclFileContents, err := ioutil.ReadFile(pclFile.FilePath)
			assert.Nilf(t, err, "Could not read contents of PCL file %s", pclFile)
			compiledFile, diagnostics, err := compilePclToJava(pclFileContents, testdataPath)
			assert.Nilf(t, err, "Could not compile %s", pclFile)
			if diagnostics != nil {
				for _, diagError := range diagnostics.Errs() {
					t.Logf("    Diagnostics: %s", diagError.Error())
				}
			}
			outputDirName := strings.ReplaceAll(pclFile.FileName, ".pp", "-pp")
			outputDir := filepath.Join(testdataPath, outputDirName)
			if !directoryExists(outputDir) {
				err := os.Mkdir(outputDir, os.ModePerm)
				assert.Nilf(t, err, "Could not create directory %s", outputDir)
			}

			outputFile := filepath.Join(outputDir, "MyStack.java")
			err = ioutil.WriteFile(outputFile, compiledFile, 0600)
			assert.Nilf(t, err, "Could not write compiled Java file %s", outputFile)
			t.Logf("Written compiled file %s", outputFile)
		}
	}
}

func TestAwsStaticWebsite(t *testing.T) {
	compileSingleFile("aws-s3-folder.pp", t)
}

func TestAwsFargate(t *testing.T) {
	compileSingleFile("aws-fargate.pp", t)
}

func TestAwsWebserver(t *testing.T) {
	compileSingleFile("aws-webserver.pp", t)
}

func TestSimpleInvokeWithRange(t *testing.T) {
	compileSingleFile("simple-invoke-with-range.pp", t)
}

func TestAzureNativeExample(t *testing.T) {
	compileSingleFile("azure-native.pp", t)
}
