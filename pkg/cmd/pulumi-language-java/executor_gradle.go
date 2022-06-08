// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"path/filepath"
	"strings"
)

type gradle struct{}

var _ javaExecutorFactory = &gradle{}

func (g gradle) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
	ok, err := g.isGradleProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.useExecutor}
	if opts.useExecutor == "" {
		probePaths = []string{"gradle", filepath.Join(opts.wd, "gradlew")}
	}
	cmd, err := lookupPath(probePaths...)
	if err != nil {
		return nil, err
	}
	return g.newGradleExecutor(cmd)
}

func (gradle) isGradleProject(opts javaExecutorOptions) (bool, error) {
	if strings.Contains(opts.useExecutor, "gradle") {
		return true, nil
	}
	gradleMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
		"build.gradle",
	}
	for _, p := range gradleMarkers {
		isGradle, err := fileExists(filepath.Join(opts.wd, p))
		if err != nil {
			return false, err
		}
		if isGradle {
			return true, nil
		}
	}
	return false, nil
}

func (gradle) newGradleExecutor(cmd string) (*javaExecutor, error) {
	return &javaExecutor{
		cmd:       cmd,
		buildArgs: []string{"build", "--console=plain"},
		runArgs:   []string{"run", "--console=plain"},
		pluginArgs: []string{
			/* STDOUT needs to be clean of gradle output, because we expect a JSON with plugin results */
			"-q", // must first due to a bug https://github.com/gradle/gradle/issues/5098
			"run", "--console=plain",
			"-PmainClass=com.pulumi.bootstrap.internal.Main",
			"--args=packages",
		},
	}, nil
}
