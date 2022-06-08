// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"path/filepath"
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
)

type jbang struct{}

var _ javaExecutorFactory = &jbang{}

func (j jbang) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
	ok, err := j.isJBangProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	script := opts.binary
	if script == "" {
		script = "src/main.java"
	}
	probePaths := []string{opts.useExecutor}
	if opts.useExecutor == "" {
		probePaths = []string{"./jbang", "jbang"}
	}
	cmd, err := lookupPath(opts.wd, probePaths...)
	if err != nil {
		return nil, err
	}
	return j.newJBangExecutor(cmd, script)
}

func (j jbang) isJBangProject(opts javaExecutorOptions) (bool, error) {
	if opts.binary != "" {
		suffix := strings.ToLower(filepath.Ext(opts.binary))
		switch suffix {
		case ".java", ".kt", ".groovy":
			return true, nil
		}
	}
	if strings.Contains(opts.useExecutor, "jbang") {
		return true, nil
	}
	return fileExists(opts.wd, "jbang.properties")
}

func (j jbang) newJBangExecutor(cmd string, script string) (*javaExecutor, error) {
	logging.V(3).Infof("Making JBang executor for: `%s`", cmd)
	return &javaExecutor{
		cmd:       cmd,
		buildArgs: []string{"--quiet", "build", script},
		runArgs:   []string{"--quiet", "run", script},
		pluginArgs: []string{
			"--quiet", "run",
			"--main=com.pulumi.bootstrap.internal.Main",
			script,
			"packages",
		},
	}, nil
}
