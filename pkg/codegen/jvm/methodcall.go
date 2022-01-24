package jvm

import (
	"fmt"
	"strings"
)

type MethodCall struct {
	This   string
	Method string
	Args   []string
}

func (m MethodCall) String() string {
	return fmt.Sprintf("%s.%s(%s)", m.This, javaIdentifier(m.Method), strings.Join(m.Args, ","))
}
