// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"html"
	"io"
	"regexp"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
)

// formatForeignBlockComment formats comment as a java block comment for use in the
// generated SDK. formatForeignBlockComment is responsible for ensuring that comment is
// safe to embed in the generated SDK. That means that comment cannot contain any codes
// that escape javadoc or the embedding comment.
func formatForeignBlockComment(comment, indent string) string {
	return formatForeignBlockCommentFrom(comment, 0, indent)
}

var replaceAtRegexp = regexp.MustCompile("( ?)(@+)")

// formatForeignBlockCommentFrom is like [formatForeignBlockComment], except that it
// unconditionally accepts the first idx bytes of comment.
func formatForeignBlockCommentFrom(comment string, idx int, indent string) string {
	comment = codegen.FilterExamples(comment, "java")

	// Escape a '@' with a '{@literal @}'.
	//
	// This inserts a space before the new '@', so we attempt to remove a leading
	// space when possible. It not possible, we do it anyway.
	escapeAtLiteral := func(s string) string {
		return replaceAtRegexp.ReplaceAllString(s, "{@literal $2}")
	}

	comment = comment[:idx] + mapCommentHelper(comment[idx:],
		// Code
		func(s string) string {
			s = strings.TrimPrefix(s, "```java")
			s = strings.TrimSuffix(s, "```")

			// Javadoc doesn't have a way to escape an arbitrarily code
			// snippet. The best we have is:
			//
			// <pre>
			// {@code
			// THE CODE GOES HERE:
			// - '@' needs to be escaped.
			// - '&', '<', '>' does not need to be escaped.
			// }
			// </pre>
			var b strings.Builder
			b.WriteString("<pre>\n{@code")
			b.WriteString(escapeAtLiteral(s))
			b.WriteString("}\n</pre>")
			return b.String()
		},
		// Non-code
		func(s string) string { return html.EscapeString(escapeAtLiteral(s)) },
	)

	return formatBlockComment(comment, indent)
}

// mapCommentHelper maps 2 functions over `comment`. It maps `code` over the section of
// `comment` that is made up of Java code (as defined by markdown code fences) and
// `nonCode` over all other parts of `comment`.
func mapCommentHelper(comment string, code, nonCode func(string) string) string {
	var dst strings.Builder
	dst.Grow(len(comment))

	for {
		const (
			codeStartMarker = "```java"
			codeEndMarker   = "```"
		)

		codeStart := strings.Index(comment, codeStartMarker)
		if codeStart == -1 {
			break
		}

		codeStartOffset := codeStart + len(codeStartMarker)

		codeEnd := strings.Index(comment[codeStartOffset:], codeEndMarker)
		if codeEnd == -1 {
			break
		}
		codeEnd += codeStartOffset // Make codeEnd relative to comment
		codeEndOffset := codeEnd + len(codeEndMarker)

		// We have now found a code block:

		// Write all non-copied text proceeding the code block
		if codeStart != 0 {
			dst.WriteString(nonCode(comment[:codeStart]))
		}

		// Then write the code block itself
		dst.WriteString(code(comment[codeStart:codeEndOffset]))
		// Then adjust copiedTo to start after the code block
		comment = comment[codeEndOffset:]
	}

	// Copy any remaining non-code into the dst buffer.
	if comment != "" {
		dst.WriteString(nonCode(comment))
	}

	return dst.String()
}

func formatBlockComment(comment, indent string) string {
	prefix := fmt.Sprintf("%s * ", indent)
	comment = strings.ReplaceAll(comment, "*/", "*{@literal /}")

	lines := strings.Split(comment, "\n")
	if nth := len(lines) - 1; nth >= 0 && lines[nth] != "" {
		lines = append(lines, "")
	}

	comment = strings.Join(lines, "\n"+prefix)
	return prefix + comment
}

func fprintf(w io.Writer, format string, args ...interface{}) {
	_, err := fmt.Fprintf(w, format, args...)
	if err != nil {
		panic("error writing format string [format=" + format + "]")
	}
}
