// Copyright 2026, Pulumi Corporation.  All rights reserved.

// Package executors: Mill executor.
//
// Detects Mill 1.x projects (build.mill / build.mill.scala) and
// invokes the program through Mill's CLI. Mill's long-lived daemon
// (enabled by default in Mill 1.x) makes subsequent `pulumi preview`
// invocations significantly faster than sbt's `-batch` model.
//
// Environment-variable propagation
// --------------------------------
// pulumi-language-java sets per-invocation env vars (PULUMI_MONITOR,
// PULUMI_ENGINE, etc.) that the Pulumi Java SDK reads via
// System.getenv on startup. Mill's RunModule defines
// `propagateEnv: T[Boolean] = Task { true }` by default, which causes
// the forked `run` / `runMain` JVM to inherit the client's current
// environment. This means the executor works without any build.mill
// changes — provided the user has not overridden `propagateEnv` to
// `false` on the Pulumi module.
//
// Module selection
// ----------------
// Mill addresses tasks by module path (`mill app.compile`), and a
// programmable build (build.mill / build.mill.scala) can declare any
// number of top-level modules with no implicit default — unlike sbt's
// root project. The module that holds the Pulumi program is therefore
// taken from the required `mill-module` runtime option rather than
// guessed from the build file. The declarative build.mill.yaml form is
// a single root module and needs no option; its tasks run unprefixed.

package executors

import (
	"fmt"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

type mill struct{}

var _ javaExecutorFactory = mill{}

func (m mill) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	ok, err := m.isMillProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./mill", "mill"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	module, err := m.moduleName(opts)
	if err != nil {
		return nil, err
	}
	return m.newMillExecutor(cmd, module)
}

// taskPath returns the Mill task selector for the given task name in the
// detected module. When module is empty (declarative build.mill.yaml at the
// project root), tasks are invoked without a prefix.
func taskPath(module, task string) string {
	if module == "" {
		return task
	}
	return module + "." + task
}

func (mill) isMillProject(opts JavaExecutorOptions) (bool, error) {
	if strings.Contains(opts.UseExecutor, "mill") {
		return true, nil
	}
	for _, p := range millBuildFiles {
		exists, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return false, err
		}
		if exists {
			return true, nil
		}
	}
	return false, nil
}

// moduleName returns the Mill module that contains the Pulumi program, or the
// empty string when tasks should run without a module prefix.
//
// Programmable builds (build.mill / build.mill.scala) require the module to be
// named via the `mill-module` runtime option. Inferring it by scanning the
// build file is unreliable: a regex cannot tell a real `object X extends
// ScalaModule` declaration from one inside a comment or a string literal, nor
// know which of several modules is the Pulumi program.
//
// The declarative build.mill.yaml form describes a single root module with no
// name, so Mill invokes its tasks unprefixed and moduleName returns "".
func (mill) moduleName(opts JavaExecutorOptions) (string, error) {
	for _, p := range millBuildFiles {
		exists, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return "", err
		}
		if !exists {
			continue
		}
		// build.mill.yaml is the declarative single-root-module form; Mill
		// invokes its tasks without a module prefix.
		if p == "build.mill.yaml" {
			return "", nil
		}
		// Programmable build: the module name is required.
		if opts.MillModule == "" {
			return "", fmt.Errorf(
				"the Mill executor requires the \"mill-module\" runtime option to select the "+
					"module that contains the Pulumi program; set it in Pulumi.yaml, e.g.:\n"+
					"  runtime:\n    name: java\n    options:\n      mill-module: app\n"+
					"matching a top-level `object app extends ScalaModule` in %s", p)
		}
		return opts.MillModule, nil
	}
	// No recognized build file: the executor was forced via `use-executor`.
	// Honor an explicit module if one was given, otherwise run unprefixed.
	return opts.MillModule, nil
}

func (mill) newMillExecutor(cmd, module string) (*JavaExecutor, error) {
	return &JavaExecutor{
		Cmd:       cmd,
		BuildArgs: []string{taskPath(module, "compile")},
		RunArgs:   []string{taskPath(module, "run")},
		PluginArgs: []string{
			// --ticker false silences Mill's progress chatter so STDOUT
			// contains only the JSON output of the bootstrap main class.
			"--ticker", "false",
			taskPath(module, "runMain"),
			"com.pulumi.bootstrap.internal.Main",
			"packages",
		},
	}, nil
}

// millBuildFiles is the ordered list of file names that mark a Mill project.
// "build.mill" is the canonical Mill 1.x programmable form, "build.mill.scala"
// is the IDE-friendly variant of the same, and "build.mill.yaml" is Mill
// 1.1+'s declarative single-root-module form. Mill 0.x's "build.sc" is
// intentionally not supported — this executor has only been verified against
// Mill 1.x, whose RunModule is known to propagate the client's environment
// into forked program JVMs.
var millBuildFiles = []string{
	"build.mill",
	"build.mill.scala",
	"build.mill.yaml",
}
