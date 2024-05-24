package codebase

import (
	"bytes"
	"fmt"
	"math/big"
	"slices"
	"strings"

	"golang.org/x/exp/maps"
)

const Indentation = "    "

const TargetWidth = 80

type Codebase interface {
	Package(name string) Package
	Instantiate() map[string][]byte
}

type codebaseImpl struct {
	packages map[string]*packageImpl
}

func NewCodebase() Codebase {
	return &codebaseImpl{
		packages: make(map[string]*packageImpl),
	}
}

func (c *codebaseImpl) Package(name string) Package {
	if pkg, ok := c.packages[name]; ok {
		return pkg
	}

	pkg := newPackage(name)
	c.packages[name] = pkg
	return pkg
}

func (c *codebaseImpl) Instantiate() map[string][]byte {
	m := make(map[string][]byte)
	for _, pkg := range c.packages {
		for path, content := range pkg.Instantiate() {
			m[path] = content
		}
	}

	return m
}

type Named interface {
	Name() string
}

type Sourced interface {
	WriteBlockSource(b *bytes.Buffer, indent string)
}

type Package interface {
	Named

	Path() string

	CompilationUnit(name string) CompilationUnit

	Instantiate() map[string][]byte
}

type packageImpl struct {
	name             string
	path             string
	compilationUnits map[string]*compilationUnitImpl
}

func newPackage(name string) *packageImpl {
	// dots to slashes
	path := strings.ReplaceAll(name, ".", "/")

	return &packageImpl{
		name:             name,
		path:             path,
		compilationUnits: make(map[string]*compilationUnitImpl),
	}
}

func (p *packageImpl) Name() string {
	return p.name
}

func (p *packageImpl) Path() string {
	return p.path
}

func (p *packageImpl) CompilationUnit(name string) CompilationUnit {
	if cu, ok := p.compilationUnits[name]; ok {
		return cu
	}

	cu := newCompilationUnit(p.path, p.name, name)
	p.compilationUnits[name] = cu
	return cu
}

func (p *packageImpl) Instantiate() map[string][]byte {
	m := make(map[string][]byte)
	for _, cu := range p.compilationUnits {
		for path, content := range cu.Instantiate() {
			m[path] = content
		}
	}

	return m
}

type CompilationUnit interface {
	Named
	Sourced

	PackageName() string
	Import(s Symbol) Symbol
	ImportStatic(s Symbol, m string) Symbol

	PublicClass() Class
	Class(visiblity Visibility, modifiers []Modifier, name string) Class

	Instantiate() map[string][]byte
}

type compilationUnitImpl struct {
	packagePath    string
	packageName    string
	imports        map[string]Symbol
	staticImports  map[string]Symbol
	name           string
	publicClass    *classImpl
	siblingClasses map[string]*classImpl
}

func newCompilationUnit(packagePath string, packageName string, name string) *compilationUnitImpl {
	publicClass := newClass(packageName, Public, name)

	return &compilationUnitImpl{
		packagePath:    packagePath,
		packageName:    packageName,
		imports:        make(map[string]Symbol),
		staticImports:  make(map[string]Symbol),
		name:           name,
		publicClass:    publicClass,
		siblingClasses: make(map[string]*classImpl),
	}
}

func (c *compilationUnitImpl) PackageName() string {
	return c.packageName
}

func (c *compilationUnitImpl) Name() string {
	return c.name
}

func (c *compilationUnitImpl) Import(s Symbol) Symbol {
	if isImplicitlyImportedPackage(s.Package()) {
		return s
	}

	if s, ok := c.imports[s.QualifiedName()]; ok {
		return s
	}

	c.imports[s.QualifiedName()] = s
	return s
}

func (c *compilationUnitImpl) ImportStatic(s Symbol, m string) Symbol {
	sym := NewSymbol(s.QualifiedName(), m)
	if isImplicitlyImportedPackage(s.Package()) {
		return sym
	}

	if s, ok := c.staticImports[sym.QualifiedName()]; ok {
		return s
	}

	c.staticImports[sym.QualifiedName()] = sym
	return sym
}

func (c *compilationUnitImpl) PublicClass() Class {
	return c.publicClass
}

func (c *compilationUnitImpl) Class(visibility Visibility, modifiers []Modifier, name string) Class {
	if sibling, ok := c.siblingClasses[name]; ok {
		return sibling
	}

	sibling := newClass(c.packageName, visibility, name)
	c.siblingClasses[name] = sibling
	return sibling
}

func (c *compilationUnitImpl) WriteBlockSource(b *bytes.Buffer, indent string) {
	b.WriteString("package ")
	b.WriteString(c.packageName)
	b.WriteString(";\n\n")

	staticImports := maps.Values(c.staticImports)
	slices.SortFunc(staticImports, func(a, b Symbol) int {
		if a.QualifiedName() < b.QualifiedName() {
			return -1
		} else if a.QualifiedName() > b.QualifiedName() {
			return 1
		} else {
			return 0
		}
	})

	for i, s := range staticImports {
		b.WriteString("import static ")
		b.WriteString(s.QualifiedName())
		b.WriteString(";\n")

		if i == len(staticImports)-1 {
			b.WriteString("\n")
		}
	}

	imports := maps.Values(c.imports)
	slices.SortFunc(imports, func(a, b Symbol) int {
		if a.QualifiedName() < b.QualifiedName() {
			return -1
		} else if a.QualifiedName() > b.QualifiedName() {
			return 1
		} else {
			return 0
		}
	})

	for i, s := range imports {
		b.WriteString("import ")
		b.WriteString(s.QualifiedName())
		b.WriteString(";\n")

		if i == len(imports)-1 {
			b.WriteString("\n")
		}
	}

	c.publicClass.WriteBlockSource(b, indent)

	siblings := maps.Values(c.siblingClasses)
	slices.SortFunc(siblings, func(a, b *classImpl) int {
		if a.Name() < b.Name() {
			return -1
		} else if a.Name() > b.Name() {
			return 1
		} else {
			return 0
		}
	})

	for _, s := range siblings {
		b.WriteString("\n")
		s.WriteBlockSource(b, indent)
	}
}

func (c *compilationUnitImpl) Instantiate() map[string][]byte {
	path := fmt.Sprintf("%s/%s.java", c.packagePath, c.name)
	b := new(bytes.Buffer)
	c.WriteBlockSource(b, "")

	m := make(map[string][]byte)
	m[path] = b.Bytes()

	return m
}

type Visibility int

const (
	PackagePrivate Visibility = iota
	Private
	Protected
	Public
)

func (v Visibility) WriteSource(b *bytes.Buffer) {
	switch v {
	case PackagePrivate:
		return
	case Private:
		b.WriteString("private")
	case Protected:
		b.WriteString("protected")
	case Public:
		b.WriteString("public")
	default:
		return
	}
}

type Modifier int

const (
	Final Modifier = iota
	Static
	Abstract
)

func (m Modifier) WriteSource(b *bytes.Buffer) {
	switch m {
	case Final:
		b.WriteString("final")
	case Static:
		b.WriteString("static")
	case Abstract:
		b.WriteString("abstract")
	default:
		return
	}
}

type Class interface {
	Named
	Sourced

	Symbol() Symbol
	Visibility() Visibility
	Method(
		visibility Visibility,
		modifiers []Modifier,
		returnType Type,
		name string,
		args []Argument,
		body []Statement,
	) ClassMethod
}

type classImpl struct {
	packageName string
	visibility  Visibility
	name        string
	methods     map[string]*classMethodImpl
}

func newClass(packageName string, visibility Visibility, name string) *classImpl {
	return &classImpl{
		packageName: packageName,
		visibility:  visibility,
		name:        name,
		methods:     make(map[string]*classMethodImpl),
	}
}

func (c *classImpl) Symbol() Symbol {
	return NewSymbol(c.packageName, c.name)
}

func (c *classImpl) Visibility() Visibility {
	return c.visibility
}

func (c *classImpl) Name() string {
	return c.name
}

func (c *classImpl) Method(
	visibility Visibility,
	modifiers []Modifier,
	returnType Type,
	name string,
	args []Argument,
	body []Statement,
) ClassMethod {
	if cm, ok := c.methods[name]; ok {
		return cm
	}

	cm := newClassMethod(
		visibility,
		modifiers,
		returnType,
		name,
		args,
		body,
	)

	c.methods[name] = cm
	return cm
}

func (c *classImpl) WriteBlockSource(b *bytes.Buffer, indent string) {
	methods := maps.Values(c.methods)
	slices.SortFunc(methods, func(a, b *classMethodImpl) int {
		if a.Name() < b.Name() {
			return -1
		} else if a.Name() > b.Name() {
			return 1
		} else {
			return 0
		}
	})

	if c.visibility != PackagePrivate {
		c.visibility.WriteSource(b)
		b.WriteString(" ")
	}

	b.WriteString("class ")
	b.WriteString(c.name)
	b.WriteString(" {\n")

	for i, m := range methods {
		b.WriteString(indent)
		b.WriteString(Indentation)

		m.WriteBlockSource(b, indent+Indentation)
		if i < len(methods)-1 {
			b.WriteString("\n\n")
		}
	}

	b.WriteString("\n")
	b.WriteString(indent)
	b.WriteString("}\n")
}

type ClassMethod interface {
	Named
	Sourced

	Visibility() Visibility
	Modifiers() []Modifier
}

type classMethodImpl struct {
	visibility Visibility
	modifiers  []Modifier
	returnType Type
	name       string
	args       []Argument
	body       []Statement
}

func newClassMethod(
	visibility Visibility,
	modifiers []Modifier,
	returnType Type,
	name string,
	args []Argument,
	body []Statement,
) *classMethodImpl {
	return &classMethodImpl{
		visibility: visibility,
		modifiers:  modifiers,
		returnType: returnType,
		name:       name,
		args:       args,
		body:       body,
	}
}

func (cm *classMethodImpl) Visibility() Visibility {
	return cm.visibility
}

func (cm *classMethodImpl) Modifiers() []Modifier {
	return cm.modifiers
}

func (cm *classMethodImpl) Name() string {
	return cm.name
}

func (cm *classMethodImpl) WriteBlockSource(b *bytes.Buffer, indent string) {
	if cm.visibility != PackagePrivate {
		cm.visibility.WriteSource(b)
		b.WriteString(" ")
	}

	for _, m := range cm.modifiers {
		m.WriteSource(b)
		b.WriteString(" ")
	}

	cm.returnType.WriteBlockSource(b, indent)
	b.WriteString(" ")
	b.WriteString(cm.name)

	b.WriteString("(")
	if len(cm.args) > 0 {
		tooLong := func() bool {
			b := new(bytes.Buffer)
			for _, arg := range cm.args {
				arg.WriteBlockSource(b, "")
			}

			return b.Len() > TargetWidth
		}

		if tooLong() {
			b.WriteString("\n")
			b.WriteString(indent)
			b.WriteString(Indentation)

			for i, arg := range cm.args {
				if i > 0 {
					b.WriteString(",\n")
					b.WriteString(indent)
					b.WriteString(Indentation)
				}

				arg.WriteBlockSource(b, indent+Indentation)
			}

			b.WriteString("\n")
			b.WriteString(indent)
		} else {
			for i, arg := range cm.args {
				if i > 0 {
					b.WriteString(", ")
				}

				arg.WriteBlockSource(b, indent)
			}
		}
	}
	b.WriteString(") {")

	for _, s := range cm.body {
		b.WriteRune('\n')
		b.WriteString(indent)
		b.WriteString(Indentation)
		s.WriteSource(b, indent+Indentation)
	}

	b.WriteRune('\n')
	b.WriteString(indent)
	b.WriteString("}")
}

type Argument struct {
	Sourced

	Name string
	Type Type
}

func (a *Argument) WriteBlockSource(b *bytes.Buffer, indent string) {
	a.Type.WriteBlockSource(b, indent)
	b.WriteString(" ")
	b.WriteString(a.Name)
}

type Type interface {
	Sourced

	WriteInlineSource(b *bytes.Buffer)

	Apply(ts ...Type) Type
}

type typeImpl struct {
	writeBlockSourceImpl  func(b *bytes.Buffer, indent string)
	writeInlineSourceImpl func(b *bytes.Buffer)
}

func (t *typeImpl) WriteBlockSource(b *bytes.Buffer, indent string) {
	t.writeBlockSourceImpl(b, indent)
}

func (t *typeImpl) WriteInlineSource(b *bytes.Buffer) {
	t.writeInlineSourceImpl(b)
}

func (t *typeImpl) Apply(ts ...Type) Type {
	return &typeImpl{
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			t.WriteBlockSource(b, indent)
			b.WriteString("<\n")

			for i, t := range ts {
				if i > 0 {
					b.WriteString(",\n")
				}

				b.WriteString(indent)
				b.WriteString(Indentation)
				t.WriteBlockSource(b, indent+Indentation)
				b.WriteString("\n")
			}

			b.WriteString(indent)
			b.WriteString(">")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			t.WriteInlineSource(b)
			b.WriteString("<")

			for i, t := range ts {
				if i > 0 {
					b.WriteString(", ")
				}

				t.WriteInlineSource(b)
			}

			b.WriteString(">")
		},
	}
}

var VoidT Type = &typeImpl{
	writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
		b.WriteString("void")
	},
	writeInlineSourceImpl: func(b *bytes.Buffer) {
		b.WriteString("void")
	},
}

var StringT Type = &typeImpl{
	writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
		b.WriteString("String")
	},
	writeInlineSourceImpl: func(b *bytes.Buffer) {
		b.WriteString("String")
	},
}

func ArrayT(element Type) Type {
	return &typeImpl{
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			element.WriteBlockSource(b, indent)
			b.WriteString("[]")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			element.WriteInlineSource(b)
			b.WriteString("[]")
		},
	}
}

type Statement interface {
	WriteSource(b *bytes.Buffer, indent string)
}

type statementImpl struct {
	writeSourceImpl func(b *bytes.Buffer, indent string)
}

func (s *statementImpl) WriteSource(b *bytes.Buffer, indent string) {
	s.writeSourceImpl(b, indent)
}

func ExprS(e Expression) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			e.WriteSource(b, indent)
			b.WriteString(";")
		},
	}
}

func CommentS(comment string) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("//")
			b.WriteString(comment)
		},
	}
}

func VarS(
	modifiers []Modifier,
	name string,
	e Expression,
) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			for _, m := range modifiers {
				m.WriteSource(b)
				b.WriteString(" ")
			}

			b.WriteString("var ")
			b.WriteString(name)
			b.WriteString(" = ")
			e.WriteSource(b, indent)
			b.WriteString(";")
		},
	}
}

func ForS(
	initializer []Statement,
	condition Expression,
	update []Statement,
	body []Statement,
) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("for (")

			if len(initializer) > 0 {
				for i, s := range initializer {
					if i > 0 {
						b.WriteString(", ")
					}

					s.WriteSource(b, indent)
				}
			}

			b.WriteString("; ")
			condition.WriteInlineSource(b)
			b.WriteString(";")

			if len(update) > 0 {
				b.WriteString(" ")
				for i, s := range update {
					if i > 0 {
						b.WriteString(", ")
					}

					s.WriteSource(b, indent)
				}
			}

			b.WriteString(") {\n")

			for _, s := range body {
				b.WriteString(indent)
				b.WriteString(Indentation)
				s.WriteSource(b, indent+Indentation)
				b.WriteString("\n")
			}

			b.WriteString(indent)
			b.WriteString("}")
		},
	}
}

func ForEachVarS(
	modifiers []Modifier,
	name string,
	e Expression,
	body []Statement,
) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			for _, m := range modifiers {
				m.WriteSource(b)
				b.WriteString(" ")
			}

			b.WriteString("for (var ")
			b.WriteString(name)
			b.WriteString(" : ")
			e.WriteInlineSource(b)
			b.WriteString(") {\n")

			for _, s := range body {
				b.WriteString(indent)
				b.WriteString(Indentation)
				s.WriteSource(b, indent+Indentation)
				b.WriteString("\n")
			}

			b.WriteString(indent)
			b.WriteString("}")
		},
	}
}

func ReturnS(e Expression) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("return ")
			e.WriteSource(b, indent)
			b.WriteString(";")
		},
	}
}

type StatementGroupOptions struct {
	Separator func(indent string) string
}

func GroupS(opts *StatementGroupOptions, statements ...Statement) Statement {
	return &statementImpl{
		writeSourceImpl: func(b *bytes.Buffer, indent string) {
			if len(statements) > 0 {
				for i, s := range statements {
					s.WriteSource(b, indent)

					if opts != nil && i < len(statements)-1 {
						b.WriteString(opts.Separator(indent))
					}
				}
			}
		},
	}
}

var Compact = &StatementGroupOptions{
	Separator: func(indent string) string {
		return "\n" + indent
	},
}

var Spaced = &StatementGroupOptions{
	Separator: func(indent string) string {
		return "\n\n" + indent
	},
}

type Expression interface {
	Sourced

	WriteInlineSource(b *bytes.Buffer)
	WriteSource(b *bytes.Buffer, indent string)

	Call(args ...Expression) Expression
	Index(index Expression) Expression
	Method(method string, args ...Expression) Expression
	MethodReference(name string) Expression
	Property(property string) Expression

	Precedence() int
}

// https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
const (
	ParenPrecedence                int = 16
	ApplicationOrLiteralPrecedence int = 15
	PostfixPrecedence              int = 14
	UnaryPrecedence                int = 13
	MultiplicativePrecedence       int = 12
	AdditivePrecedence             int = 11
	ShiftPrecedence                int = 10
	RelationalPrecedence           int = 9
	EqualityPrecedence             int = 8
	BitwiseAndPrecedence           int = 7
	BitwiseXorPrecedence           int = 6
	BitwiseOrPrecedence            int = 5
	LogicalAndPrecedence           int = 4
	LogicalOrPrecedence            int = 3
	TernaryPrecedence              int = 2
	AssignmentPrecedence           int = 1
	NoPrecedence                   int = 0
)

type expressionImpl struct {
	precedence            int
	writeBlockSourceImpl  func(b *bytes.Buffer, indent string)
	writeInlineSourceImpl func(b *bytes.Buffer)
}

func (e *expressionImpl) Precedence() int {
	return e.precedence
}

func (e *expressionImpl) WriteBlockSource(b *bytes.Buffer, indent string) {
	e.writeBlockSourceImpl(b, indent)
}

func (e *expressionImpl) WriteInlineSource(b *bytes.Buffer) {
	e.writeInlineSourceImpl(b)
}

func (e *expressionImpl) WriteSource(b *bytes.Buffer, indent string) {
	var l bytes.Buffer
	e.WriteInlineSource(&l)
	if l.Len() > TargetWidth {
		e.WriteBlockSource(b, indent)
	} else {
		e.WriteInlineSource(b)
	}
}

func (e *expressionImpl) Call(args ...Expression) Expression {
	return appE(e, args...)
}

func (e *expressionImpl) Index(index Expression) Expression {
	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			e.WriteBlockSource(b, indent)
			b.WriteString("[")
			index.WriteSource(b, indent)
			b.WriteString("]")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			e.WriteInlineSource(b)
			b.WriteString("[")
			index.WriteInlineSource(b)
			b.WriteString("]")
		},
	}
}

func (e *expressionImpl) Method(
	name string,
	args ...Expression,
) Expression {
	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			e.WriteBlockSource(b, indent)
			b.WriteString("\n")
			b.WriteString(indent)
			b.WriteString(Indentation)
			b.WriteString(".")
			b.WriteString(name)

			appE(nil, args...).WriteSource(b, indent+Indentation)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			e.WriteInlineSource(b)
			b.WriteString(".")
			b.WriteString(name)

			appE(nil, args...).WriteInlineSource(b)
		},
	}
}

func (e *expressionImpl) MethodReference(name string) Expression {
	return &expressionImpl{
		precedence: NoPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			e.WriteBlockSource(b, indent)
			b.WriteString("::")
			b.WriteString(name)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			e.WriteInlineSource(b)
			b.WriteString("::")
			b.WriteString(name)
		},
	}
}

func (e *expressionImpl) Property(
	property string,
) Expression {
	eDotP := &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			e.WriteBlockSource(b, indent)
			b.WriteString(".")
			b.WriteString(property)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			e.WriteInlineSource(b)
			b.WriteString(".")
			b.WriteString(property)
		},
	}

	return eDotP
}

func literalE(literal string) Expression {
	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString(literal)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString(literal)
		},
	}
}

var NullE = literalE("null")

var TrueE = literalE("true")

var FalseE = literalE("false")

func StringE(s string) Expression {
	return literalE(fmt.Sprintf(`"%s"`, s))
}

func IntE(x int64) Expression {
	return literalE(fmt.Sprintf("%d", x))
}

func DoubleE(x float64) Expression {
	return literalE(fmt.Sprintf("%f", x))
}

func NumberE(x *big.Float) Expression {
	return literalE(x.String())
}

func QuoteE(e Expression) Expression {
	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("\"")
			e.WriteSource(b, indent)
			b.WriteString("\"")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString("\"")
			e.WriteInlineSource(b)
			b.WriteString("\"")
		},
	}
}

func NewE(
	t Type,
	args ...Expression,
) Expression {
	newCls := &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("new ")
			t.WriteInlineSource(b)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString("new ")
			t.WriteInlineSource(b)
		},
	}

	return appE(newCls, args...)
}

type LambdaArgument struct {
	Sourced

	Name string
	Type Type
}

func (la *LambdaArgument) WriteBlockSource(b *bytes.Buffer, indent string) {
	if la.Type != nil {
		la.Type.WriteBlockSource(b, indent)
		b.WriteString(" ")
	}

	b.WriteString(la.Name)
}

func LambdaEE(
	args []LambdaArgument,
	body Expression,
) Expression {
	return &expressionImpl{
		precedence: NoPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			n := len(args)
			if n == 0 {
				b.WriteString("()")
			} else if n == 1 {
				args[0].WriteBlockSource(b, indent)
			} else {
				b.WriteString("(")
				for i, arg := range args {
					arg.WriteBlockSource(b, indent)
					if i < n-1 {
						b.WriteString(", ")
					}
				}
				b.WriteString(")")
			}

			b.WriteString(" -> ")
			body.WriteBlockSource(b, indent)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			n := len(args)
			if n == 0 {
				b.WriteString("()")
			} else if n == 1 {
				args[0].WriteBlockSource(b, "")
			} else {
				b.WriteString("(")
				for i, arg := range args {
					arg.WriteBlockSource(b, "")
					if i < n-1 {
						b.WriteString(", ")
					}
				}
				b.WriteString(")")
			}

			b.WriteString(" -> ")
			body.WriteInlineSource(b)
		},
	}
}

func LambdaSE(
	args []LambdaArgument,
	body []Statement,
) Expression {
	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			n := len(args)
			if n == 0 {
				b.WriteString("()")
			} else if n == 1 {
				args[0].WriteBlockSource(b, indent)
			} else {
				b.WriteString("(")
				for i, arg := range args {
					arg.WriteBlockSource(b, indent)
					if i < n-1 {
						b.WriteString(", ")
					}
				}
				b.WriteString(")")
			}

			b.WriteString(" -> {\n")

			for _, s := range body {
				b.WriteString(indent)
				b.WriteString(Indentation)
				s.WriteSource(b, indent+Indentation)
				b.WriteString("\n")
			}

			b.WriteString("\n")
			b.WriteString(indent)
			b.WriteString("}")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			n := len(args)
			if n == 0 {
				b.WriteString("()")
			} else if n == 1 {
				args[0].WriteBlockSource(b, "")
			} else {
				b.WriteString("(")
				for i, arg := range args {
					arg.WriteBlockSource(b, "")
					if i < n-1 {
						b.WriteString(", ")
					}
				}
				b.WriteString(")")
			}

			b.WriteString(" -> { ")
			for i, s := range body {
				s.WriteSource(b, "")

				if i < len(body)-1 {
					b.WriteString("; ")
				}
			}
			b.WriteString(" }")
		},
	}
}

func ParenE(e Expression) Expression {
	return &expressionImpl{
		precedence: ParenPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString("(")
			e.WriteBlockSource(b, indent)
			b.WriteString(")")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString("(")
			e.WriteInlineSource(b)
			b.WriteString(")")
		},
	}
}

func PrecedenceE(
	precedence int,
	e Expression,
) Expression {
	return &expressionImpl{
		precedence: precedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			if e.Precedence() < precedence {
				ParenE(e).WriteBlockSource(b, indent)
			} else {
				e.WriteBlockSource(b, indent)
			}
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			if e.Precedence() < precedence {
				ParenE(e).WriteInlineSource(b)
			} else {
				e.WriteInlineSource(b)
			}
		},
	}
}

func prefixUnOpE(
	precedence int,
	op string,
	e Expression,
) Expression {
	return &expressionImpl{
		precedence: precedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			b.WriteString(op)
			PrecedenceE(precedence, e).WriteBlockSource(b, indent)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			b.WriteString(op)
			PrecedenceE(precedence, e).WriteInlineSource(b)
		},
	}
}

func postfixUnOpE(
	precedence int,
	op string,
	e Expression,
) Expression {
	return &expressionImpl{
		precedence: precedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			PrecedenceE(precedence, e).WriteBlockSource(b, indent)
			b.WriteString(op)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			PrecedenceE(precedence, e).WriteInlineSource(b)
			b.WriteString(op)
		},
	}
}

func binOpE(
	precedence int,
	op string,
	lhs Expression,
	rhs Expression,
) Expression {
	return &expressionImpl{
		precedence: precedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			PrecedenceE(precedence, lhs).WriteBlockSource(b, indent)
			b.WriteString(" ")
			b.WriteString(op)
			b.WriteString(" ")
			PrecedenceE(precedence, rhs).WriteBlockSource(b, indent)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			PrecedenceE(precedence, lhs).WriteInlineSource(b)
			b.WriteString(" ")
			b.WriteString(op)
			b.WriteString(" ")
			PrecedenceE(precedence, rhs).WriteInlineSource(b)
		},
	}
}

func AndE(lhs Expression, rhs Expression) Expression {
	return binOpE(LogicalAndPrecedence, "&&", lhs, rhs)
}

func OrE(lhs Expression, rhs Expression) Expression {
	return binOpE(LogicalOrPrecedence, "||", lhs, rhs)
}

func NotE(e Expression) Expression {
	return prefixUnOpE(UnaryPrecedence, "!", e)
}

func AddE(lhs Expression, rhs Expression) Expression {
	return binOpE(AdditivePrecedence, "+", lhs, rhs)
}

func SubtractE(lhs Expression, rhs Expression) Expression {
	return binOpE(AdditivePrecedence, "-", lhs, rhs)
}

func MultiplyE(lhs Expression, rhs Expression) Expression {
	return binOpE(MultiplicativePrecedence, "*", lhs, rhs)
}

func DivideE(lhs Expression, rhs Expression) Expression {
	return binOpE(MultiplicativePrecedence, "/", lhs, rhs)
}

func ModuloE(lhs Expression, rhs Expression) Expression {
	return binOpE(MultiplicativePrecedence, "%", lhs, rhs)
}

func NegateE(e Expression) Expression {
	return prefixUnOpE(UnaryPrecedence, "-", e)
}

func EqualE(lhs Expression, rhs Expression) Expression {
	return binOpE(EqualityPrecedence, "==", lhs, rhs)
}

func NotEqualE(lhs Expression, rhs Expression) Expression {
	return binOpE(EqualityPrecedence, "!=", lhs, rhs)
}

func LessThanE(lhs Expression, rhs Expression) Expression {
	return binOpE(RelationalPrecedence, "<", lhs, rhs)
}

func LessThanOrEqualE(lhs Expression, rhs Expression) Expression {
	return binOpE(RelationalPrecedence, "<=", lhs, rhs)
}

func GreaterThanE(lhs Expression, rhs Expression) Expression {
	return binOpE(RelationalPrecedence, ">", lhs, rhs)
}

func GreaterThanOrEqualE(lhs Expression, rhs Expression) Expression {
	return binOpE(RelationalPrecedence, ">=", lhs, rhs)
}

func PrefixIncrementE(e Expression) Expression {
	return prefixUnOpE(UnaryPrecedence, "++", e)
}

func PrefixDecrementE(e Expression) Expression {
	return prefixUnOpE(UnaryPrecedence, "--", e)
}

func PostfixIncrementE(e Expression) Expression {
	return postfixUnOpE(PostfixPrecedence, "++", e)
}

func PostfixDecrementE(e Expression) Expression {
	return postfixUnOpE(PostfixPrecedence, "--", e)
}

func TernaryE(
	condition Expression,
	t Expression,
	f Expression,
) Expression {
	return &expressionImpl{
		precedence: TernaryPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			PrecedenceE(TernaryPrecedence, condition).WriteBlockSource(b, indent)
			b.WriteString(" ? ")
			PrecedenceE(TernaryPrecedence, t).WriteBlockSource(b, indent)
			b.WriteString(" : ")
			PrecedenceE(TernaryPrecedence, f).WriteBlockSource(b, indent)
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			PrecedenceE(TernaryPrecedence, condition).WriteInlineSource(b)
			b.WriteString(" ? ")
			PrecedenceE(TernaryPrecedence, t).WriteInlineSource(b)
			b.WriteString(" : ")
			PrecedenceE(TernaryPrecedence, f).WriteInlineSource(b)
		},
	}
}

func appE(
	f Expression,
	args ...Expression,
) Expression {
	filteredArgs := make([]Expression, 0, len(args))
	for _, arg := range args {
		if arg != nil {
			filteredArgs = append(filteredArgs, arg)
		}
	}

	return &expressionImpl{
		precedence: ApplicationOrLiteralPrecedence,
		writeBlockSourceImpl: func(b *bytes.Buffer, indent string) {
			if f != nil {
				f.WriteBlockSource(b, indent)
			}

			b.WriteString("(")

			if len(filteredArgs) > 0 {
				b.WriteString("\n")
				b.WriteString(indent)
				b.WriteString(Indentation)

				for i, arg := range filteredArgs {
					if i > 0 {
						b.WriteString(",\n")
						b.WriteString(indent)
						b.WriteString(Indentation)
					}

					arg.WriteSource(b, indent+Indentation)
				}

				b.WriteString("\n")
				b.WriteString(indent)
			}

			b.WriteString(")")
		},
		writeInlineSourceImpl: func(b *bytes.Buffer) {
			if f != nil {
				f.WriteInlineSource(b)
			}

			b.WriteString("(")

			for i, arg := range filteredArgs {
				if i > 0 {
					b.WriteString(", ")
				}

				arg.WriteInlineSource(b)
			}

			b.WriteString(")")
		},
	}
}
