// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
	"path/filepath"
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
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
	gradleRoot, err := g.findGradleRoot(opts.wd)
	if err != nil {
		return nil, err
	}
	probePaths := []string{opts.useExecutor}
	if opts.useExecutor == "" {
		probePaths = []string{filepath.Join(gradleRoot, "gradlew"), "gradle"}
	}
	cmd, err := lookupPath(probePaths...)
	if err != nil {
		return nil, err
	}
	logging.V(3).Infof("Detected Gradle Java executor: `%s`", cmd)
	return g.newGradleExecutor(cmd)
}

func (gradle) findGradleRoot(wd string) (string, error) {
	gradleRootMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
	}
	d := wd
	for {
		for _, p := range gradleRootMarkers {
			isGradleRoot, err := fileExists(filepath.Join(d, p))
			if err != nil {
				return "", err
			}
			if isGradleRoot {
				return d, nil
			}
		}
		nextD := filepath.Dir(d)
		if nextD == d {
			return "", fmt.Errorf("No ancestor dir with settings.gradle(.kts)? found"+
				" for %s", wd)
		}
		d = nextD
	}
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
