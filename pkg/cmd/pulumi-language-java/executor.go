// Copyright 2022, Pulumi Corporation.  All rights reserved.

package main

import (
	"fmt"
)

// Abstracts interactions with a Java project, abiltiy to build, run
// Java code, and detect plugin dependencies.
type javaExecutor struct {
	cmd        string
	buildArgs  []string
	runArgs    []string
	pluginArgs []string
}

// Information available to pick an executor.
type javaExecutorOptions struct {
	// current working directory
	wd string

	// runtime.options.binary setting from Pulumi.yaml, if any
	binary string

	// runtime.options.use-executor setting from Pulumi.yaml, if any
	useExecutor string
}

type javaExecutorFactory interface {
	// Tries configuring an executor from the given options. May
	// return nil if options are not-applicable.
	tryConfigureExecutor(javaExecutorOptions) (*javaExecutor, error)
}

func configureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
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

func (c combinedJavaExecutorFactory) tryConfigureExecutor(opts javaExecutorOptions) (*javaExecutor, error) {
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
