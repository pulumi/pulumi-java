package names

import (
	"fmt"
	"sort"
	"strings"
)

// A valid Java identifier
type Ident string

func (id Ident) FQN() FQN {
	return FQN{[]Ident{}, id}
}

// FQN represents a fully qualified names (1+ identifiers with dots in the middle)
type FQN struct {
	prefix []Ident
	id     Ident
}

func (fqn FQN) Dot(id Ident) FQN {
	return FQN{prefix: append(fqn.prefix, fqn.id), id: id}
}

func (fqn FQN) ToString() string {
	var elements []string
	for _, p := range fqn.prefix {
		elements = append(elements, string(p))
	}
	elements = append(elements, string(fqn.id))
	return strings.Join(elements, ".")
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

type Imports struct {
	pkg      FQN
	pubClass Ident
	imports  map[Ident]FQN
}

func NewImports(pkg FQN, pubClass Ident) (*Imports, error) {
	bound := pkg.Dot(pubClass)
	i := &Imports{pkg, pubClass, map[Ident]FQN{}}
	if err := i.add(bound); err != nil {
		return nil, err
	}
	return i, nil
}

func (i *Imports) PackageCode() string {
	return fmt.Sprintf("package %s;", i.pkg.ToString())
}

func (i *Imports) ImportCode() string {
	lines := []string{}
	for _, fqn := range i.imports {
		if fqn.Equal(i.pkg.Dot(i.pubClass)) {
			continue // do not import self
		}
		lines = append(lines, fmt.Sprintf("import %s;", fqn.ToString()))
	}
	sort.Strings(lines)
	return strings.Join(lines, "\n")
}

func (i *Imports) add(name FQN) error {
	if len(name.prefix) == 0 {
		return fmt.Errorf("Refusing to import unqualified name: %s", name.ToString())
	}
	if old, conflict := i.imports[name.id]; conflict && !old.Equal(name) {
		return fmt.Errorf("Import conflict on %s", name.ToString())
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
	i.add(name) // try adding, ignore error deliberately
	if i.Resolve(name.id).Equal(name) {
		return string(name.id)
	}
	return name.ToString()
}
