package com.pulumi.provider.internal.schema;

public class PropertySpec extends TypeSpec {
    public PropertySpec(String type, String ref, Boolean plain, TypeSpec items, TypeSpec additionalProperties) {
        super(type, ref, plain, items, additionalProperties);
    }

    public static PropertySpec ofBuiltin(String type) {
        return ofBuiltin(type, null);
    }

    public static PropertySpec ofBuiltin(String type, Boolean plain) {
        return new PropertySpec(type, null, plain, null, null);
    }

    public static PropertySpec ofRef(String ref) {
        return ofRef(ref, null);
    }

    public static PropertySpec ofRef(String ref, Boolean plain) {
        return new PropertySpec(null, ref, plain, null, null);
    }

    public static PropertySpec ofArray(TypeSpec items) {
        return new PropertySpec("array", null, null, items, null);
    }

    public static PropertySpec ofDict(TypeSpec additionalProperties) {
        return new PropertySpec("object", null, null, null, additionalProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "PropertySpec" + super.toString();
    }
} 