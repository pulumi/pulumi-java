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
				path.Join(expectedFilesPath("plant"), "inputs", "ContainerArgs.java"),
				path.Join(expectedFilesPath("plant"), "outputs", "Container.java"),
			},
		},
		{
			"External resource schema",
			"external-resource-schema",
			[]string{
				path.Join(expectedFilesPath("example"), "ArgFunction.java"),
				path.Join(expectedFilesPath("example"), "Cat.java"),
				path.Join(expectedFilesPath("example"), "Component.java"),
				path.Join(expectedFilesPath("example"), "inputs", "PetArgs.java"),
				path.Join(expectedFilesPath("example"), "Workload.java"),
			},
		},
		{
			"Simple schema with plain properties",
			"simple-plain-schema",
			[]string{
				path.Join(expectedFilesPath("example"), "inputs", "Foo.java"),
				path.Join(expectedFilesPath("example"), "inputs", "FooArgs.java"),
				path.Join(expectedFilesPath("example"), "outputs", "Foo.java"),
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
