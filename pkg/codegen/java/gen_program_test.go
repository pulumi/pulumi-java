package java

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/blang/semver"

	"github.com/pulumi/pulumi/pkg/v3/codegen/pcl"
	"github.com/pulumi/pulumi/pkg/v3/codegen/testing/test"
	"github.com/pulumi/pulumi/pkg/v3/resource/deploy/deploytest"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
	"github.com/stretchr/testify/assert"
)

type PclTestFile struct {
	FileName string
	FilePath string
}

var testdataPath = filepath.Join("..", "testing", "test", "testdata")

// kubernetesPluginContext returns a schema-only plugin context that serves the committed kubernetes
// 3.7.0 schema. The shared test host (utils.NewContext) stopped registering a kubernetes provider, so
// the kubernetes-template program test supplies its own context to keep resolving kubernetes types.
func kubernetesPluginContext() *plugin.Context {
	loader := deploytest.NewProviderLoader("kubernetes", semver.MustParse("3.7.0"),
		func() (plugin.Provider, error) {
			return &deploytest.Provider{
				GetSchemaF: func(_ context.Context, _ plugin.GetSchemaRequest) (plugin.GetSchemaResponse, error) {
					data, err := os.ReadFile(filepath.Join(testdataPath, "kubernetes-3.7.0.json"))
					if err != nil {
						return plugin.GetSchemaResponse{}, err
					}
					return plugin.GetSchemaResponse{Schema: data}, nil
				},
			}, nil
		})
	host := deploytest.NewPluginHost(nil, nil, nil, loader)
	return plugin.NewContextWithHost(context.Background(), nil, nil, host, "", "", nil)
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
		programTest := test.ProgramTest{
			Directory: strings.TrimSuffix(name, "-pp"),
			BindOptions: []pcl.BindOption{
				pcl.SkipResourceTypechecking,
				pcl.PreferOutputVersionedInvokes,
			},
		}
		if strings.HasPrefix(programTest.Directory, "kubernetes-") {
			programTest.PluginContext = kubernetesPluginContext()
		}
		tests = append(tests, programTest)
	}
	test.TestProgramCodegen(t, test.ProgramCodegenOptions{
		Language:   "java",
		Extension:  "java",
		OutputFile: "MyStack.java",
		GenProgram: GenerateProgram,
		TestCases:  tests,
	})
}
