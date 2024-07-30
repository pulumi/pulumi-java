// Copyright 2022-2024, Pulumi Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//nolint:goconst
package java

import (
	"os"
	"testing"

	"github.com/hexops/autogold/v2"
	"github.com/stretchr/testify/require"
)

func TestFormatForeignComments(t *testing.T) {
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
@ needs to be escaped
& doesn't need to be escaped
< and > don't need to be escaped
` + "```" + `
@foo
`,
			expected: autogold.Expect(` * {@literal @}foo
 * <pre>
 * {@code
 * }{@literal @}{@code  needs to be escaped
 * & doesn't need to be escaped
 * < and > don't need to be escaped
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
- @Foo @ bar
- Foo @ bar
- Foo bar @@
`,
			expected: autogold.Expect(` * - {@literal @}Foo {@literal @} bar
 * - Foo {@literal @} bar
 * - Foo bar {@literal @@}
 * `),
		},
		{
			name:  "balanced braces text",
			input: "This is some text with {balanced} braces",
			expected: autogold.Expect(` * This is some text with {balanced} braces
 * `),
		},
		{
			name:  "unbalanced braces text",
			input: "This is some text with {unbalanced braces",
			expected: autogold.Expect(` * This is some text with {unbalanced braces
 * `),
		},
		{
			name: "balanced braces code",
			input: "```java" + `
This is some code with {balanced} braces
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some code with {balanced} braces
 * }
 * </pre>
 * `),
		},
		{
			name: "unbalanced braces code",
			input: "```java" + `
This is some code with {unbalanced braces
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some code with }{{@code unbalanced braces
 * }
 * </pre>
 * `),
		},
		{
			name: "balanced braces code with @",
			input: "```java" + `
This is some code with {balanced} braces and @
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some code with }{{@code balanced}}{@code  braces and }{@literal @}{@code
 * }
 * </pre>
 * `),
		},
		{
			name: "unbalanced braces code with @",
			input: "```java" + `
This is some @@ code with {unbalanced braces and @
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some }{@literal @@}{@code  code with }{{@code unbalanced braces and }{@literal @}{@code
 * }
 * </pre>
 * `),
		},
		{
			name: "code with Javadoc-looking contents",
			input: "```java" + `
This is some code with {@code stuff} in it
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some code with }{{@literal @}{@code code stuff}}{@code  in it
 * }
 * </pre>
 * `),
		},
		{
			name: "code with repeated escapes",
			input: "```java" + `
This is some @@ code with {{ repeated escapes }} that should be @@ chained together
` + "```",
			// nolint:lll
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some }{@literal @@}{@code  code with }{{{@code  repeated escapes }}}{@code  that should be }{@literal @@}{@code  chained together
 * }
 * </pre>
 * `),
		},
		{
			name: "code with comment terminators",
			input: "```java" + `
This is some code with */ in it, as well as {@code stuff}
` + "```",
			expected: autogold.Expect(` * <pre>
 * {@code
 * This is some code with *}&#47;{@code  in it, as well as }{{@literal @}{@code code stuff}}{@code
 * }
 * </pre>
 * `),
		},
		{
			name: "messy code and text",
			input: `
Foo {} @@bar */

This is */ @*/

<html>Should be escaped</html>

` + "```java" + `
This is some @code with */*/ in it, as well as {{@code stuff}

xx @SuppressWarnings
public static void main(String[] args) {}

public static bar(String s) {
  baz(s, s, s);
}

x @ foo
`,
			// nolint:lll
			expected: autogold.Expect(` * Foo {} {@literal @@}bar *&#47;
 *
 * This is *&#47; {@literal @}*&#47;
 *
 * &lt;html&gt;Should be escaped&lt;/html&gt;
 *
 * <pre>
 * {@code
 * This is some }{@literal @}{@code code with *}&#47;{@code *}&#47;{@code  in it, as well as }{{{@literal @}{@code code stuff}}{@code
 *
 * xx }{@literal @}{@code SuppressWarnings
 * public static void main(String[] args) }{}{@code
 *
 * public static bar(String s) }{{@code
 *   baz(s, s, s);
 * }}{@code
 *
 * x }{@literal @}{@code  foo
 * }
 * </pre>
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
