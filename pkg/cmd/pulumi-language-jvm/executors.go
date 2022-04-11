// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"io/ioutil"
	"path/filepath"

	"github.com/pkg/errors"
)

type jvmExecutor struct {
	cmd        string
	buildArgs  []string
	runArgs    []string
	pluginArgs []string
}

func probeExecutor(pwd string) (string, error) {
	files, err := ioutil.ReadDir(pwd)
	if err != nil {
		return "", errors.Wrap(err, "could not read the working directory")
	}
	mvn := "mvn"
	// detect mvn wrapper
	for _, file := range files {
		if !file.IsDir() && file.Name() == "mvnw" {
			mvn = "./mvnw"
		}
	}
	gradle := "gradle"
	// detect gradle wrapper
	for _, file := range files {
		if !file.IsDir() && file.Name() == "gradlew" {
			gradle = "./gradlew"
		}
	}
	// detect maven or gradle
	for _, file := range files {
		if !file.IsDir() {
			switch file.Name() {
			case "pom.xml":
				return mvn, nil
			case "settings.gradle", "settings.gradle.kts":
				return gradle, nil
			}
		}
	}
	return "", errors.New("did not found an executor, expected one of: gradle (settings.gradle), maven (pom.xml)")
}

func resolveExecutor(exec string) (*jvmExecutor, error) {
	switch exec {
	case "gradle", "./gradlew":
		cmd, err := lookupPath(exec)
		if err != nil {
			return nil, err
		}
		return newGradleExecutor(cmd)
	case "mvn", "./mvnw":
		cmd, err := lookupPath(exec)
		if err != nil {
			return nil, err
		}
		return newMavenExecutor(cmd)
	default:
		return nil, errors.Errorf("did not recognize executor '%s', "+
			"expected one of: gradle, mvn, gradlew, mvnw", exec)
	}
}

func newGradleExecutor(cmd string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:       cmd,
		buildArgs: []string{"build", "--console=plain"},
		runArgs:   []string{"run", "--console=plain"},
		pluginArgs: []string{
			"-q", // must first due to a bug https://github.com/gradle/gradle/issues/5098
			"run", "--console=plain",
			"-PmainClass=io.pulumi.bootstrap.internal.Main",
			"--args=packages",
		},
	}, nil
}

func newMavenExecutor(cmd string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:       cmd,
		buildArgs: []string{"--no-transfer-progress", "compile"},
		runArgs:   []string{"--no-transfer-progress", "compile", "exec:java"},
		pluginArgs: []string{
			"--quiet", "--no-transfer-progress", "compile", "exec:java",
			"-DmainClass=io.pulumi.bootstrap.internal.Main",
			"-DmainArgs=packages",
		},
	}, nil
}

func newJarExecutor(cmd string, path string) (*jvmExecutor, error) {
	return &jvmExecutor{
		cmd:        cmd,
		buildArgs:  nil,
		runArgs:    []string{"-jar", filepath.Clean(path)},
		pluginArgs: []string{"-cp", filepath.Clean(path), "io.pulumi.bootstrap.internal.Main", "packages"},
	}, nil
}
