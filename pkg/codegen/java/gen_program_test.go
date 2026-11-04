package java

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/utils"
	"github.com/pulumi/pulumi/pkg/v3/resource/plugin"
	"github.com/stretchr/testify/assert"
)

type PclTestFile struct {
	FileName string
	FilePath string
}

var testdataPath = filepath.Join("..", "testing", "test", "testdata")

// localSchemaTests lists program tests whose schema isn't in pulumi/pulumi's shared test
// schemas. Each one is loaded from the test's own directory as "<name>-<version>.json".
var localSchemaTests = map[string]utils.SchemaProvider{
	"enum-list": utils.NewSchemaProvider("enumlist", "1.0.0"),
}

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
		directory := strings.TrimSuffix(name, "-pp")
		var pluginCtx *plugin.Context
		if provider, ok := localSchemaTests[directory]; ok {
			pluginCtx = utils.NewContextWithProviders(filepath.Join(testdataPath, name), provider)
		}
		tests = append(tests, test.ProgramTest{
			Directory: directory,
			BindOptions: []pcl.BindOption{
				pcl.SkipResourceTypechecking,
				pcl.PreferOutputVersionedInvokes,
			},
			PluginContext: pluginCtx,
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
