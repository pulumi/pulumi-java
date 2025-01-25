// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
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
		Dir: filepath.Dir(cmd),
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

		// Implements the GetProgramDependencies function to retrieve the dependencies of a Maven project.
		GetProgramDependencies: func(
			_ context.Context,
			req *pulumirpc.GetProgramDependenciesRequest,
		) (*pulumirpc.GetProgramDependenciesResponse, error) {
			// Maven provides a dependency:tree goal that can be used to list all dependencies in a project. It supports
			// a variety of formats, but in the absence of JSON, the most machine-readable is the "Trivial Graph
			// Format", TGF. A TGF file lists a series of nodes followed by a series of edges. Each node definition is a
			// single line of text consisting of a unique node ID, followed by a space and the node's label. Edge
			// definitions are similarly lines of text, starting with the IDs of the nodes being linked separated by a
			// space. If an edge has a label, it appears after the two node IDs. Node definitions are separated from
			// edge definitions by a line containing only a `#` character. For example:
			//
			//   1 First node
			//   2 Second node
			//   #
			//   1 2 Edge from first to second
			//
			// In the output of dependency:tree, the nodes are our dependencies. We don't need to worry about the edges
			// so we can break as soon as we see a line starting with `#`.

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

				// At this point:
				//
				// * parts[3] will be the type of artifact, such as `jar`
				// * parts[4] will be the version of the artifact
				//
				// We don't care about the artifact type, and for now we don't report versions, since in their absence
				// the checks performed by e.g. conformance tests are more relaxed, and Java code generation isn't good
				// enough to pass stricter checks at present.

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
