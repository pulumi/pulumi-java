// Copyright 2022, Pulumi Corporation.  All rights reserved.

package fsys

import (
	"io/fs"
)

// File system and os interface. Helps test executor detection.
// Extends fs.FS with ability to explore parent folders, search for
// executables in PATH.
type ParentFS interface {
	fs.FS

	Path() string
	HasParent() bool
	Parent() ParentFS

	// Like LookPath from os/exec but local executable paths are
	// interpreted relative to current FS.
	LookPath(string) (string, error)
}
