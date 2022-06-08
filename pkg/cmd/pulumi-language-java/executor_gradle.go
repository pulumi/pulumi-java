// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"io/fs"
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
)

type gradle struct{}

var _ javaExecutorFactory = &gradle{}

func (g gradle) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
	ok, err := g.isGradleProject(opts.wd, opts)
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
		probePaths = []string{"./gradlew", "gradle"}
	}
	cmd, err := lookupPath(gradleRoot, probePaths...)
	if err != nil {
		return nil, err
	}
	logging.V(3).Infof("Detected Gradle Java executor (root: `%s`): `%s`",
		gradleRoot.Path(), cmd)
	return g.newGradleExecutor(cmd)
}

func (gradle) findGradleRoot(workdir parentFS) (parentFS, error) {
	gradleRootMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
	}
	d := workdir
	for {
		for _, p := range gradleRootMarkers {
			isGradleRoot, err := fileExists(d, p)
			if err != nil {
				return nil, err
			}
			if isGradleRoot {
				return d, nil
			}
		}
		if !d.HasParent() {
			// Abort search and assume workdir is the root
			return workdir, nil
		}
		d = d.Parent()
	}
}

func (gradle) isGradleProject(dir fs.FS, opts javaExecutorOptions) (bool, error) {
	if strings.Contains(opts.useExecutor, "gradle") {
		return true, nil
	}
	gradleMarkers := []string{
		"settings.gradle",
		"settings.gradle.kts",
		"build.gradle",
	}
	for _, p := range gradleMarkers {
		isGradle, err := fileExists(dir, p)
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
