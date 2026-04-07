// Copyright 2016-2021, Pulumi Corporation.  All rights reserved.

package examples

import (
	"github.com/pulumi/pulumi/pkg/v3/testing/integration"
)

func getBaseOptions() integration.ProgramTestOptions {
	return integration.ProgramTestOptions{
		Dependencies: []string{"@pulumi/pulumi"},
	}
}
