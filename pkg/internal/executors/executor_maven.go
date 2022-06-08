// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"strings"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

type maven struct{}

var _ javaExecutorFactory = &maven{}

func (m maven) tryConfigureExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	ok, err := m.isMavenProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./mvnw", "mvn"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	return m.newMavenExecutor(cmd)
}

func (maven) isMavenProject(opts JavaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "mvn") {
		return true, nil
	}
	return fsys.FileExists(opts.WD, "pom.xml")
}

func (maven) newMavenExecutor(cmd string) (*JavaExecutor, error) {
	return &JavaExecutor{
		Cmd:       cmd,
		BuildArgs: []string{"--no-transfer-progress", "compile"},
		RunArgs:   []string{"--no-transfer-progress", "compile", "exec:java"},
		PluginArgs: []string{
			/* move normal output to STDERR, because we need STDOUT for JSON with plugin results */
			"-Dorg.slf4j.simpleLogger.logFile=System.err",
			"--no-transfer-progress", "compile", "exec:java",
			"-DmainClass=com.pulumi.bootstrap.internal.Main",
			"-DmainArgs=packages",
		},
	}, nil
}
