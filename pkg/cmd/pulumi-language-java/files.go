// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
	"os"
	"os/exec"
	"strings"
)

func fileExists(path string) (bool, error) {
	_, err := os.Stat(path)
	if err != nil && os.IsNotExist(err) {
		return false, nil
	}
	return true, err
}

func lookupPath(files ...string) (string, error) {
	var lastError error
	for _, file := range files {
		pathExec, err := exec.LookPath(file)
		if err == nil {
			return pathExec, nil
		}
		lastError = err
	}
	return "", fmt.Errorf("could not find %s on the $PATH: %w",
		strings.Join(files, ", "), lastError)
}
