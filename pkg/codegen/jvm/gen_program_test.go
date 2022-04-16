package jvm

import (
	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"testing"

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

func TestGenerateProgram(t *testing.T) {
	t.Parallel()

	test.TestProgramCodegen(t,
		test.ProgramCodegenOptions{
			Language:   "jvm",
			Extension:  "java",
			OutputFile: "App.java",
			Check: func(t *testing.T, path string, dependencies codegen.StringSet) {
				// TODO: implement the check properly
				// Check(t, path, dependencies, "../../../../../../../sdk")
			},
			GenProgram: GenerateProgram,
			TestCases:  test.PulumiPulumiProgramTests,
		})
}

func TestGenerateJavaProgram(t *testing.T) {
	pclFiles := pclTestFilePaths(testdataPath)
	assert.NotEmpty(t, pclFiles)
	for _, pclFile := range pclFiles {
		t.Logf("Compiling %s", pclFile)
		pclFileContents, err := ioutil.ReadFile(pclFile.FilePath)
		assert.NoErrorf(t, err, "Could not read the contents of %s", pclFile.FilePath)
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

func TestSimpleInvokeWithRange(t *testing.T) {
	compileSingleFile("simple-invoke-with-range.pp", t)
}
