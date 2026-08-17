// Copyright 2026, Pulumi Corporation.  All rights reserved.

package java

import (
	"fmt"
	"net/url"
	"slices"
	"sort"
	"strings"

	"github.com/pulumi/pulumi/pkg/v3/codegen"
	"github.com/pulumi/pulumi/pkg/v3/codegen/schema"

	"github.com/pulumi/pulumi-java/pkg/codegen/java/names"
)

const typeRefPrefix = "#/types/"

// discriminatedUnion is a schema union that carries a discriminator with a non-empty mapping. Such
// a union is generated as a Java interface that each member class implements, rather than degrading
// to java.lang.Object or to Either<L, R>. Member count never takes part in the decision, so adding
// a member to a union does not change the shape of the code generated for it.
type discriminatedUnion struct {
	// name is the interface name without the shape suffix that typeName applies to member classes.
	name string
	// propertyName is the schema discriminator property name. It is emitted verbatim.
	propertyName string
	// tags are the discriminator mapping keys in sorted order.
	tags []string
	// members maps each tag to the plain shape of the object type that tag selects.
	members map[string]*schema.ObjectType
	// supersets are the registered unions whose mappings strictly contain this one's, so that a
	// value of this union assigns into a slot typed as one of them.
	supersets []*discriminatedUnion
	// mod is the module the interface is generated into: the module its members live in.
	mod string
}

// directSupersets returns the supersets that are not themselves supersets of another superset, so
// the generated interface declaration lists each parent once.
func (du *discriminatedUnion) directSupersets() []*discriminatedUnion {
	var direct []*discriminatedUnion
	for _, candidate := range du.supersets {
		redundant := false
		for _, other := range du.supersets {
			if other != candidate && isTagSubset(other.tags, candidate.tags) {
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

// unionRegistry names the discriminated unions of a package. It is shared by every module of that
// package so that a union referenced from several modules resolves to a single interface.
type unionRegistry struct {
	byKey    map[string]*discriminatedUnion
	byMember map[string][]*discriminatedUnion
}

func (r *unionRegistry) lookup(t *schema.UnionType) (*discriminatedUnion, bool) {
	if r == nil {
		return nil, false
	}
	key, ok := discriminatedUnionKey(t)
	if !ok {
		return nil, false
	}
	du, ok := r.byKey[key]
	return du, ok
}

func (r *unionRegistry) containing(token string) []*discriminatedUnion {
	if r == nil {
		return nil
	}
	return r.byMember[token]
}

// unionMemberObject unwraps a union element down to the object type it names.
func unionMemberObject(t schema.Type) (*schema.ObjectType, bool) {
	obj, ok := codegen.UnwrapType(t).(*schema.ObjectType)
	return obj, ok
}

// plainShape returns the non-input shape of an object type.
func plainShape(obj *schema.ObjectType) *schema.ObjectType {
	if obj.PlainShape != nil {
		return obj.PlainShape
	}
	return obj
}

// discriminatedUnionMembers resolves the union's discriminator mapping to the object types it
// selects, keyed by tag. It reports false when the union is not a well-formed discriminated union
// of object types, in which case the union is generated the way it always was.
func discriminatedUnionMembers(t *schema.UnionType) (map[string]*schema.ObjectType, bool) {
	if t.Discriminator == "" || len(t.Mapping) == 0 {
		return nil, false
	}

	byToken := map[string]*schema.ObjectType{}
	for _, e := range t.ElementTypes {
		obj, ok := unionMemberObject(e)
		if !ok {
			return nil, false
		}
		byToken[plainShape(obj).Token] = plainShape(obj)
	}

	members := map[string]*schema.ObjectType{}
	for tag, ref := range t.Mapping {
		token, found := strings.CutPrefix(ref, typeRefPrefix)
		if !found {
			return nil, false
		}
		obj, ok := byToken[token]
		if !ok {
			// Refs may percent-encode characters that are legal in a token.
			unescaped, err := url.PathUnescape(token)
			if err != nil {
				return nil, false
			}
			if obj, ok = byToken[unescaped]; !ok {
				return nil, false
			}
		}
		members[tag] = obj
	}

	// Every element must be reachable through the mapping, otherwise the tag dispatch the generated
	// annotations describe would be incomplete. Compare the set of covered tokens rather than the
	// counts: two tags may name the same member, which keeps the counts equal while leaving
	// another member uncovered.
	covered := map[string]bool{}
	for _, obj := range members {
		covered[obj.Token] = true
	}
	if len(covered) != len(byToken) {
		return nil, false
	}
	return members, true
}

// javaStringLiteral renders s as a Java string literal. Discriminator names and tags come from
// the schema, so they may contain characters that would otherwise terminate or escape the literal.
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

// discriminatedUnionKey identifies a union by its discriminator and tag-to-member mapping, so that
// the same union reached through different properties, or through its input and output shapes,
// resolves to one generated interface.
func discriminatedUnionKey(t *schema.UnionType) (string, bool) {
	members, ok := discriminatedUnionMembers(t)
	if !ok {
		return "", false
	}

	var b strings.Builder
	b.WriteString(t.Discriminator)
	for _, tag := range sortedTags(members) {
		fmt.Fprintf(&b, "|%s=%s", tag, members[tag].Token)
	}
	return b.String(), true
}

func sortedTags(members map[string]*schema.ObjectType) []string {
	tags := make([]string, 0, len(members))
	for tag := range members {
		tags = append(tags, tag)
	}
	sort.Strings(tags)
	return tags
}

func isTagSubset(sub, super []string) bool {
	if len(sub) >= len(super) {
		return false
	}
	for _, tag := range sub {
		if !slices.Contains(super, tag) {
			return false
		}
	}
	return true
}

// isUnionSubset reports whether every case of sub is also a case of super, mapped to the same type.
func isUnionSubset(sub, super *discriminatedUnion) bool {
	if sub.propertyName != super.propertyName || sub.mod != super.mod || !isTagSubset(sub.tags, super.tags) {
		return false
	}
	for tag, member := range sub.members {
		if super.members[tag] != member {
			return false
		}
	}
	return true
}

// unionShapeSuffix is the suffix modContext.typeName appends to a member's class name for the shape
// being generated. The generated interface carries the same suffix so it sits alongside the classes
// that implement it.
func unionShapeSuffix(obj *schema.ObjectType) string {
	if obj.IsInputShape() {
		return "Args"
	}
	return ""
}

// unionInterfaceFQN is the fully qualified name of the interface generated for du in the shape that
// member is generated in, and queues that interface for generation. The interface lives in the same
// Java package as its members.
func (mod *modContext) unionInterfaceFQN(
	du *discriminatedUnion, member *schema.ObjectType, qualifier qualifier, input bool,
) (names.FQN, bool) {
	packageName, err := parsePackageName(mod.tokenToPackage(plainShape(member).Token, qualifier))
	if err != nil {
		return names.FQN{}, false
	}
	className := names.Ident(du.name + unionShapeSuffix(member))

	if mod.classQueue != nil {
		mod.classQueue.ensureInterfaceGenerated(unionQueueEntry{
			packageName: packageName,
			className:   className,
			union:       du,
			qualifier:   qualifier,
			input:       input,
			inputShape:  member.IsInputShape(),
		})
	}
	return packageName.Dot(className), true
}

// discriminatedUnionTypeString renders the interface generated for t, if t qualifies for one.
func (mod *modContext) discriminatedUnionTypeString(
	t *schema.UnionType, qualifier qualifier, input bool,
) (TypeShape, bool) {
	du, ok := mod.unions.lookup(t)
	if !ok {
		return TypeShape{}, false
	}

	member, ok := unionMemberObject(t.ElementTypes[0])
	if !ok {
		return TypeShape{}, false
	}

	fqn, ok := mod.unionInterfaceFQN(du, member, qualifier, input)
	if !ok {
		return TypeShape{}, false
	}
	return TypeShape{Type: fqn}, true
}

// unionInterfaces returns the discriminated union interfaces obj implements in the shape being
// generated. Only the narrowest applicable interfaces are listed, because the wider ones are
// already reachable through them.
func (mod *modContext) unionInterfaces(
	obj *schema.ObjectType, qualifier qualifier, input bool,
) []names.FQN {
	matches := mod.unions.containing(plainShape(obj).Token)

	var interfaces []names.FQN
	for _, du := range matches {
		redundant := false
		for _, other := range matches {
			if other != du && slices.Contains(other.supersets, du) {
				redundant = true
				break
			}
		}
		if redundant {
			continue
		}
		if fqn, ok := mod.unionInterfaceFQN(du, obj, qualifier, input); ok {
			interfaces = append(interfaces, fqn)
		}
	}
	return interfaces
}

// genDiscriminatedUnionInterface emits the interface that stands for a discriminated union. It
// carries the annotations the Pulumi runtime dispatches on, and is otherwise empty: the members
// share no properties beyond the discriminator.
func (mod *modContext) genDiscriminatedUnionInterface(ctx *classFileContext, entry unionQueueEntry) error {
	w := ctx.writer
	du := entry.union

	fprintf(w, "\n")
	fprintf(w, "@%s(%s)\n", ctx.ref(names.DiscriminatedUnion), javaStringLiteral(du.propertyName))
	for _, tag := range du.tags {
		member := entry.shapeOf(du.members[tag])
		memberType := mod.typeStringForObjectType(member, entry.qualifier, entry.input)
		fprintf(w, "@%s.Case(tag = %s, type = %s.class)\n",
			ctx.ref(names.DiscriminatedUnion), javaStringLiteral(tag), memberType.ToCode(ctx.imports))
	}

	var extends string
	if supersets := du.directSupersets(); len(supersets) > 0 {
		parents := make([]string, 0, len(supersets))
		for _, superset := range supersets {
			member := entry.shapeOf(superset.members[superset.tags[0]])
			fqn, ok := mod.unionInterfaceFQN(superset, member, entry.qualifier, entry.input)
			if !ok {
				continue
			}
			parents = append(parents, ctx.ref(fqn))
		}
		if len(parents) > 0 {
			extends = " extends " + strings.Join(parents, ", ")
		}
	}

	fprintf(w, "public interface %s%s {\n", entry.className, extends)
	fprintf(w, "}\n")
	return nil
}

// visitUnionTypes calls visitor for every union held by t, along with the number of collections the
// union is nested in. It deliberately does not descend into object types: a union nested in an
// object belongs to that object's own property, and object types are visited in their own right.
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
	}
}

// unionPosition is a schema location that holds a discriminated union. It is the source of the
// generated interface name.
type unionPosition struct {
	owner    string
	property string
	union    *schema.UnionType
	key      string
	depth    int
	seq      int
}

// registerDiscriminatedUnions names every qualifying discriminated union in pkg. A union is named
// after the first schema position that holds it - the owning resource, function or type, plus the
// property name - which keeps the name stable when members are added to the union, and unique
// because positions are unique. Positions are visited in a fixed order so regenerating a package
// produces the same names.
//
// The registry stays empty unless the package sets the fullyTypedUnions option, because typing a
// union that used to be java.lang.Object or Either<L, R> breaks the callers of the generated SDK.
func registerDiscriminatedUnions(pkg *schema.Package, fullyTypedUnions bool) *unionRegistry {
	reg := &unionRegistry{
		byKey:    map[string]*discriminatedUnion{},
		byMember: map[string][]*discriminatedUnion{},
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
	}

	var positions []unionPosition
	collect := func(owner string, props []*schema.Property) {
		for _, p := range props {
			propertyName := names.Title(p.Name)
			visitUnionTypes(p.Type, 0, func(union *schema.UnionType, depth int) {
				key, ok := discriminatedUnionKey(union)
				if !ok {
					return
				}
				positions = append(positions, unionPosition{
					owner:    owner,
					property: propertyName,
					union:    union,
					key:      key,
					depth:    depth,
					seq:      len(positions),
				})
			})
		}
	}

	if pkg.Provider != nil {
		collect(resourceName(pkg.Provider), pkg.Provider.InputProperties)
		collect(resourceName(pkg.Provider), pkg.Provider.Properties)
	}

	resources := slices.Clone(pkg.Resources)
	sort.Slice(resources, func(i, j int) bool { return resources[i].Token < resources[j].Token })
	for _, r := range resources {
		collect(resourceName(r), r.InputProperties)
		collect(resourceName(r), r.Properties)
		if r.StateInputs != nil {
			collect(resourceName(r), r.StateInputs.Properties)
		}
	}

	functions := slices.Clone(pkg.Functions)
	sort.Slice(functions, func(i, j int) bool { return functions[i].Token < functions[j].Token })
	for _, f := range functions {
		name := tokenToName(f.Token)
		if f.Inputs != nil {
			collect(name, f.Inputs.Properties)
		}
		if f.Outputs != nil {
			collect(name, f.Outputs.Properties)
		}
	}

	var objects []*schema.ObjectType
	for _, t := range pkg.Types {
		if obj, ok := t.(*schema.ObjectType); ok {
			objects = append(objects, obj)
		}
	}
	sort.Slice(objects, func(i, j int) bool {
		if objects[i].Token != objects[j].Token {
			return objects[i].Token < objects[j].Token
		}
		return !objects[i].IsInputShape()
	})
	for _, obj := range objects {
		collect(tokenToName(obj.Token), obj.Properties)
	}

	// A union held directly by a property names the interface better than one buried in a list or a
	// map, so shallower positions win.
	sort.SliceStable(positions, func(i, j int) bool {
		if positions[i].depth != positions[j].depth {
			return positions[i].depth < positions[j].depth
		}
		return positions[i].seq < positions[j].seq
	})

	for _, pos := range positions {
		if _, ok := reg.byKey[pos.key]; ok {
			continue
		}
		members, _ := discriminatedUnionMembers(pos.union)
		tags := sortedTags(members)

		// Every member must live in this package and share a module, otherwise there is no single
		// Java package to generate the interface into.
		mod := pkg.TokenToModule(members[tags[0]].Token)
		usable := true
		for _, tag := range tags {
			member := members[tag]
			if !codegen.PkgEquals(member.PackageReference, pkg.Reference()) ||
				pkg.TokenToModule(member.Token) != mod {
				usable = false
				break
			}
		}
		if !usable {
			continue
		}

		candidate := "I" + pos.owner + pos.property
		name := candidate
		for i := 2; !reserve(mod, names.Ident(name).String()); i++ {
			name = fmt.Sprintf("%s%d", candidate, i)
		}

		du := &discriminatedUnion{
			name:         name,
			propertyName: pos.union.Discriminator,
			tags:         tags,
			members:      members,
			mod:          mod,
		}
		reg.byKey[pos.key] = du
		for _, tag := range tags {
			token := members[tag].Token
			reg.byMember[token] = append(reg.byMember[token], du)
		}
	}

	// A union whose mapping is contained in another's must be assignable to it, which Java models by
	// having the narrower interface extend the wider one.
	all := make([]*discriminatedUnion, 0, len(reg.byKey))
	for _, du := range reg.byKey {
		all = append(all, du)
	}
	sort.Slice(all, func(i, j int) bool { return all[i].name < all[j].name })
	for _, du := range all {
		for _, other := range all {
			if isUnionSubset(du, other) {
				du.supersets = append(du.supersets, other)
			}
		}
	}
	for _, dus := range reg.byMember {
		sort.Slice(dus, func(i, j int) bool { return dus[i].name < dus[j].name })
	}

	return reg
}
