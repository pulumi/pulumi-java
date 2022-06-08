// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"strings"
)

type maven struct{}

var _ javaExecutorFactory = &maven{}

func (m maven) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
	ok, err := m.isMavenProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.useExecutor}
	if opts.useExecutor == "" {
		probePaths = []string{"./mvnw", "mvn"}
	}
	cmd, err := lookupPath(opts.wd, probePaths...)
	if err != nil {
		return nil, err
	}
	return m.newMavenExecutor(cmd)
}

func (maven) isMavenProject(opts javaExecutorOptions) (bool, error) {
	if strings.Contains(opts.useExecutor, "mvn") {
		return true, nil
	}
	return fileExists(opts.wd, "pom.xml")
}

func (maven) newMavenExecutor(cmd string) (*javaExecutor, error) {
	return &javaExecutor{
		cmd:       cmd,
		buildArgs: []string{"--no-transfer-progress", "compile"},
		runArgs:   []string{"--no-transfer-progress", "compile", "exec:java"},
		pluginArgs: []string{
			/* move normal output to STDERR, because we need STDOUT for JSON with plugin results */
			"-Dorg.slf4j.simpleLogger.logFile=System.err",
			"--no-transfer-progress", "compile", "exec:java",
			"-DmainClass=com.pulumi.bootstrap.internal.Main",
			"-DmainArgs=packages",
		},
	}, nil
}
