package main

import (
	"fmt"
	"os/exec"
	"strings"
)

func gradlePublish() (string, error) {
	cmd := exec.Command("gradle", "publish")
	out, err := cmd.CombinedOutput()
	return string(out), err
}

func readGradleProperties(dir string) (string, string, error) {
	cmd := exec.Command("gradle", "properties")
	if dir != "" {
		cmd.Dir = dir
	}
	out, err := cmd.Output()
	if err != nil {
		return "", "", err
	}
	lines := strings.Split(string(out), "\n")
	var name, version string
	for _, line := range lines {
		if strings.HasPrefix(line, "version:") {
			version = strings.TrimSpace(strings.TrimPrefix(line, "version:"))
		}
		if strings.HasPrefix(line, "name:") {
			name = strings.TrimSpace(strings.TrimPrefix(line, "name:"))
		}
	}
	if name != "" && version != "" {
		return name, version, nil
	}
	return "", "", fmt.Errorf("failed to read gradle name and version properties")
}
