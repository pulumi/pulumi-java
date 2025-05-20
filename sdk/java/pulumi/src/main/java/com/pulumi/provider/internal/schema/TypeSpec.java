package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class TypeSpec {
    @SerializedName("type")
    private String type;

    @SerializedName("items")
    private TypeSpec items;

    @SerializedName("additionalProperties")
    private TypeSpec additionalProperties;

    @SerializedName("$ref")
    private String ref;

    @SerializedName("plain")
    private Boolean plain;

    public TypeSpec(String type, String ref, Boolean plain, TypeSpec items, TypeSpec additionalProperties) {
        this.type = type;
        this.ref = ref;
        if (Boolean.TRUE.equals(plain)) {
            // Don't set plain to false, instead omit it from the JSON.
            this.plain = plain;
        }
        this.items = items;
        this.additionalProperties = additionalProperties;
    }

    public static TypeSpec ofBuiltin(String type) {
        return ofBuiltin(type, null);
    }

    public static TypeSpec ofBuiltin(String type, Boolean plain) {
        return new TypeSpec(type, null, plain, null, null);
    }

    public static TypeSpec ofRef(String ref) {
        return ofRef(ref, null);
    }

    public static TypeSpec ofRef(String ref, Boolean plain) {
        return new TypeSpec(null, ref, plain, null, null);
    }

    public static TypeSpec ofArray(TypeSpec items) {
        return new TypeSpec("array", null, null, items, null);
    }

    public static TypeSpec ofDict(TypeSpec additionalProperties) {
        return new TypeSpec("object", null, null, null, additionalProperties);
    }

    public String getType() {
        return type;
    }

    public TypeSpec getItems() {
        return items;
    }

    public TypeSpec getAdditionalProperties() {
        return additionalProperties;
    }

    public String getRef() {
        return ref;
    }

    public Boolean getPlain() {
        return plain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeSpec that = (TypeSpec) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(ref, that.ref) &&
               Objects.equals(plain, that.plain) &&
               Objects.equals(items, that.items) &&
               Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ref, plain, items, additionalProperties);
    }

    @Override
    public String toString() {
        return "TypeSpec{" +
               "type='" + type + '\'' +
               ", ref='" + ref + '\'' +
               ", plain=" + plain +
               ", items=" + items +
               ", additionalProperties=" + additionalProperties +
               '}';
    }
} 