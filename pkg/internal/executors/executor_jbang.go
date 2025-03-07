// Copyright 2022, Pulumi Corporation.  All rights reserved.

package executors

import (
	"path/filepath"
	"strings"

	"github.com/pulumi/pulumi/sdk/v3/go/common/util/logging"

	"github.com/pulumi/pulumi-java/pkg/internal/fsys"
)

type jbang struct{}

var _ javaExecutorFactory = &jbang{}

func (j jbang) NewJavaExecutor(opts JavaExecutorOptions) (*JavaExecutor, error) {
	ok, err := j.isJBangProject(opts)
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	script := opts.Binary
	if script == "" {
		script = "src/main.java"
	}
	probePaths := []string{opts.UseExecutor}
	if opts.UseExecutor == "" {
		probePaths = []string{"./jbang", "jbang"}
	}
	cmd, err := fsys.LookPath(opts.WD, probePaths...)
	if err != nil {
		return nil, err
	}
	return j.newJBangExecutor(cmd, script)
}

func (j jbang) isJBangProject(opts JavaExecutorOptions) (bool, error) {
	if opts.Binary != "" {
		suffix := strings.ToLower(filepath.Ext(opts.Binary))
		switch suffix {
		case ".java", ".kt", ".groovy":
			return true, nil
		}
	}
	if strings.Contains(opts.UseExecutor, "jbang") {
		return true, nil
	}
	return fsys.FileExists(opts.WD, "jbang.properties")
}

func (j jbang) newJBangExecutor(cmd string, script string) (*JavaExecutor, error) {
	logging.V(3).Infof("Making JBang executor for: `%s`", cmd)
	return &JavaExecutor{
		Cmd:       cmd,
		BuildArgs: []string{"--quiet", "build", script},
		RunArgs:   []string{"--quiet", "run", script},
		AnalyzerArgs: []string{
			"--quiet", "run",
			"--main=com.pulumi.bootstrap.internal.Main",
			script,
			"analyzer",
		},
		PluginArgs: []string{
			"--quiet", "run",
			"--main=com.pulumi.bootstrap.internal.Main",
			script,
			"packages",
		},
	}, nil
}
