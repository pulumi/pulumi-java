// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestFormatForignComments(t *testing.T) {
	tests := []struct {
		name            string
		input, expected string
	}{
		{
			name:     "plain text",
			input:    `just some text`,
			expected: " * just some text\n * ",
		},
		{
			name:  "code block",
			input: "```java\nsome code\n```\n",
			expected: ` * {@code
 * some code
 * }
 * `,
		},
		{
			name:     "repro auth0 failure",
			input:    openFile(t, "./testdata/auth0_connection_description.md"),
			expected: openFile(t, "./testdata/auth0_connection_description_expected.md"),
		},
		{
			name:  "mix code and plain",
			input: "my comment\n```java\nmy code\n```\nanother comment\n",
			expected: ` * my comment
 * {@code
 * my code
 * }
 * another comment
 * `,
		},
		{
			name:  "@ escapes",
			input: "@foo\n```java\n@foo\n```\n@foo\n",
			expected: ` * {@literal @}foo
 * {@code
 * @foo
 * }
 * {@literal @}foo
 * `,
		},
		{
			name:     "tight bounds",
			input:    "foo-```java-fizz-```-buzz",
			expected: " * foo-{@code-fizz-}-buzz\n * ",
		},
	}

	for _, tt := range tests {
		t.Run("", func(t *testing.T) {
			actual := formatForeignBlockComment(tt.input, "")
			assert.Equal(t, tt.expected, actual)
		})
	}
}

func openFile(t *testing.T, path string) string {
	contents, err := os.ReadFile(path)
	require.NoError(t, err)
	return string(contents)
}
