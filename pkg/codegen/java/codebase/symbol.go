package codebase

import (
	"bytes"
	"fmt"
)

type Symbol interface {
	QualifiedName() string
	Package() string
	Name() string

	AsType() Type
	AsExpression() Expression
}

type symbolImpl struct {
	qualifiedName string
	pkg           string
	name          string
}

func NewSymbol(pkg string, name string) Symbol {
	return &symbolImpl{
		qualifiedName: fmt.Sprintf("%s.%s", pkg, name),
		pkg:           pkg,
		name:          name,
	}
}

func (s *symbolImpl) QualifiedName() string {
	return s.qualifiedName
}

func (s *symbolImpl) Package() string {
	return s.pkg
}

func (s *symbolImpl) Name() string {
	return s.name
}

func (s *symbolImpl) AsType() Type {
	return &typeImpl{
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString(s.name)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString(s.name)
		},
	}
}

func (s *symbolImpl) AsExpression() Expression {
	return &expressionImpl{
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString(s.name)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString(s.name)
		},
	}
}
