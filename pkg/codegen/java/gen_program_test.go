package java

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
	"github.com/stretchr/testify/assert"
)

type PclTestFile struct {
	FileName string
	FilePath string
}

var testdataPath = filepath.Join("..", "testing", "test", "testdata")

func TestGenerateJavaProgram(t *testing.T) {
	t.Parallel()

	files, err := os.ReadDir(testdataPath)
	assert.NoError(t, err)
	tests := make([]test.ProgramTest, 0, len(files))
	for _, f := range files {
		name := f.Name()
		if !strings.HasSuffix(name, "-pp") {
			continue
		}
		tests = append(tests, test.ProgramTest{
			Directory:   strings.TrimSuffix(name, "-pp"),
			BindOptions: []pcl.BindOption{pcl.SkipResourceTypechecking},
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
