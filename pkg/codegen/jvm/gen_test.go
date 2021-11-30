package jvm

import (
	"path"
	"path/filepath"
	"testing"

	"github.com/pulumi/pulumi/pkg/v3/codegen/internal/test"
	"github.com/stretchr/testify/assert"
)

func TestGeneratePackage(t *testing.T) {
	tests := []struct {
		name          string
		schemaDir     string
		expectedFiles []string
	}{
		{
			"Simple schema with local resource properties",
			"simple-resource-schema",
			[]string{
				path.Join(expectedFilesPath("example"), "Resource.java"),
				path.Join(expectedFilesPath("example"), "OtherResource.java"),
				path.Join(expectedFilesPath("example"), "ArgFunction.java"),
			},
		},
		{
			"Simple schema with enum types",
			"simple-enum-schema",
			[]string{
				path.Join(expectedFilesPath("plant"), "tree", "v1", "RubberTree.java"),
				path.Join(expectedFilesPath("plant"), "tree", "v1", "Nursery.java"),
				path.Join(expectedFilesPath("plant"), "tree", "v1", "enums", "Diameter.java"),
				path.Join(expectedFilesPath("plant"), "tree", "v1", "enums", "Farm.java"),
				path.Join(expectedFilesPath("plant"), "tree", "v1", "enums", "RubberTreeVariety.java"),
				path.Join(expectedFilesPath("plant"), "tree", "v1", "enums", "TreeSize.java"),
				path.Join(expectedFilesPath("plant"), "enums", "ContainerBrightness.java"),
				path.Join(expectedFilesPath("plant"), "enums", "ContainerColor.java"),
				path.Join(expectedFilesPath("plant"), "enums", "ContainerSize.java"),
				path.Join(expectedFilesPath("plant"), "Inputs.java"),
				path.Join(expectedFilesPath("plant"), "Outputs.java"),
			},
		},
		{
			"External resource schema",
			"external-resource-schema",
			[]string{
				path.Join(expectedFilesPath("example"), "ArgFunction.java"),
				path.Join(expectedFilesPath("example"), "Cat.java"),
				path.Join(expectedFilesPath("example"), "Component.java"),
				path.Join(expectedFilesPath("example"), "Inputs.java"),
				path.Join(expectedFilesPath("example"), "Workload.java"),
			},
		},
		{
			"Simple schema with plain properties",
			"simple-plain-schema",
			[]string{
				path.Join(expectedFilesPath("example"), "Inputs.java"),
				path.Join(expectedFilesPath("example"), "Outputs.java"),
				path.Join(expectedFilesPath("example"), "Component.java"),
			},
		},
	}
	testDir := filepath.Join("..", "internal", "test", "testdata")
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			files, err := test.GeneratePackageFilesFromSchema(
				filepath.Join(testDir, tt.schemaDir, "schema.json"), GeneratePackage)
			assert.NoError(t, err)

			// create files, FIXME: temporary, use PULUMI_ACCEPT instead
			/*for path_, file := range files {
				fullPath := filepath.Join(testDir, tt.schemaDir, "jvm", path_)
				dir := filepath.Dir(fullPath)
				err := os.MkdirAll(dir, 0777)
				if err != nil {
					t.Errorf("can't create dir: '%s': %s", dir, err)
				}
				out, err := os.Create(fullPath)
				if err != nil {
					t.Errorf("can't create file: '%s': %s", fullPath, err)
				}
				_, err = out.Write(file)
				if err != nil {
					_ = out.Close()
					t.Errorf("can't write file: '%s': %s", fullPath, err)
				}
				if err != out.Close() {
					t.Errorf("can't close file: '%s': %s", fullPath, err)
				}
			}*/
			// end of create files

			for path_, _ := range files {
				t.Logf("generated file: %s", path_)
			}

			dir := filepath.Join(testDir, tt.schemaDir)
			lang := "jvm"

			test.RewriteFilesWhenPulumiAccept(t, dir, lang, files)

			expectedFiles, err := test.LoadFiles(dir, lang, tt.expectedFiles)
			assert.NoError(t, err)

			test.ValidateFileEquality(t, files, expectedFiles)
		})
	}
}

func expectedFilesPath(elem ...string) string {
	parts := []string{gradleProjectPath(), "io", "pulumi"}
	parts = append(parts, elem...)
	return path.Join(parts...)
}
