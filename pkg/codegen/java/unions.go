// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"sort"
	"strings"

	mapset "github.com/deckarep/golang-set/v2"
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
	// factory is the static factory of a wrapped member: of, or the case name in lower camel case
	// when another wrapped member has the same erased Java type.
	factory string
}

// formKey identifies one generated form of a union interface: the Java package qualifier it is
// generated under, and whether its members are referred to in their Args shape.
type formKey struct {
	qualifier  qualifier
	inputShape bool
}

// unionBinding is the union value and the property that bind a location in one form.
type unionBinding struct {
	union    *schema.UnionType
	property *schema.Property
}

// unionSpec is the interface generated for one schema location that holds a union.
type unionSpec struct {
	name string
	// ownerToken places the interface in the module of the resource, function, or type that
	// declares the property.
	ownerToken string
	members    []unionMember
	keys       mapset.Set[string]
	forms      map[formKey]unionBinding
	// supersets are the specs whose member sets strictly contain this one's.
	supersets []*unionSpec
	// equals are the specs with exactly this member set.
	equals []*unionSpec
}

func (u *unionSpec) bound(key formKey) bool {
	_, ok := u.forms[key]
	return ok
}

// unionRegistry names the union interfaces of a package. It is shared by every module of that
// package.
type unionRegistry struct {
	enabled bool
	// The schema binder shares one union value between every location with the same members, and
	// type rendering rebuilds union values as it strips input wrappers, so a union is resolved by
	// the property that holds it and its member key.
	byLocation map[*schema.Property]map[string]unionForm
	byMember   map[string][]*unionSpec
	specs      []*unionSpec
}

// unionForm is a union type bound at one location in one shape.
type unionForm struct {
	spec       *unionSpec
	inputShape bool
}

func (r *unionRegistry) lookup(location *schema.Property, t *schema.UnionType) (unionForm, bool) {
	if r == nil || location == nil {
		return unionForm{}, false
	}
	form, ok := r.byLocation[location][memberKey(t)]
	return form, ok
}

func (r *unionRegistry) containing(token string) []*unionSpec {
	if r == nil {
		return nil
	}
	return r.byMember[token]
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
	switch t := codegen.UnwrapType(t).(type) {
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
		return caseBaseName(t.UnderlyingType)
	}
	switch codegen.UnwrapType(t) {
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

// erasure is the erased Java type of a wrapped member. Two factories with the same erasure cannot
// overload one another.
func erasure(t schema.Type) string {
	switch t := codegen.UnwrapType(t).(type) {
	case *schema.ArrayType:
		return "List"
	case *schema.MapType:
		return "Map"
	case *schema.ObjectType, *schema.EnumType, *schema.ResourceType:
		return memberKey(t)
	case *schema.TokenType:
		return erasure(t.UnderlyingType)
	}
	return caseBaseName(t)
}

// typedInput reports whether the input form of t gets an interface: every member has a Java type
// of its own.
func typedInput(t *schema.UnionType) bool {
	for _, m := range flattenUnion(t) {
		if m == schema.AnyType || m == schema.JSONType {
			return false
		}
		if token, ok := m.(*schema.TokenType); ok && token.UnderlyingType == nil {
			return false
		}
	}
	return true
}

// collapsedUnionType is the type of a union that gets no interface: the primitive every member
// shares, or Object.
func collapsedUnionType(t *schema.UnionType) TypeShape {
	var shared names.FQN
	for _, m := range flattenUnion(t) {
		if e, ok := m.(*schema.EnumType); ok {
			m = e.ElementType
		}
		var primitive names.FQN
		switch m {
		case schema.StringType:
			primitive = names.String
		case schema.IntType, schema.NumberType:
			primitive = names.Double
		case schema.BoolType:
			primitive = names.Boolean
		default:
			return TypeShape{Type: names.Object}
		}
		if shared.String() != "" && !shared.Equal(primitive) {
			return TypeShape{Type: names.Object}
		}
		shared = primitive
	}
	return TypeShape{Type: shared}
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
		members = append(members, unionMember{key: key, caseName: caseName, factory: "of"})
	}

	erasures := map[string]int{}
	for _, e := range flattenUnion(t) {
		erasures[erasure(e)]++
	}
	for i, m := range members {
		if m.object == nil && erasures[erasure(flattenUnion(t)[memberIndex(t, m.key)])] > 1 {
			members[i].factory = names.LowerCamelCase(m.caseName)
		}
	}
	return members
}

// memberIndex returns the position of the member with the given key among the flattened members
// of t.
func memberIndex(t *schema.UnionType, key string) int {
	for i, e := range flattenUnion(t) {
		if memberKey(e) == key {
			return i
		}
	}
	panic(fmt.Sprintf("union has no member %s", key))
}

// visitUnionTypes calls visitor for every union held by t, along with the number of collections the
// union is nested in. It does not descend into object types.
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
	forms        []formKey
	depth        int
}

var (
	inputArgsForm    = []formKey{{inputsQualifier, true}}
	inputPlainForm   = []formKey{{inputsQualifier, false}}
	outputForm       = []formKey{{outputsQualifier, false}}
	plainObjectForms = []formKey{{inputsQualifier, false}, {outputsQualifier, false}}
)

// registerUnions names every union in pkg that gets an interface: every input form that typedInput
// accepts, and every output form that IsWireDiscriminatableUnionType accepts. Each schema location
// gets its own interface, named after the owner and the property. The forms of one location share
// a spec.
func registerUnions(pkg *schema.Package, fullyTypedUnions bool) *unionRegistry {
	reg := &unionRegistry{
		enabled:    fullyTypedUnions,
		byLocation: map[*schema.Property]map[string]unionForm{},
		byMember:   map[string][]*unionSpec{},
	}
	if !fullyTypedUnions {
		return reg
	}

	// Reserve the names the package already generates.
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
	collect := func(ownerName, ownerToken string, props []*schema.Property, forms []formKey) {
		for _, p := range props {
			propertyName := names.Title(p.Name)
			visitUnionTypes(p.Type, 0, func(union *schema.UnionType, depth int) {
				var typed []formKey
				for _, key := range forms {
					if key.qualifier == outputsQualifier {
						if codegen.IsWireDiscriminatableUnionType(union) {
							typed = append(typed, key)
						}
					} else if typedInput(union) {
						typed = append(typed, key)
					}
				}
				if len(typed) == 0 {
					return
				}
				positions = append(positions, unionPosition{
					ownerName:    ownerName,
					ownerToken:   ownerToken,
					propertyName: propertyName,
					union:        union,
					property:     p,
					forms:        typed,
					depth:        depth,
				})
			})
		}
	}

	if pkg.Provider != nil {
		collect(resourceName(pkg.Provider), pkg.Provider.Token, pkg.Provider.InputProperties, inputArgsForm)
		collect(resourceName(pkg.Provider), pkg.Provider.Token, pkg.Provider.Properties, outputForm)
	}

	resources := append([]*schema.Resource{}, pkg.Resources...)
	sort.Slice(resources, func(i, j int) bool { return resources[i].Token < resources[j].Token })
	for _, r := range resources {
		collect(resourceName(r), r.Token, r.InputProperties, inputArgsForm)
		collect(resourceName(r), r.Token, r.Properties, outputForm)
		if r.StateInputs != nil {
			collect(resourceName(r), r.Token, r.StateInputs.Properties, inputArgsForm)
		}
	}

	functions := append([]*schema.Function{}, pkg.Functions...)
	sort.Slice(functions, func(i, j int) bool { return functions[i].Token < functions[j].Token })
	for _, f := range functions {
		name := tokenToName(f.Token)
		if f.Inputs != nil {
			collect(name, f.Token, f.Inputs.Properties, inputPlainForm)
			if f.Inputs.InputShape != nil {
				collect(name, f.Token, f.Inputs.InputShape.Properties, inputArgsForm)
			}
		}
		if f.Outputs != nil {
			collect(name, f.Token, f.Outputs.Properties, outputForm)
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
		forms := plainObjectForms
		if obj.IsInputShape() {
			forms = inputArgsForm
		}
		collect(tokenToName(obj.Token), obj.Token, obj.Properties, forms)
	}

	// A union held directly by a property names the interface before one buried in a list or a
	// map.
	sort.SliceStable(positions, func(i, j int) bool { return positions[i].depth < positions[j].depth })

	type location struct {
		owner, property, members string
	}
	specs := map[location]*unionSpec{}
	for _, pos := range positions {
		members := unionMembers(pos.union, pkg)
		keys := mapset.NewSet[string]()
		for _, m := range members {
			keys.Add(m.key)
		}
		sorted := keys.ToSlice()
		sort.Strings(sorted)
		loc := location{pos.ownerToken, pos.propertyName, strings.Join(sorted, "|")}

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
				keys:       keys,
				forms:      map[formKey]unionBinding{},
			}
			specs[loc] = spec
			reg.specs = append(reg.specs, spec)
			for _, m := range members {
				if m.object != nil {
					reg.byMember[m.object.Token] = append(reg.byMember[m.object.Token], spec)
				}
			}
		}
		for _, key := range pos.forms {
			if !spec.bound(key) {
				spec.forms[key] = unionBinding{union: pos.union, property: pos.property}
			}
		}
		if _, has := reg.byLocation[pos.property]; !has {
			reg.byLocation[pos.property] = map[string]unionForm{}
		}
		reg.byLocation[pos.property][memberKey(pos.union)] = unionForm{
			spec: spec, inputShape: pos.forms[0].inputShape,
		}
	}

	sort.Slice(reg.specs, func(i, j int) bool { return reg.specs[i].name < reg.specs[j].name })
	for _, spec := range reg.specs {
		for _, other := range reg.specs {
			switch {
			case spec == other:
			case spec.keys.IsProperSubset(other.keys):
				spec.supersets = append(spec.supersets, other)
			case spec.keys.Equal(other.keys):
				spec.equals = append(spec.equals, other)
			}
		}
	}
	for _, specs := range reg.byMember {
		sort.Slice(specs, func(i, j int) bool { return specs[i].name < specs[j].name })
	}
	return reg
}

// unionQueueEntry is one generated form of a union interface.
type unionQueueEntry struct {
	packageName names.FQN
	className   names.Ident
	spec        *unionSpec
	key         formKey
	input       bool
}

// memberType returns the type of member m in the shape this entry generates.
func (e unionQueueEntry) memberType(m unionMember) schema.Type {
	for _, t := range flattenUnion(e.spec.forms[e.key].union) {
		if memberKey(t) == m.key {
			return t
		}
	}
	panic(fmt.Sprintf("union %s has no member %s", e.spec.name, m.key))
}

// withUnionLocation sets the property whose type is being rendered. It returns the function that
// restores the previous location.
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
// form, and queues that interface for generation.
func (mod *modContext) unionInterfaceFQN(spec *unionSpec, key formKey, input bool) names.FQN {
	packageName, err := parsePackageName(mod.tokenToPackage(spec.ownerToken, key.qualifier))
	if err != nil {
		panic(err)
	}
	className := names.Ident(spec.name + unionShapeSuffix(key.inputShape))
	if mod.classQueue != nil {
		mod.classQueue.ensureInterfaceGenerated(unionQueueEntry{
			packageName: packageName,
			className:   className,
			spec:        spec,
			key:         key,
			input:       input,
		})
	}
	return packageName.Dot(className)
}

// unionTypeString renders the interface generated for t, or the collapsed type of a union that
// gets none. It reports false when the fullyTypedUnions option is off.
func (mod *modContext) unionTypeString(t *schema.UnionType, qualifier qualifier, input bool) (TypeShape, bool) {
	form, ok := mod.unions.lookup(mod.unionLocation, t)
	if ok {
		return TypeShape{Type: mod.unionInterfaceFQN(form.spec, formKey{qualifier, form.inputShape}, input)}, true
	}
	if mod.unions != nil && mod.unions.enabled {
		return collapsedUnionType(t), true
	}
	return TypeShape{}, false
}

// unionInterfaces returns the union interfaces obj implements in the form being generated.
func (mod *modContext) unionInterfaces(obj *schema.ObjectType, qualifier qualifier, input bool) []names.FQN {
	key := formKey{qualifier, obj.IsInputShape()}
	var interfaces []names.FQN
	for _, spec := range mod.unions.containing(plainShape(obj).Token) {
		if spec.bound(key) {
			interfaces = append(interfaces, mod.unionInterfaceFQN(spec, key, input))
		}
	}
	return interfaces
}

// unionProperty returns the interface entry behind a property typed as a union, if any.
func (mod *modContext) unionProperty(prop *schema.Property, qualifier qualifier, input bool) (unionQueueEntry, bool) {
	union, ok := codegen.UnwrapType(prop.Type).(*schema.UnionType)
	if !ok {
		return unionQueueEntry{}, false
	}
	form, ok := mod.unions.lookup(prop, union)
	if !ok {
		return unionQueueEntry{}, false
	}
	return unionQueueEntry{spec: form.spec, key: formKey{qualifier, form.inputShape}, input: input}, true
}

// wrappedMembers returns the members of the interface that are held by case classes, with the Java
// type each one wraps in the form being generated.
func (mod *modContext) wrappedMembers(ctx *classFileContext, entry unionQueueEntry) []wrappedMember {
	var wrapped []wrappedMember
	for _, m := range entry.spec.members {
		if m.object != nil {
			continue
		}
		wireType := entry.memberType(m)
		wrapped = append(wrapped, wrappedMember{
			caseName:  m.caseName,
			factory:   m.factory,
			shape:     mod.typeStringRecHelper(ctx, wireType, entry.key.qualifier, entry.input, false, false),
			parameter: factoryParameterType(wireType),
		})
	}
	return wrapped
}

type wrappedMember struct {
	caseName string
	factory  string
	// shape is the boxed Java type the case class holds.
	shape TypeShape
	// parameter is the parameter type of the static factory: a primitive for a scalar member.
	parameter string
}

// factoryParameterType is the parameter type of the of(...) factory for a wrapped member.
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
	defer mod.withUnionLocation(spec.forms[entry.key].property)()

	wrapped := mod.wrappedMembers(ctx, entry)

	var equals []string
	for _, equal := range spec.equals {
		if equal.bound(entry.key) {
			equals = append(equals, ctx.ref(mod.unionInterfaceFQN(equal, entry.key, entry.input)))
		}
	}

	fprintf(w, "\n")
	fprintf(w, "@%s\n", ctx.ref(names.UnionType))
	for _, m := range spec.members {
		if m.object != nil {
			memberType := mod.typeStringForObjectType(
				objectShape(m.object, entry.key.inputShape), entry.key.qualifier, entry.input)
			fprintf(w, "@%s.Case(type = %s.class)\n", ctx.ref(names.UnionType), memberType.ToCode(ctx.imports))
		}
	}
	for _, m := range wrapped {
		refs, tree := m.shape.ToTree(ctx.imports)
		fprintf(w, "@%s.Case(type = %s.%s.class, refs = {%s}, tree = %s)\n",
			ctx.ref(names.UnionType), self, m.caseName, refs, javaStringLiteral(tree))
	}

	var parents []string
	for _, superset := range spec.supersets {
		if superset.bound(entry.key) {
			parents = append(parents, ctx.ref(mod.unionInterfaceFQN(superset, entry.key, entry.input)))
		}
	}
	extends := ""
	if len(parents) > 0 {
		extends = " extends " + strings.Join(parents, ", ")
	}

	fprintf(w, "public interface %s%s {\n", self, extends)

	for _, m := range wrapped {
		parameterType := m.parameter
		if parameterType == "" {
			parameterType = m.shape.ToCode(ctx.imports)
		}
		fprintf(w, "    static %s %s(%s value) {\n", self, m.factory, parameterType)
		fprintf(w, "        return new %s.%s(value);\n", self, m.caseName)
		fprintf(w, "    }\n\n")
	}
	for _, equal := range equals {
		fprintf(w, "    static %s of(%s value) {\n", self, equal)
		fprintf(w, "        return (%s) value;\n", self)
		fprintf(w, "    }\n\n")
	}

	for _, m := range wrapped {
		implements := append(append([]string{self}, equals...), ctx.ref(names.UnionCase))
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
// carries.
func constValueLiteral(v interface{}) string {
	if s, ok := v.(string); ok {
		return javaStringLiteral(s)
	}
	return javaStringLiteral(fmt.Sprint(v))
}
