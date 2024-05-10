// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"os"
	"testing"

	"github.com/hexops/autogold/v2"
	"github.com/stretchr/testify/require"
)

func TestFormatForignComments(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		expected autogold.Value
	}{
		{
			name:     "plain text",
			input:    `just some text`,
			expected: autogold.Expect(" * just some text\n * "),
		},
		{
			name:  "code block",
			input: "```java\nsome code\n```\n",
			expected: autogold.Expect(` * <pre>
 * {@code
 * some code
 * }
 * </pre>
 * `),
		},
		{
			name:     "repro auth0 failure",
			input:    openFile(t, "./testdata/auth0_connection_description.md"),
			expected: AutoFile{"./testdata/auth0_connection_description_expected.md"},
		},
		{
			name:  "mix code and plain",
			input: "my comment\n```java\nmy code\n```\nanother comment\n",
			expected: autogold.Expect(` * my comment
 * <pre>
 * {@code
 * my code
 * }
 * </pre>
 * another comment
 * `),
		},
		{
			name: "@ escapes",
			input: `@foo
` + "```java" + `
@ does not need to be escaped
& must be escaped
< and > must be escaped
` + "```" + `
@foo
`,
			expected: autogold.Expect(` * {@literal @}foo
 * <pre>
 * {@code
 * {@literal @} does not need to be escaped
 * & must be escaped
 * < and > must be escaped
 * }
 * </pre>
 * {@literal @}foo
 * `),
		},
		{
			name:  "tight bounds",
			input: "foo-```java-fizz-```-buzz",
			expected: autogold.Expect(` * foo-<pre>
 * {@code-fizz-}
 * </pre>-buzz
 * `),
		},
		{
			name: "escape @ variations",
			input: `
-@foo-
- @foo- The space here should go away since it will *always* be inserted by the {@literal} escape
- @@@foo -
`,
			//nolint:lll
			expected: autogold.Expect(` * -{@literal @}foo-
 * -{@literal @}foo- The space here should go away since it will *always* be inserted by the {{@literal @}literal} escape
 * -{@literal @@@}foo -
 * `),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			actual := formatForeignBlockComment(tt.input, "")
			tt.expected.Equal(t, actual)
		})
	}
}

func openFile(t *testing.T, path string) string {
	contents, err := os.ReadFile(path)
	require.NoError(t, err)
	return string(contents)
}

type AutoFile struct{ Path string }

func (a AutoFile) Equal(t *testing.T, got any, opts ...autogold.Option) {
	s, ok := got.(string)
	if !ok {
		t.Fatalf("AutoFile must receive an argument of type %T, found %T", s, got)
	}
	autogold.ExpectFile(t, autogold.Raw(s), opts...)
}
