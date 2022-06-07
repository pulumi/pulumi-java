// Copyright 2016-2022, Pulumi Corporation.  All rights reserved.

package tests

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFixupGradleVersion(t *testing.T) {
	given := `implementation "com.pulumi:pulumi:(,1.0]"`
	expect := `implementation("com.pulumi:pulumi") { version { strictly "0.0.1" }}`
	assert.Equal(t, expect,
		string(fixupGradleVersion("pulumi", "0.0.1", []byte(given))))
}
