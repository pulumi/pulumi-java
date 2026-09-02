// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"slices"
	"sort"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

// unionMember is one member of a union interface. An object member of this package implements the
// interface. Every other member is held by a case class and constructed through a static factory.
type unionMember struct {
	// key identifies the member across the input and output shapes of the union.
	key string
	// object is the plain shape of an object member of this package, or nil for a wrapped member.
	object *schema.ObjectType
	// caseName is the nested case class of a wrapped member, such as OfString.
	caseName string
}

// unionSpec is the interface generated for one schema location that holds a union. Its members
// are the same in every shape; the shapes differ only in how the member types are referred to.
type unionSpec struct {
	name string
	// ownerToken places the interface in the module of the resource, function, or type that
	// declares the property.
	ownerToken string
	members    []unionMember
	// forms holds the union type at this location in each shape it is bound in, keyed by whether
	// the shape is an input shape.
	forms map[bool]*schema.UnionType
	// locations holds the property that binds this location in each shape.
	locations map[bool]*schema.Property
	// supersets are the specs whose member sets strictly contain this one's.
	supersets []*unionSpec
	// equals are the specs with exactly this member set.
	equals []*unionSpec
}

func (u *unionSpec) keys() []string {
	keys := make([]string, 0, len(u.members))
	for _, m := range u.members {
		keys = append(keys, m.key)
	}
	return keys
}

// directSupersets returns the supersets that are not themselves supersets of another superset, so
// the generated interface declaration lists each parent once.
func (u *unionSpec) directSupersets() []*unionSpec {
	var direct []*unionSpec
	for _, candidate := range u.supersets {
		redundant := false
		for _, other := range u.supersets {
			if other != candidate && isStrictSubset(other.keys(), candidate.keys()) {
				redundant = true
				break
			}
		}
		if !redundant {
			direct = append(direct, candidate)
		}
	}
	return direct
}

// unionRegistry names the union interfaces of a package. It is shared by every module of that
// package.
type unionRegistry struct {
	// The schema binder shares one union value between every location with the same members, and
	// type rendering rebuilds union values as it strips input wrappers, so a union is resolved by
	// the property that holds it and its member key. byKey keeps the first location of each member
	// set for the callers that render a type without a property.
	byLocation map[*schema.Property]map[string]unionForm
	byKey      map[string]unionForm
	byMember   map[string][]*unionSpec
	// byFQN records the generated interface behind each Java type the registry has handed out,
	// so builder helpers can recognise a union-typed property from its TypeShape.
	byFQN map[string]unionQueueEntry
}

// unionForm is a union type bound at one location in one shape.
type unionForm struct {
	spec       *unionSpec
	inputShape bool
}

func (r *unionRegistry) lookup(location *schema.Property, t *schema.UnionType) (unionForm, bool) {
	if r == nil {
		return unionForm{}, false
	}
	key := memberKey(t)
	if location != nil {
		if form, ok := r.byLocation[location][key]; ok {
			return form, true
		}
	}
	form, ok := r.byKey[key]
	return form, ok
}

func (r *unionRegistry) containing(token string) []*unionSpec {
	if r == nil {
		return nil
	}
	return r.byMember[token]
}

func (r *unionRegistry) entryFor(fqn names.FQN) (unionQueueEntry, bool) {
	if r == nil {
		return unionQueueEntry{}, false
	}
	entry, ok := r.byFQN[fqn.String()]
	return entry, ok
}

// flattenUnion resolves the concrete members of t through optional, input, token, and nested
// union wrappers, in declaration order.
func flattenUnion(t *schema.UnionType) []schema.Type {
	var members []schema.Type
	var visit func(schema.Type)
	visit = func(t schema.Type) {
		switch t := t.(type) {
		case *schema.OptionalType:
			visit(t.ElementType)
		case *schema.InputType:
			visit(t.ElementType)
		case *schema.TokenType:
			if t.UnderlyingType != nil {
				visit(t.UnderlyingType)
			} else {
				members = append(members, t)
			}
		case *schema.UnionType:
			for _, e := range t.ElementTypes {
				visit(e)
			}
		default:
			members = append(members, t)
		}
	}
	visit(t)
	return members
}

// plainShape returns the non-input shape of an object type.
func plainShape(obj *schema.ObjectType) *schema.ObjectType {
	if obj.PlainShape != nil {
		return obj.PlainShape
	}
	return obj
}

// memberKey identifies a member type independently of the shape it is bound in.
func memberKey(t schema.Type) string {
	switch t := t.(type) {
	case *schema.OptionalType:
		return memberKey(t.ElementType)
	case *schema.InputType:
		return memberKey(t.ElementType)
	case *schema.ArrayType:
		return "list<" + memberKey(t.ElementType) + ">"
	case *schema.MapType:
		return "map<" + memberKey(t.ElementType) + ">"
	case *schema.ObjectType:
		return "object:" + plainShape(t).Token
	case *schema.EnumType:
		return "enum:" + t.Token
	case *schema.ResourceType:
		return "resource:" + t.Token
	case *schema.TokenType:
		if t.UnderlyingType != nil {
			return memberKey(t.UnderlyingType)
		}
		return "token:" + t.Token
	case *schema.UnionType:
		keys := make([]string, 0, len(t.ElementTypes))
		for _, e := range flattenUnion(t) {
			keys = append(keys, memberKey(e))
		}
		sort.Strings(keys)
		return "union<" + strings.Join(keys, "|") + ">"
	default:
		return t.String()
	}
}

// caseBaseName is the name of the case class that wraps a member, before collisions are resolved.
func caseBaseName(t schema.Type) string {
	switch t := t.(type) {
	case *schema.OptionalType:
		return caseBaseName(t.ElementType)
	case *schema.InputType:
		return caseBaseName(t.ElementType)
	case *schema.ArrayType:
		return "OfList"
	case *schema.MapType:
		return "OfMap"
	case *schema.ObjectType:
		return "Of" + tokenToName(plainShape(t).Token)
	case *schema.EnumType:
		return "Of" + tokenToName(t.Token)
	case *schema.ResourceType:
		if strings.HasPrefix(t.Token, "pulumi:providers:") {
			return "OfProvider"
		}
		return "Of" + tokenToName(t.Token)
	case *schema.TokenType:
		if t.UnderlyingType != nil {
			return caseBaseName(t.UnderlyingType)
		}
		return "Of" + tokenToName(t.Token)
	}
	switch t {
	case schema.StringType:
		return "OfString"
	case schema.IntType:
		return "OfInteger"
	case schema.NumberType:
		return "OfNumber"
	case schema.BoolType:
		return "OfBoolean"
	case schema.ArchiveType:
		return "OfArchive"
	case schema.AssetType:
		return "OfAssetOrArchive"
	}
	return "OfValue"
}

// unionMembers resolves the members of t. An object of pkg becomes an implementing member; every
// other type becomes a wrapped member with a case class.
func unionMembers(t *schema.UnionType, pkg *schema.Package) []unionMember {
	var members []unionMember
	seen := codegen.StringSet{}
	caseNames := codegen.StringSet{}
	for _, e := range flattenUnion(t) {
		key := memberKey(e)
		if seen.Has(key) {
			continue
		}
		seen.Add(key)

		if obj, ok := e.(*schema.ObjectType); ok && codegen.PkgEquals(obj.PackageReference, pkg.Reference()) {
			members = append(members, unionMember{key: key, object: plainShape(obj)})
			continue
		}

		base := caseBaseName(e)
		caseName := base
		for i := 2; caseNames.Has(caseName); i++ {
			caseName = fmt.Sprintf("%s%d", base, i)
		}
		caseNames.Add(caseName)
		members = append(members, unionMember{key: key, caseName: caseName})
	}
	return members
}

// isStrictSubset reports whether every key of sub is in super, and super has more keys.
func isStrictSubset(sub, super []string) bool {
	if len(sub) >= len(super) {
		return false
	}
	for _, key := range sub {
		if !slices.Contains(super, key) {
			return false
		}
	}
	return true
}

func sameKeys(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for _, key := range a {
		if !slices.Contains(b, key) {
			return false
		}
	}
	return true
}

// visitUnionTypes calls visitor for every union held by t, along with the number of collections the
// union is nested in. It does not descend into object types: a union nested in an object belongs to
// that object's own property, and object types are visited in their own right. Unions nested in a
// union are flattened into it, so they are not visited separately.
func visitUnionTypes(t schema.Type, depth int, visitor func(*schema.UnionType, int)) {
	switch t := t.(type) {
	case *schema.OptionalType:
		visitUnionTypes(t.ElementType, depth, visitor)
	case *schema.InputType:
		visitUnionTypes(t.ElementType, depth, visitor)
	case *schema.ArrayType:
		visitUnionTypes(t.ElementType, depth+1, visitor)
	case *schema.MapType:
		visitUnionTypes(t.ElementType, depth+1, visitor)
	case *schema.UnionType:
		visitor(t, depth)
		for _, e := range flattenUnion(t) {
			switch e.(type) {
			case *schema.ArrayType, *schema.MapType:
				visitUnionTypes(e, depth+1, visitor)
			}
		}
	}
}

// unionPosition is a schema location that holds a union.
type unionPosition struct {
	ownerName    string
	ownerToken   string
	propertyName string
	union        *schema.UnionType
	property     *schema.Property
	inputShape   bool
	depth        int
	seq          int
}

// registerUnions names every union in pkg that IsWireDiscriminatableUnionType accepts. Each schema
// location gets its own interface, named after the owner and the property, so a name never
// depends on what other locations hold. The input and output shapes of one location share a spec.
//
// The registry stays empty unless the package sets the fullyTypedUnions option, because typing a
// union that used to be java.lang.Object or Either<L, R> breaks the callers of the generated SDK.
func registerUnions(pkg *schema.Package, fullyTypedUnions bool) *unionRegistry {
	reg := &unionRegistry{
		byLocation: map[*schema.Property]map[string]unionForm{},
		byKey:      map[string]unionForm{},
		byMember:   map[string][]*unionSpec{},
		byFQN:      map[string]unionQueueEntry{},
	}
	if !fullyTypedUnions {
		return reg
	}

	// Reserve the names of the types the package already generates so an interface can never shadow
	// one of them. The Args suffix follows the base name, so reserving the base name is enough.
	taken := codegen.StringSet{}
	reserve := func(mod, name string) bool {
		key := mod + "/" + name
		if taken.Has(key) {
			return false
		}
		taken.Add(key)
		return true
	}
	for _, t := range pkg.Types {
		switch t := t.(type) {
		case *schema.ObjectType:
			reserve(pkg.TokenToModule(t.Token), tokenToName(t.Token))
		case *schema.EnumType:
			reserve(pkg.TokenToModule(t.Token), tokenToName(t.Token))
		}
	}
	for _, r := range pkg.Resources {
		reserve(pkg.TokenToModule(r.Token), resourceName(r))
		reserve(pkg.TokenToModule(r.Token), resourceName(r)+"State")
	}
	for _, f := range pkg.Functions {
		name := tokenToName(f.Token)
		reserve(pkg.TokenToModule(f.Token), name)
		reserve(pkg.TokenToModule(f.Token), name+"Plain")
		reserve(pkg.TokenToModule(f.Token), name+"Result")
	}

	var positions []unionPosition
	collect := func(ownerName, ownerToken string, props []*schema.Property, inputShape bool) {
		for _, p := range props {
			propertyName := names.Title(p.Name)
			visitUnionTypes(p.Type, 0, func(union *schema.UnionType, depth int) {
				if !codegen.IsWireDiscriminatableUnionType(union) {
					return
				}
				positions = append(positions, unionPosition{
					ownerName:    ownerName,
					ownerToken:   ownerToken,
					propertyName: propertyName,
					union:        union,
					property:     p,
					inputShape:   inputShape,
					depth:        depth,
					seq:          len(positions),
				})
			})
		}
	}

	if pkg.Provider != nil {
		collect(resourceName(pkg.Provider), pkg.Provider.Token, pkg.Provider.InputProperties, true)
		collect(resourceName(pkg.Provider), pkg.Provider.Token, pkg.Provider.Properties, false)
	}

	resources := slices.Clone(pkg.Resources)
	sort.Slice(resources, func(i, j int) bool { return resources[i].Token < resources[j].Token })
	for _, r := range resources {
		collect(resourceName(r), r.Token, r.InputProperties, true)
		collect(resourceName(r), r.Token, r.Properties, false)
		if r.StateInputs != nil {
			collect(resourceName(r), r.Token, r.StateInputs.Properties, true)
		}
	}

	functions := slices.Clone(pkg.Functions)
	sort.Slice(functions, func(i, j int) bool { return functions[i].Token < functions[j].Token })
	for _, f := range functions {
		name := tokenToName(f.Token)
		if f.Inputs != nil {
			collect(name, f.Token, f.Inputs.Properties, false)
			if f.Inputs.InputShape != nil {
				collect(name, f.Token, f.Inputs.InputShape.Properties, true)
			}
		}
		if f.Outputs != nil {
			collect(name, f.Token, f.Outputs.Properties, false)
		}
	}

	// pkg.Types lists the input shape of an object type alongside its plain shape.
	var objects []*schema.ObjectType
	for _, t := range pkg.Types {
		if obj, ok := t.(*schema.ObjectType); ok {
			objects = append(objects, obj)
		}
	}
	sort.SliceStable(objects, func(i, j int) bool {
		if objects[i].Token != objects[j].Token {
			return objects[i].Token < objects[j].Token
		}
		return !objects[i].IsInputShape() && objects[j].IsInputShape()
	})
	for _, obj := range objects {
		collect(tokenToName(obj.Token), obj.Token, obj.Properties, obj.IsInputShape())
	}

	// A union held directly by a property names the interface before one buried in a list or a
	// map, so shallower positions take the plain name.
	sort.SliceStable(positions, func(i, j int) bool {
		if positions[i].depth != positions[j].depth {
			return positions[i].depth < positions[j].depth
		}
		return positions[i].seq < positions[j].seq
	})

	// The input and output shapes of one location hold different union values with the same
	// members. They share one spec, found by location and member set.
	type location struct {
		owner, property, members string
	}
	specs := map[location]*unionSpec{}
	var all []*unionSpec
	for _, pos := range positions {
		members := unionMembers(pos.union, pkg)
		keys := make([]string, 0, len(members))
		for _, m := range members {
			keys = append(keys, m.key)
		}
		sort.Strings(keys)
		loc := location{pos.ownerName, pos.propertyName, strings.Join(keys, "|")}

		spec, ok := specs[loc]
		if !ok {
			mod := pkg.TokenToModule(pos.ownerToken)
			candidate := pos.ownerName + pos.propertyName
			name := candidate
			for i := 2; !reserve(mod, names.Ident(name).String()); i++ {
				name = fmt.Sprintf("%s%d", candidate, i)
			}
			spec = &unionSpec{
				name:       name,
				ownerToken: pos.ownerToken,
				members:    members,
				forms:      map[bool]*schema.UnionType{},
				locations:  map[bool]*schema.Property{},
			}
			specs[loc] = spec
			all = append(all, spec)
			for _, m := range members {
				if m.object != nil {
					reg.byMember[m.object.Token] = append(reg.byMember[m.object.Token], spec)
				}
			}
		}
		form := unionForm{spec: spec, inputShape: pos.inputShape}
		if _, has := spec.forms[pos.inputShape]; !has {
			spec.forms[pos.inputShape] = pos.union
			spec.locations[pos.inputShape] = pos.property
		}
		key := memberKey(pos.union)
		if _, has := reg.byLocation[pos.property]; !has {
			reg.byLocation[pos.property] = map[string]unionForm{}
		}
		reg.byLocation[pos.property][key] = form
		if _, has := reg.byKey[key]; !has {
			reg.byKey[key] = form
		}
	}

	sort.Slice(all, func(i, j int) bool { return all[i].name < all[j].name })
	for _, spec := range all {
		for _, other := range all {
			if spec == other {
				continue
			}
			switch {
			case isStrictSubset(spec.keys(), other.keys()):
				spec.supersets = append(spec.supersets, other)
			case sameKeys(spec.keys(), other.keys()):
				spec.equals = append(spec.equals, other)
			}
		}
	}
	for _, specs := range reg.byMember {
		sort.Slice(specs, func(i, j int) bool { return specs[i].name < specs[j].name })
	}
	return reg
}

// unionQueueEntry is one generated form of a union interface, matching the form its member classes
// are generated in.
type unionQueueEntry struct {
	packageName names.FQN
	className   names.Ident
	spec        *unionSpec
	qualifier   qualifier
	input       bool
	// inputShape reports whether the members are referred to in their Args shape.
	inputShape bool
}

// memberType returns the type of member m in the shape this entry generates.
func (e unionQueueEntry) memberType(m unionMember) schema.Type {
	form, ok := e.spec.forms[e.inputShape]
	if !ok {
		// The location is not bound in this shape; fall back to the other one.
		for _, form = range e.spec.forms {
			break
		}
	}
	for _, t := range flattenUnion(form) {
		if memberKey(t) == m.key {
			return t
		}
	}
	panic(fmt.Sprintf("union %s has no member %s", e.spec.name, m.key))
}

// withUnionLocation sets the property whose type is being rendered, so that a union shared between
// locations resolves to the interface of this one. It returns the function that restores the
// previous location.
func (mod *modContext) withUnionLocation(p *schema.Property) func() {
	previous := mod.unionLocation
	mod.unionLocation = p
	return func() { mod.unionLocation = previous }
}

func unionShapeSuffix(inputShape bool) string {
	if inputShape {
		return "Args"
	}
	return ""
}

// unionInterfaceFQN is the fully qualified name of the interface generated for spec in the given
// shape and package qualifier, and queues that interface for generation.
func (mod *modContext) unionInterfaceFQN(
	spec *unionSpec, qualifier qualifier, input bool, inputShape bool,
) names.FQN {
	packageName, err := parsePackageName(mod.tokenToPackage(spec.ownerToken, qualifier))
	if err != nil {
		panic(err)
	}
	className := names.Ident(spec.name + unionShapeSuffix(inputShape))
	entry := unionQueueEntry{
		packageName: packageName,
		className:   className,
		spec:        spec,
		qualifier:   qualifier,
		input:       input,
		inputShape:  inputShape,
	}
	fqn := packageName.Dot(className)
	mod.unions.byFQN[fqn.String()] = entry
	if mod.classQueue != nil {
		mod.classQueue.ensureInterfaceGenerated(entry)
	}
	return fqn
}

// unionTypeString renders the interface generated for t, if t has one.
func (mod *modContext) unionTypeString(t *schema.UnionType, qualifier qualifier, input bool) (TypeShape, bool) {
	form, ok := mod.unions.lookup(mod.unionLocation, t)
	if !ok {
		return TypeShape{}, false
	}
	fqn := mod.unionInterfaceFQN(form.spec, qualifier, input, form.inputShape)
	return TypeShape{Type: fqn}, true
}

// unionInterfaces returns the union interfaces obj implements in the shape being generated. Only
// the narrowest applicable interfaces are listed, because the wider ones are reachable through them.
func (mod *modContext) unionInterfaces(obj *schema.ObjectType, qualifier qualifier, input bool) []names.FQN {
	matches := mod.unions.containing(plainShape(obj).Token)
	inputShape := obj.IsInputShape()

	var interfaces []names.FQN
	for _, spec := range matches {
		if _, bound := spec.forms[inputShape]; !bound {
			continue
		}
		redundant := false
		for _, other := range matches {
			if other != spec && slices.Contains(other.supersets, spec) {
				redundant = true
				break
			}
		}
		if redundant {
			continue
		}
		interfaces = append(interfaces, mod.unionInterfaceFQN(spec, qualifier, input, inputShape))
	}
	return interfaces
}

// wrappedMembers returns the members of the interface that are held by case classes, with the Java
// type each one wraps in the shape being generated.
func (mod *modContext) wrappedMembers(ctx *classFileContext, entry unionQueueEntry) []wrappedMember {
	var wrapped []wrappedMember
	for _, m := range entry.spec.members {
		if m.object != nil {
			continue
		}
		wireType := entry.memberType(m)
		shape := mod.typeStringRecHelper(ctx, wireType, entry.qualifier, entry.input, false, false)
		wrapped = append(wrapped, wrappedMember{
			caseName: m.caseName,
			shape:    shape,
			factory:  factoryParameterType(wireType),
		})
	}
	return wrapped
}

type wrappedMember struct {
	caseName string
	// shape is the boxed Java type the case class holds.
	shape TypeShape
	// factory is the parameter type of the static factory: a primitive for a scalar member.
	factory string
}

// factoryParameterType is the parameter type of the of(...) factory for a wrapped member. Scalars
// take primitives so that of(1) resolves against a number member through widening.
func factoryParameterType(wireType schema.Type) string {
	switch codegen.UnwrapType(wireType) {
	case schema.IntType:
		return "int"
	case schema.NumberType:
		return "double"
	case schema.BoolType:
		return "boolean"
	}
	return ""
}

// genUnionInterface emits the interface that stands for a union: the annotations the runtime
// selects members with, a static factory and a case class for every wrapped member, and a factory
// for every interface with the same member set.
func (mod *modContext) genUnionInterface(ctx *classFileContext, entry unionQueueEntry) error {
	w := ctx.writer
	spec := entry.spec
	self := entry.className.String()

	// Members that hold a nested union resolve it through the property that binds this location.
	location, ok := spec.locations[entry.inputShape]
	if !ok {
		for _, location = range spec.locations {
			break
		}
	}
	defer mod.withUnionLocation(location)()

	wrapped := mod.wrappedMembers(ctx, entry)

	// Only the interfaces bound in this shape take part in the relations of this form.
	var equals []string
	for _, equal := range spec.equals {
		if _, bound := equal.forms[entry.inputShape]; bound {
			equals = append(equals, ctx.ref(mod.unionInterfaceFQN(equal, entry.qualifier, entry.input, entry.inputShape)))
		}
	}

	fprintf(w, "\n")
	fprintf(w, "@%s\n", ctx.ref(names.UnionType))
	for _, m := range spec.members {
		if m.object != nil {
			memberType := mod.typeStringForObjectType(
				objectShape(m.object, entry.inputShape), entry.qualifier, entry.input)
			fprintf(w, "@%s.Case(type = %s.class)\n", ctx.ref(names.UnionType), memberType.ToCode(ctx.imports))
		}
	}
	for _, m := range wrapped {
		refs, tree := m.shape.ToTree(ctx.imports)
		fprintf(w, "@%s.Case(type = %s.%s.class, refs = {%s}, tree = %s)\n",
			ctx.ref(names.UnionType), self, m.caseName, refs, javaStringLiteral(tree))
	}

	var extends string
	var parents []string
	for _, superset := range spec.directSupersets() {
		if _, bound := superset.forms[entry.inputShape]; bound {
			parents = append(parents, ctx.ref(mod.unionInterfaceFQN(
				superset, entry.qualifier, entry.input, entry.inputShape)))
		}
	}
	if len(parents) > 0 {
		extends = " extends " + strings.Join(parents, ", ")
	}

	fprintf(w, "public interface %s%s {\n", self, extends)

	for _, m := range wrapped {
		parameterType := m.factory
		if parameterType == "" {
			parameterType = m.shape.ToCode(ctx.imports)
		}
		fprintf(w, "    static %s of(%s value) {\n", self, parameterType)
		fprintf(w, "        return new %s.%s(value);\n", self, m.caseName)
		fprintf(w, "    }\n\n")
	}
	for _, equal := range equals {
		fprintf(w, "    static %s of(%s value) {\n", self, equal)
		fprintf(w, "        return (%s) value;\n", self)
		fprintf(w, "    }\n\n")
	}

	implements := append([]string{self}, equals...)
	implements = append(implements, ctx.ref(names.UnionCase))
	for _, m := range wrapped {
		valueType := m.shape.ToCode(ctx.imports)
		fprintf(w, "    final class %s implements %s {\n", m.caseName, strings.Join(implements, ", "))
		fprintf(w, "        private final %s value;\n\n", valueType)
		fprintf(w, "        private %s(%s value) {\n", m.caseName, valueType)
		fprintf(w, "            this.value = %s.requireNonNull(value);\n", ctx.ref(names.Objects))
		fprintf(w, "        }\n\n")
		fprintf(w, "        @Override\n")
		fprintf(w, "        public %s value() {\n", valueType)
		fprintf(w, "            return this.value;\n")
		fprintf(w, "        }\n\n")
		fprintf(w, "        @Override\n")
		fprintf(w, "        public boolean equals(%s other) {\n", ctx.ref(names.Object))
		fprintf(w, "            return other instanceof %[1]s.%[2]s && this.value.equals(((%[1]s.%[2]s) other).value);\n",
			self, m.caseName)
		fprintf(w, "        }\n\n")
		fprintf(w, "        @Override\n")
		fprintf(w, "        public int hashCode() {\n")
		fprintf(w, "            return this.value.hashCode();\n")
		fprintf(w, "        }\n\n")
		fprintf(w, "        @Override\n")
		fprintf(w, "        public %s toString() {\n", ctx.ref(names.String))
		fprintf(w, "            return \"%s.%s(\" + this.value + \")\";\n", self, m.caseName)
		fprintf(w, "        }\n")
		fprintf(w, "    }\n\n")
	}

	fprintf(w, "}\n")
	return nil
}

// objectShape returns obj in the requested shape.
func objectShape(obj *schema.ObjectType, inputShape bool) *schema.ObjectType {
	if inputShape && obj.InputShape != nil {
		return obj.InputShape
	}
	return plainShape(obj)
}

// javaStringLiteral renders s as a Java string literal.
func javaStringLiteral(s string) string {
	var b strings.Builder
	b.WriteByte('"')
	for _, r := range s {
		switch r {
		case '\\':
			b.WriteString(`\\`)
		case '"':
			b.WriteString(`\"`)
		case '\n':
			b.WriteString(`\n`)
		case '\r':
			b.WriteString(`\r`)
		case '\t':
			b.WriteString(`\t`)
		default:
			b.WriteRune(r)
		}
	}
	b.WriteByte('"')
	return b.String()
}

// constValueLiteral renders a schema constant as the string literal the ConstValue annotation
// carries. The runtime parses it according to the type of the annotated property.
func constValueLiteral(v interface{}) string {
	if s, ok := v.(string); ok {
		return javaStringLiteral(s)
	}
	return javaStringLiteral(fmt.Sprint(v))
}
