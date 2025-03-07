// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"path/filepath"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

type jarexec struct{}

var _ javaExecutorFactory = &jarexec{}

func (j jarexec) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	if opts.Binary == "" {
		return nil, nil
	}
	suffix := strings.ToLower(filepath.Ext(opts.Binary))
	if suffix != ".jar" {
		return nil, nil
	}
	cmd, err := fsys.LookPath(opts.WD, "java")
	if err != nil {
		return nil, err
	}
	return j.newJarExecutor(cmd, opts.Binary)
}

func (jarexec) newJarExecutor(cmd string, path string) (*JavaExecutor, error) {
	return &JavaExecutor{
		Cmd:          cmd,
		BuildArgs:    nil, // not supported
		RunArgs:      []string{"-jar", filepath.Clean(path)},
		AnalyzerArgs: []string{"-cp", filepath.Clean(path), "com.pulumi.bootstrap.internal.Main", "analyzer"},
		PluginArgs:   []string{"-cp", filepath.Clean(path), "com.pulumi.bootstrap.internal.Main", "packages"},
	}, nil
}
