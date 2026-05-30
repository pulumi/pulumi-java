// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"strings"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

type sbt struct{}

var _ javaExecutorFactory = &sbt{}

func (s sbt) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	ok, err := s.isSbtProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./sbt", "sbt"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	return s.newSbtExecutor(cmd)
}

func (sbt) isSbtProject(opts JavaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "sbt") {
		return true, nil
	}
	sbtMarkers := []string{
		"project/build.properties",
		"build.sbt",
	}
	for _, p := range sbtMarkers {
		exists, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return false, err
		}
		if exists {
			return true, nil
		}
	}
	return false, nil
}

func (sbt) newSbtExecutor(cmd string) (*JavaExecutor, error) {
	return &JavaExecutor{
		Cmd:       cmd,
		BuildArgs: []string{"-batch", "compile"},
		RunArgs:   []string{"-batch", "run"},
		PluginArgs: []string{
			/* STDOUT needs to be clean of sbt output,
			   because we expect a JSON with plugin
			   results */
			"-batch", "-error",
			"'; set outputStrategy := Some(StdoutOutput) ; runMain com.pulumi.bootstrap.internal.Main packages'",
		},
	}, nil
}
