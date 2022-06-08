// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"path/filepath"
	"strings"
)

type jarexec struct{}

var _ javaExecutorFactory = &jarexec{}

func (j jarexec) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
	if opts.binary == "" {
		return nil, nil
	}
	suffix := strings.ToLower(filepath.Ext(opts.binary))
	if suffix != ".jar" {
		return nil, nil
	}
	cmd, err := lookupPath(opts.wd, "java")
	if err != nil {
		return nil, err
	}
	return j.newJarExecutor(cmd, opts.binary)
}

func (jarexec) newJarExecutor(cmd string, path string) (*javaExecutor, error) {
	return &javaExecutor{
		cmd:        cmd,
		buildArgs:  nil, // not supported
		runArgs:    []string{"-jar", filepath.Clean(path)},
		pluginArgs: []string{"-cp", filepath.Clean(path), "com.pulumi.bootstrap.internal.Main", "packages"},
	}, nil
}
