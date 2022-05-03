// Copyright 2022, Pulumi Corporation.  All rights reserved.

package jvm

import (
	"fmt"
	"strings"

	"github.com/pulumi/pulumi-java/pkg/codegen/jvm/names"
)

type MethodCall struct {
	This   string
	Method string
	Args   []string
}

func (m MethodCall) String() string {
	return fmt.Sprintf("%s.%s(%s)", m.This, names.Ident(m.Method), strings.Join(m.Args, ","))
}
