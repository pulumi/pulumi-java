package names

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

var mapType FQN = Ident("java").FQN().Dot(Ident("util")).Dot(Ident("Map"))
var treeMapType FQN = Ident("java").FQN().Dot(Ident("util")).Dot(Ident("TreeMap"))
var confoundingMapType FQN = Ident("org").FQN().Dot(Ident("confound")).Dot(Ident("Map"))

func TestFQN(t *testing.T) {
	assert.True(t, mapType.Equal(mapType))
	assert.False(t, mapType.Equal(treeMapType))
	assert.Equal(t, mapType.ToString(), "java.util.Map")
}

func TestImports(t *testing.T) {
	pkg := Ident("io").FQN().Dot(Ident("pulumi")).Dot(Ident("random"))
	cls := Ident("RandomString")
	i, err := NewImports(pkg, cls)
	assert.NoError(t, err)
	assert.True(t, i.Resolve(cls).Equal(pkg.Dot(cls)))
	assert.Equal(t, "Map", i.Ref(mapType))
	assert.Equal(t, "org.confound.Map", i.Ref(confoundingMapType))
	assert.Equal(t, "RandomString", i.Ref(pkg.Dot(cls)))

	assert.Equal(t, `import io.pulumi.random.RandomString;
import java.util.Map;`, i.ImportCode())

	assert.Equal(t, `package io.pulumi.random;`, i.PackageCode())
}
