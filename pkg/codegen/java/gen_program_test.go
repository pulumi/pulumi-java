package java

import (
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

var testdataPath = filepath.Join("..", "testing", "test", "testdata")
var transpiledExamplesDir = "transpiled_examples"

var skipGenerationTests = []string{
	"azure-container-apps",
	"getting-started",
	"pulumi-variable",
}

func skipGeneration(test string) bool {
	for _, t := range skipGenerationTests {
		if t == test {
			return true
		}
	}
	return false
}

func TestGenerateJavaProgram(t *testing.T) {
	t.Parallel()

	files, err := os.ReadDir(testdataPath)
	assert.NoError(t, err)
	tests := make([]test.ProgramTest, 0, len(files))
	for _, f := range files {
		name := f.Name()
		if f.IsDir() && name == transpiledExamplesDir {
			syncDir := filepath.Join(testdataPath, transpiledExamplesDir)
			files, err := os.ReadDir(syncDir)
			assert.NoError(t, err)
			for _, f := range files {
				name := strings.TrimSuffix(f.Name(), "-pp")
				if skipGeneration(name) {
					continue
				}
				tests = append(tests, test.ProgramTest{
					Directory: filepath.Join(transpiledExamplesDir, name),
				})
			}
			continue
		}
		if !strings.HasSuffix(name, "-pp") {
			continue
		}
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
