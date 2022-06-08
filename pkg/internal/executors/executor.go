// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"fmt"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

// Abstracts interactions with a Java project, abiltiy to build, run
// Java code, and detect plugin dependencies.
type JavaExecutor struct {
	Cmd        string
	BuildArgs  []string
	RunArgs    []string
	PluginArgs []string
}

// Information available to pick an executor.
type JavaExecutorOptions struct {
	// current working directory
	WD fsys.ParentFS

	// runtime.options.binary setting from Pulumi.yaml, if any
	Binary string

	// runtime.options.use-executor setting from Pulumi.yaml, if any
	UseExecutor string
}

type javaExecutorFactory interface {
	// Tries configuring an executor from the given options. May
	// return nil if options are not-applicable.
	tryConfigureExecutor(JavaExecutorOptions) (*JavaExecutor, error)
}

func ConfigureExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	e, err := combineJavaExecutorFactories(
		&jarexec{},
		&maven{},
		&gradle{},
		&jbang{},
	).tryConfigureExecutor(opts)
	if err != nil {
		return nil, err
	}
	if e == nil {
		return nil, fmt.Errorf("Failed to configure executor, tried: jar, maven, gradle, jbang")
	}
	return e, nil
}

type combinedJavaExecutorFactory []javaExecutorFactory

func (c combinedJavaExecutorFactory) tryConfigureExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	for _, v := range c {
		e, err := v.tryConfigureExecutor(opts)
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
