package java

import (
	"testing"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
	"github.com/stretchr/testify/assert"
)

func TestTypeShape_ToTree(t *testing.T) {
	tests := []struct {
		name    string
		ts      TypeShape
		refs    string
		tree    string
		imports string
	}{
		{
			name:    "Empty",
			ts:      TypeShape{},
			refs:    "",
			tree:    "[]",
			imports: "",
		},
		{
			name: "String",
			ts: TypeShape{
				Type: names.String,
			},
			refs:    "String.class",
			tree:    "[0]",
			imports: "import java.lang.String;",
		},
		{
			name: "List<String>",
			ts: TypeShape{
				Type:       names.List,
				Parameters: []TypeShape{{Type: names.String}},
			},
			refs:    "List.class,String.class",
			tree:    "[0,1]",
			imports: "import java.lang.String;\nimport java.util.List;",
		},
		{
			name: "Either<Integer,String>",
			ts: TypeShape{
				Type: names.Either,
				Parameters: []TypeShape{
					{Type: names.Integer},
					{Type: names.String},
				},
			},
			refs:    "Either.class,Integer.class,String.class",
			tree:    "[0,1,2]",
			imports: "import com.pulumi.core.Either;\nimport java.lang.Integer;\nimport java.lang.String;",
		},
		{
			name: "List<List<List<String>>>",
			ts: TypeShape{
				Type: names.List,
				Parameters: []TypeShape{
					{Type: names.List, Parameters: []TypeShape{
						{
							Type: names.List, Parameters: []TypeShape{
								{Type: names.String},
							},
						},
					}},
				},
			},
			refs:    "List.class,String.class",
			tree:    "[0,[0,[0,1]]]",
			imports: "import java.lang.String;\nimport java.util.List;",
		},
		{
			name: "Either<Map<String,List<String>>,Map<String,List<String>>>",
			ts: TypeShape{
				Type: names.Either,
				Parameters: []TypeShape{
					{Type: names.Map, Parameters: []TypeShape{
						{Type: names.String},
						{Type: names.List, Parameters: []TypeShape{{Type: names.String}}},
					}},
					{Type: names.Map, Parameters: []TypeShape{
						{Type: names.List, Parameters: []TypeShape{{Type: names.String}}},
						{Type: names.String},
					}},
				},
			},
			refs:    "Either.class,Map.class,String.class,List.class",
			tree:    "[0,[1,2,[3,2]],[1,[3,2],2]]",
			imports: "import com.pulumi.core.Either;\nimport java.lang.String;\nimport java.util.List;\nimport java.util.Map;",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pkg := names.Ident("com").FQN().Dot("pulumi").Dot("tests")
			cls := names.Ident("TypeShapeTest")
			imports := names.NewImports(pkg, cls)
			gotRefs, gotTree := tt.ts.ToTree(imports)
			gotImports := imports.ImportCode()
			assert.Equal(t, tt.refs, gotRefs)
			assert.Equal(t, tt.tree, gotTree)
			assert.Equal(t, tt.imports, gotImports)
		})
	}
}
