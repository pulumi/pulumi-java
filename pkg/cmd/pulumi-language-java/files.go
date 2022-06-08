// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

// File system and os interface. Helps test executor detection.
// Extends fs.FS with ability to explore parent folders, search for
// executables in PATH.
type parentFS interface {
	fs.FS

	Path() string
	HasParent() bool
	Parent() parentFS

	// Like LookPath from os/exec but local executable paths are
	// interpreted relative to current FS.
	LookPath(string) (string, error)
}

type osDirFS struct {
	dir string
}

var _ parentFS = &osDirFS{}

var _ fs.StatFS = &osDirFS{}

func (o osDirFS) fs() fs.FS {
	return os.DirFS(o.dir)
}

func (o osDirFS) Path() string {
	return o.dir
}

func (o osDirFS) Open(name string) (fs.File, error) {
	return o.fs().Open(name)
}

func (o osDirFS) Stat(name string) (fs.FileInfo, error) {
	return fs.Stat(o.fs(), name)
}

func (o osDirFS) HasParent() bool {
	pDir := filepath.Dir(o.dir)
	if pDir == o.dir {
		return false
	}
	return true
}

func (o osDirFS) Parent() parentFS {
	pDir := filepath.Dir(o.dir)
	return &osDirFS{pDir}
}

func (o osDirFS) LookPath(exe string) (string, error) {
	if strings.Contains(exe, "/") {
		return exec.LookPath(filepath.Join(o.dir, exe))
	}
	return exec.LookPath(exe)
}

func fileExists(dir fs.FS, path string) (bool, error) {
	_, err := fs.Stat(dir, path)
	if err != nil && os.IsNotExist(err) {
		return false, nil
	}
	return true, err
}

func lookupPath(dir parentFS, files ...string) (string, error) {
	var lastError error
	for _, file := range files {
		pathExec, err := dir.LookPath(file)
		if err == nil {
			return pathExec, nil
		}
		lastError = err
	}
	return "", fmt.Errorf("could not find %s on the $PATH: %w",
		strings.Join(files, ", "), lastError)
}
