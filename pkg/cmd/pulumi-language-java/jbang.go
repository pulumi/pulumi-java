// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"
)

const jbangGlobal = "jbang"
const jbangLocal = "./jbang"

func probeJBangExecutor() (string, error) {
	pwd, err := os.Getwd()
	if err != nil {
		return "", fmt.Errorf("could not get the working directory: %w", err)
	}
	files, err := ioutil.ReadDir(pwd)
	if err != nil {
		return "", fmt.Errorf("could not read the working directory: %w", err)
	}
	return resolveJBangPath(files)
}

func resolveJBangPath(wdFiles []fs.FileInfo) (string, error) {
	jbangPath := jbangGlobal
	// detect jbang wrapper
	for _, file := range wdFiles {
		if !file.IsDir() && file.Name() == jbangGlobal {
			jbangPath = jbangLocal
		}
	}
	return jbangPath, nil
}

func newJBangExecutor(cmd string, script string) (*javaExecutor, error) {
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
