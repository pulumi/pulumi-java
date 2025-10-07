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

package java

import (
	"fmt"
	"html"
	"io"
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

// formatForeignBlockCommentFrom is like [formatForeignBlockComment], except that it
// unconditionally accepts the first idx bytes of comment.
func formatForeignBlockCommentFrom(comment string, idx int, indent string) string {
	comment = codegen.FilterExamples(comment, "java")
	comment = comment[:idx] + mapComment(comment[idx:], escapeCode, escapeNonCode)
	return formatBlockComment(comment, indent)
}

// Minimally escapes a piece of Java code for use within a Javadoc comment.
//
// Returned code is wrapped in a Javadoc `{@code ...}` block inside an HTML `<pre>` tag. This provides the best
// out-of-the-box experience when it comes to not having to escape a large number of characters (e.g. HTML is fine
// as-is).
func escapeCode(code string) string {
	const (
		header = "<pre>\n{@code"
		footer = "}\n</pre>"
	)

	if !codeNeedsEscaping(code) {
		return header + code + footer
	}

	w := &codeJavadocWriter{}
	for _, rune := range code {
		w.WriteRune(rune)
	}

	return "<pre>\n{@code" + w.String() + "}\n</pre>"
}

// Returns true if and only if a piece of code needs escaping. That is:
//
// - It contains an '@' character.
// - It contains a '*/' sequence.
// - It contains unbalanced braces.
//
// All other code should be acceptable as-is within a Javadoc `{@code ...}` block.
func codeNeedsEscaping(code string) bool {
	openBraces := 0
	lastSawAsterisk := false
	for _, rune := range code {
		if rune == '@' || (rune == '/' && lastSawAsterisk) {
			return true
		} else if rune == '{' {
			openBraces++
		} else if rune == '}' {
			openBraces--
		}

		if openBraces < 0 {
			return true
		}

		lastSawAsterisk = rune == '*'
	}

	return openBraces != 0
}

// Escapes a piece of text for using within a Javadoc comment.
func escapeNonCode(nonCode string) string {
	w := &plainJavadocWriter{}
	for _, rune := range nonCode {
		w.WriteRune(rune)
	}

	return w.String()
}

// A plainJavadocWriter can be used to build a string safe for embedding in a Javadoc comment as text. It performs the
// following escaping:
//
// * '@' characters are escaped as '{@literal @}'.
// * The pair '*/' is escaped as '*&#47;' (the slash being HTML-encoded).
// * All other characters are HTML-encoded as-is.
type plainJavadocWriter struct {
	// The underlying string builder used to build the escaped string.
	b strings.Builder
	// The current state of the writer.
	state plainJavadocWriterState
	// True if and only if the last character written was an asterisk. This is used to detect "*/" sequences that need
	// to be escaped.
	lastWroteAsterisk bool
}

// A state that a plainJavadocWriter can be in.
type plainJavadocWriterState int

const (
	// The writer is currently writing plain text.
	plainJavadocText plainJavadocWriterState = iota
	// The writer is currently writing a literal sequence.
	plainJavadocLiteral
)

// WriteRune writes a rune to the writer, escaping it as necessary.
func (w *plainJavadocWriter) WriteRune(r rune) {
	switch w.state {
	case plainJavadocText:
		// If we are writing plain text and we encounter an '@', we open a literal sequence in order to write it. If we
		// encounter a '*/' sequence, we escape the slash and continue. Otherwise, we HTML-encode the character.
		if r == '@' {
			w.b.WriteString("{@literal @")
			w.state = plainJavadocLiteral
		} else if r == '/' && w.lastWroteAsterisk {
			w.b.WriteString("&#47;")
		} else {
			w.b.WriteString(html.EscapeString(string(r)))
		}
	case plainJavadocLiteral:
		// If we are already inside a literal and we encounter another '@', we can just write it as-is. If we see any
		// other character, we need to close the literal, write the rune and return to the text state.
		if r == '@' {
			w.b.WriteRune('@')
		} else {
			w.b.WriteRune('}')
			w.b.WriteString(html.EscapeString(string(r)))
			w.state = plainJavadocText
		}
	}

	w.lastWroteAsterisk = r == '*'
}

// Returns the string of escaped text that has been written to the writer so far.
func (w *plainJavadocWriter) String() string {
	return w.b.String()
}

// A codeJavadocWriter can be used to build a string safe for embedding in a Javadoc comment as code. It assumes that
// its content will be wrapped in a `<pre>{@code ...}</pre>` block and escapes '@', '{', '}', and '*/' sequences. In
// general, the only safe way to handle these characters is to exit the `@code` block temporarily, print them out (in
// some cases still requiring escapes) and then re-enter the code block. This generally harms the readability of the
// generated comment at the expense of it being renderable by Javadoc and the majority of IDEs that support e.g. docs on
// hover.
//
// In cases where multiple escape-requiring characters appear in a sequence, we "stay outside the `@code` block" as long
// as possible in order to avoid hampering readability even further.
type codeJavadocWriter struct {
	// The underlying string builder used to build the escaped string.
	b strings.Builder
	// The current state of the writer.
	state codeJavadocWriterState
	// True if and only if the last character written was an asterisk. This is used to detect "*/" sequences that need
	// to be escaped.
	lastWroteAsterisk bool
}

// A state that a codeJavadocWriter can be in.
type codeJavadocWriterState int

const (
	// The writer is currently writing code.
	codeJavadocCode codeJavadocWriterState = iota
	// The writer is currently writing text.
	codeJavadocText
	// The writer is currently writing a literal sequence.
	codeJavadocLiteral
)

// WriteRune writes a rune to the writer, escaping it as necessary.
func (w *codeJavadocWriter) WriteRune(r rune) {
	switch w.state {
	case codeJavadocCode:
		// If we are writing code and we encounter an '@', we open a literal sequence in order to write it. If we
		// encounter a brace, we enter a text state and write it out. If we see a '*/' sequence, we escape the slash in
		// a text state and continue. Otherwise, we write the character as-is.
		if r == '@' {
			w.b.WriteString("}{@literal @")
			w.state = codeJavadocLiteral
		} else if r == '{' || r == '}' {
			w.b.WriteRune('}')
			w.b.WriteRune(r)
			w.state = codeJavadocText
		} else if r == '/' && w.lastWroteAsterisk {
			w.b.WriteString("}&#47;")
			w.state = codeJavadocText
		} else {
			w.b.WriteRune(r)
		}
	case codeJavadocText:
		// If we are writing text and we encounter an '@', we open a literal sequence in order to write it. If we
		// encounter a brace, we write it as-is, allowing us to write multiple braces without repeatedly entering and
		// leaving the code state. All other characters return us to the code state before continuing.
		switch r {
		case '@':
			w.b.WriteString("{@literal @")
			w.state = codeJavadocLiteral
		case '{', '}':
			w.b.WriteRune(r)
		default:
			w.b.WriteString("{@code")
			if r != '\n' {
				w.b.WriteRune(' ')
			}
			w.b.WriteRune(r)
			w.state = codeJavadocCode
		}
	case codeJavadocLiteral:
		// If we are already inside a literal and we encounter another '@', we can just write it as-is. If we see a
		// brace, we enter a text state and write it out. If we see any other character, we need to close the literal,
		// write the rune and return to the text state.
		switch r {
		case '@':
			w.b.WriteRune('@')
		case '{', '}':
			w.b.WriteRune('}')
			w.b.WriteRune(r)
			w.state = codeJavadocText
		default:
			w.b.WriteString("}{@code")
			if r != '\n' {
				w.b.WriteRune(' ')
			}
			w.b.WriteRune(r)
			w.state = codeJavadocCode
		}
	}

	w.lastWroteAsterisk = r == '*'
}

// Returns the string of escaped code that has been written to the writer so far.
func (w *codeJavadocWriter) String() string {
	return w.b.String()
}

// mapComment maps 2 functions over `comment`. It maps `codeF` over the section of `comment` that is made up of
// Java code (as defined by markdown code fences) and `nonCodeF` over all other parts of `comment`.
func mapComment(comment string, codeF, nonCodeF func(string) string) string {
	var dst strings.Builder
	dst.Grow(len(comment))

	for {
		// At any given point in this loop, we are looking for the next code block in the comment. We do this by hunting
		// for the start marker and from there an end marker, using these to calculate the placement of the code within:
		//
		// ... ```java\npublic class C{}\n``` ...
		//     ^  ^                       ^  ^
		//     |  |                       |  |
		//     codeStartMarkerIndex       |  codeEndIndex
		//        |                       |
		//        codeStartIndex          codeEndMarkerIndex
		//
		// The code inside (trimmed of the markers) is passed to `codeF` for processing. Once we are done, we move to
		// the point after the code block (`codeEndIndex`) and go around for another iteration. Any non-code that
		// appears before the block (when the `codeStartMarkerIndex` is not zero) or after the last block (outside the
		// loop) is passed to `nonCodeF`.

		const (
			codeStartMarker = "```java"
			codeEndMarker   = "```"
		)

		codeStartMarkerIndex := strings.Index(comment, codeStartMarker)
		if codeStartMarkerIndex == -1 {
			break
		}

		codeStartIndex := codeStartMarkerIndex + len(codeStartMarker)

		codeEndMarkerIndex := strings.Index(comment[codeStartIndex:], codeEndMarker)
		if codeEndMarkerIndex == -1 {
			break
		}

		codeEndMarkerIndex += codeStartIndex
		codeEndIndex := codeEndMarkerIndex + len(codeEndMarker)

		code := comment[codeStartIndex:codeEndMarkerIndex]

		if codeStartMarkerIndex != 0 {
			dst.WriteString(nonCodeF(comment[:codeStartMarkerIndex]))
		}

		dst.WriteString(codeF(code))
		comment = comment[codeEndIndex:]
	}

	// Copy any remaining non-code into the dst buffer.
	if comment != "" {
		dst.WriteString(nonCodeF(comment))
	}

	return dst.String()
}

func formatBlockComment(comment, indent string) string {
	prefix := fmt.Sprintf("%s * ", indent)

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
