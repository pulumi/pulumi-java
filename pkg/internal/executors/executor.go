// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"context"
	"errors"
	"fmt"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
	pulumirpc "github.com/pulumi/pulumi/sdk/v3/proto/go"
)

// Abstracts interactions with a Java project, ability to build, run
// Java code, and detect plugin dependencies.
type JavaExecutor struct {
	// Path to the command to run.
	Cmd string

	// Optional dir to run the command in.
	Dir string

	// Command to run the Java code - the main entrypoint.
	RunArgs []string

	// Command args to resolve dependencies and build; this will
	// be called after `pulumi new` on Gradle templates. Optional.
	BuildArgs []string

	// Command to autodetect and print Pulumi plugins depended on
	// by the Java program.
	AnalyzerArgs []string

	// Command to autodetect and print Pulumi plugins depended on
	// by the Java program.
	PluginArgs []string

	// Command args to run a plugin (e.g. a provider). Optional if the executor
	// does not support running plugins.
	RunPluginArgs []string

	// Returns a list of program dependencies as configured for the executor (e.g. in a `pom.xml` for Maven, or a
	// `build.gradle` for Gradle).
	GetProgramDependencies func(
		ctx context.Context,
		req *pulumirpc.GetProgramDependenciesRequest,
	) (*pulumirpc.GetProgramDependenciesResponse, error)
}

// Information available to pick an executor.
type JavaExecutorOptions struct {
	// Current working directory. Abstract to enable testing.
	WD fsys.ParentFS

	// The value of `runtime.options.binary` setting from
	// `Pulumi.yaml`. Optional.
	Binary string

	// The value of `runtime.options.use-executor` setting from
	// `Pulumi.yaml`. Optional.
	UseExecutor string

	// Additional runtime arguments to pass to the program.
	ProgramArgs []string
}

type javaExecutorFactory interface {
	// Tries configuring an executor from the given options. May
	// return nil if options are not-applicable.
	NewJavaExecutor(JavaExecutorOptions) (*JavaExecutor, error)
}

// Create a new java executor. Note that when a debugger is being attached the only
// executor that is supported is the maven executor.
func NewJavaExecutor(opts JavaExecutorOptions, attachDebugger bool) (*JavaExecutor, error) {
	if attachDebugger {
		opts.UseExecutor = "mvnDebug"
		e, err := combineJavaExecutorFactories(
			&maven{},
		).NewJavaExecutor(opts)
		if e == nil {
			return nil, errors.New(
				"failed to configure executor.  For debugging only the maven executor is supported, " +
					"and mvnDebug must be installed")
		}
		if err != nil {
			return nil, err
		}
		return e, nil
	}
	e, err := combineJavaExecutorFactories(
		&jarexec{},
		&maven{},
		&gradle{},
		&jbang{},
		&sbt{},
	).NewJavaExecutor(opts)
	if err != nil {
		return nil, err
	}
	if e == nil {
		return nil, fmt.Errorf("failed to configure executor, tried: jar, maven, gradle, jbang, sbt")
	}
	return e, err
}

type combinedJavaExecutorFactory []javaExecutorFactory

func (c combinedJavaExecutorFactory) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	for _, v := range c {
		e, err := v.NewJavaExecutor(opts)
		if err != nil {
			return nil, err
		}
		if e != nil {
			return e, nil
		}
	}
	return nil, nil
}

func combineJavaExecutorFactories(variations ...javaExecutorFactory) javaExecutorFactory {
	return combinedJavaExecutorFactory(variations)
}
