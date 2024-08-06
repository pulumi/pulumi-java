// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
)

type maven struct{}

var _ javaExecutorFactory = &maven{}

func (m maven) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
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
		Cmd: cmd,
		BuildArgs: []string{
			/* only output warning or higher to reduce noise */
			"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
			"--no-transfer-progress",
			"compile",
		},
		RunArgs: []string{
			/* only output warning or higher to reduce noise */
			"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
			"--no-transfer-progress",
			"compile",
			"exec:java",
		},
		PluginArgs: []string{
			/* move normal output to STDERR, because we need STDOUT for JSON with plugin results */
			"-Dorg.slf4j.simpleLogger.logFile=System.err",
			"--no-transfer-progress", "compile", "exec:java",
			"-DmainClass=com.pulumi.bootstrap.internal.Main",
			"-DmainArgs=packages",
		},
		GetProgramDependencies: func(
			_ context.Context,
			req *pulumirpc.GetProgramDependenciesRequest,
		) (*pulumirpc.GetProgramDependenciesResponse, error) {
			tgfFile, err := os.CreateTemp("", "maven-dependencies.tgf")
			if err != nil {
				return nil, err
			}

			//nolint:gosec
			command := exec.Command(
				cmd,
				"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
				"--no-transfer-progress",
				"dependency:tree",
				"-DoutputType=tgf",
				"-DoutputFile="+tgfFile.Name(),
			)
			command.Dir = req.Info.ProgramDirectory
			err = command.Run()
			if err != nil {
				return nil, err
			}

			dependencies := []*pulumirpc.DependencyInfo{}

			scanner := bufio.NewScanner(tgfFile)
			for scanner.Scan() {
				line := scanner.Text()
				if line[0] == '#' {
					break
				}

				parts := tgfDependencyPattern.FindStringSubmatch(line)
				if len(parts) != 5 {
					return nil, fmt.Errorf("unexpected dependency encountered: %s", line)
				}

				groupID := parts[1]
				artifactID := parts[2]
				// parts[3] will be the type of artifact, such as `jar`
				// version := parts[4]

				dependencies = append(dependencies, &pulumirpc.DependencyInfo{
					Name:    fmt.Sprintf("%s:%s", groupID, artifactID),
					Version: "",
				})
			}

			err = tgfFile.Close()
			if err != nil {
				return nil, err
			}

			err = os.Remove(tgfFile.Name())
			if err != nil {
				return nil, err
			}

			return &pulumirpc.GetProgramDependenciesResponse{
				Dependencies: dependencies,
			}, nil
		},
	}, nil
}

var tgfDependencyPattern = regexp.MustCompile("[^ ]+ ([^:]+):([^:]+):([^:]+):([^:]+)")
