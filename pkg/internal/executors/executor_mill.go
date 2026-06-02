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
// Module discovery
// ----------------
// Mill projects can have multiple top-level modules. This executor
// picks the first `object X extends *Module` declaration in the build
// file. Multi-module projects where the Pulumi module is not the
// first declaration are not yet supported; a dedicated runtime option
// would be the right fix.

package executors

import (
	"fmt"
	"io/fs"
	"regexp"
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
	module, err := m.findModuleName(opts)
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

// findModuleName returns the name of the first top-level module declared
// in the build file. Mill convention is:
//
//	object myModule extends ScalaModule { ... }
//
// The match accepts any extended type whose name contains the substring
// "Module" — this covers the built-ins (ScalaModule, JavaModule,
// CrossScalaModule, KotlinModule, etc.) as well as user-defined traits
// that extend them and conventionally embed "Module" in their name
// (e.g. ScalaModuleEx, MyScalaModule). Qualified references like
// mill.scalalib.ScalaModule are also accepted.
//
// For declarative build.mill.yaml projects, the file itself describes a
// single root module with no explicit name, and Mill tasks are invoked
// without a module prefix (e.g. `mill compile` rather than `mill foo.compile`).
// findModuleName returns the empty string in that case, which taskPath
// translates into bare task selectors.
func (mill) findModuleName(opts JavaExecutorOptions) (string, error) {
	re := regexp.MustCompile(`(?m)^\s*object\s+([\w$]+)\s+extends\s+[\w.$]*Module[\w$]*\b`)
	for _, p := range millBuildFiles {
		exists, err := fsys.FileExists(opts.WD, p)
		if err != nil {
			return "", err
		}
		if !exists {
			continue
		}
		// build.mill.yaml is the declarative single-root-module form;
		// Mill invokes its tasks without a module prefix.
		if p == "build.mill.yaml" {
			return "", nil
		}
		content, err := fs.ReadFile(opts.WD, p)
		if err != nil {
			continue
		}
		if m := re.FindStringSubmatch(string(content)); m != nil {
			return m[1], nil
		}
	}
	return "", fmt.Errorf(
		"could not infer a Mill module name from %s — expected a top-level "+
			"`object X extends ScalaModule` (or similar *Module trait) declaration; "+
			"note this executor targets Mill 1.x and does not read Mill 0.x's build.sc",
		strings.Join(millBuildFiles, " / "))
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
