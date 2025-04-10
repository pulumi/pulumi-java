package names

import (
	"fmt"
	"sort"
	"strings"
)

// FQN represents a fully qualified names (1+ identifiers with dots in
// the middle). For example, `javax.annotation.Nullable`:
//
//	Ident("javax").FQN().Dot("annotation").Dot("Nullable")
type FQN struct {
	prefix []Ident
	id     Ident
}

// Appends a segment to the FQN. For example:
//
//	var JavaLang = Ident("java").FQN().Dot("lang") // java.lang
//
//	JavaLang.Dot("Boolean") // java.lang.Boolean
func (fqn FQN) Dot(id Ident) FQN {
	return FQN{prefix: append(fqn.prefix, fqn.id), id: id}
}

func (fqn FQN) String() string {
	return strings.Join(fqn.Strings(), ".")
}

func (fqn FQN) Strings() []string {
	var elements []string
	for _, p := range fqn.prefix {
		elements = append(elements, p.String())
	}
	elements = append(elements, fqn.id.String())
	return elements
}

func (fqn FQN) Equal(other FQN) bool {
	if other.id != fqn.id {
		return false
	}
	if len(other.prefix) != len(fqn.prefix) {
		return false
	}
	for i, x := range other.prefix {
		if fqn.prefix[i] != x {
			return false
		}
	}
	return true
}

// The base name. For example if the current FQN represents
// java.lang.Boolean, BaseIdent is Boolean.
func (fqn FQN) BaseIdent() Ident {
	return fqn.id
}

type Imports struct {
	pkg      FQN
	pubClass Ident
	imports  map[Ident]FQN
}

func NewImports(pkg FQN, pubClass Ident) *Imports {
	bound := pkg.Dot(pubClass)
	i := &Imports{pkg, pubClass, map[Ident]FQN{}}
	if err := i.add(bound); err != nil {
		panic(fmt.Sprintf("Impossible: %v", err))
	}
	return i
}

func (i *Imports) PackageCode() string {
	return fmt.Sprintf("package %s;", i.pkg.String())
}

func (i *Imports) ImportCode() string {
	lines := []string{}
	for _, fqn := range i.imports {
		if fqn.Equal(i.pkg.Dot(i.pubClass)) {
			continue // do not import self
		}
		lines = append(lines, fmt.Sprintf("import %s;", fqn.String()))
	}
	sort.Strings(lines)
	return strings.Join(lines, "\n")
}

func (i *Imports) add(name FQN) error {
	if len(name.prefix) == 0 {
		return fmt.Errorf("Refusing to import unqualified name: %s", name.String())
	}
	if old, conflict := i.imports[name.id]; conflict && !old.Equal(name) {
		return fmt.Errorf("Import conflict on %s", name.String())
	}
	i.imports[name.id] = name
	return nil
}

func (i *Imports) Resolve(name Ident) FQN {
	if fqn, got := i.imports[name]; got {
		return fqn
	}
	return name.FQN()
}

func (i *Imports) Ref(name FQN) string {
	_ = i.add(name) // try adding, ignore error deliberately
	if i.Resolve(name.id).Equal(name) {
		return name.id.String()
	}
	return name.String()
}
