// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"html"
	"io"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
)

func formatBlockComment(comment string, indent string) string {
	prefix := fmt.Sprintf("%s * ", indent)
	comment = strings.ReplaceAll(comment, "*/", "*{@literal /}")
	comment = codegen.FilterExamples(comment, "java")
	comment = html.EscapeString(comment)
	comment = strings.Join(strings.Split(comment, "\n"), "\n"+prefix)
	return prefix + comment
}

func fprintf(w io.Writer, format string, args ...interface{}) {
	_, err := fmt.Fprintf(w, format, args...)
	if err != nil {
		panic("error writing format string [format=" + format + "]")
	}
}
