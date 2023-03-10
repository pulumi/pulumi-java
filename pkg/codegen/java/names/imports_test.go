package names

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

var (
	mapType            = Ident("java").FQN().Dot(Ident("util")).Dot(Ident("Map"))
	treeMapType        = Ident("java").FQN().Dot(Ident("util")).Dot(Ident("TreeMap"))
	confoundingMapType = Ident("org").FQN().Dot(Ident("confound")).Dot(Ident("Map"))
)

func TestFQN(t *testing.T) {
	assert.True(t, mapType.Equal(mapType))
	assert.False(t, mapType.Equal(treeMapType))
	assert.Equal(t, mapType.String(), "java.util.Map")
}

func TestImports(t *testing.T) {
	pkg := Ident("com").FQN().Dot(Ident("pulumi")).Dot(Ident("random"))
	cls := Ident("RandomString")
	i := NewImports(pkg, cls)
	assert.True(t, i.Resolve(cls).Equal(pkg.Dot(cls)))
	assert.Equal(t, "Map", i.Ref(mapType))
	assert.Equal(t, "org.confound.Map", i.Ref(confoundingMapType))
	assert.Equal(t, "RandomString", i.Ref(pkg.Dot(cls)))

	assert.Equal(t, `import java.util.Map;`, i.ImportCode())

	assert.Equal(t, `package com.pulumi.random;`, i.PackageCode())
}
