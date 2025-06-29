package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Represents a type specification in the provider schema.
 *
 * @see PropertySpec
 * @see ComplexTypeSpec
 */
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

    /**
     * Constructs a new {@code TypeSpec} with the specified type, reference, plain flag, items, and additional properties.
     *
     * @param type the type name
     * @param ref the reference to another type
     * @param plain whether the type is plain
     * @param items the type specification for array items
     * @param additionalProperties the type specification for dictionary values=
     */
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

    /**
     * Creates a {@code TypeSpec} for a built-in type.
     *
     * @param type the built-in type name
     * @return a new {@code TypeSpec} instance for the specified built-in type
     */
    public static TypeSpec ofBuiltin(String type) {
        return ofBuiltin(type, null);
    }

    /**
     * Creates a {@code TypeSpec} for a built-in type with an optional plain flag.
     *
     * @param type the built-in type name
     * @param plain whether the type is plain
     * @return a new {@code TypeSpec} instance for the specified built-in type
     */
    public static TypeSpec ofBuiltin(String type, Boolean plain) {
        return new TypeSpec(type, null, plain, null, null);
    }

    /**
     * Creates a {@code TypeSpec} for a reference to another type.
     *
     * @param ref the reference to another type
     * @return a new {@code TypeSpec} instance referencing the specified type
     */
    public static TypeSpec ofRef(String ref) {
        return ofRef(ref, null);
    }

    /**
     * Creates a {@code TypeSpec} for a reference to another type with an optional plain flag.
     *
     * @param ref the reference to another type
     * @param plain whether the type is plain
     * @return a new {@code TypeSpec} instance referencing the specified type
     */
    public static TypeSpec ofRef(String ref, Boolean plain) {
        return new TypeSpec(null, ref, plain, null, null);
    }

    /**
     * Creates a {@code TypeSpec} for an array type with the specified item type.
     *
     * @param items the type specification for array items
     * @return a new {@code TypeSpec} instance representing an array of the specified item type
     */
    public static TypeSpec ofArray(TypeSpec items) {
        return new TypeSpec("array", null, null, items, null);
    }

    /**
     * Creates a {@code TypeSpec} for a dictionary type with the specified value type.
     *
     * @param additionalProperties the type specification for dictionary values
     * @return a new {@code TypeSpec} instance representing a dictionary with the specified value type
     */
    public static TypeSpec ofDict(TypeSpec additionalProperties) {
        return new TypeSpec("object", null, null, null, additionalProperties);
    }

    /**
     * Gets the type name for this type specification.
     *
     * @return the type name
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the type specification for array items, if this type is an array.
     *
     * @return the item type specification
     */
    public TypeSpec getItems() {
        return items;
    }

    /**
     * Gets the type specification for dictionary values
     *
     * @return the value type specification
     */
    public TypeSpec getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Gets the reference to another type
     *
     * @return the reference string
     */
    public String getRef() {
        return ref;
    }

    /**
     * Gets whether this type is a plain type.
     *
     * @return {@code true} if this is a plain type otherwise {@code false}
     */
    public Boolean getPlain() {
        return plain;
    }

    /**
     * Compares this type specification to another for equality.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
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

    /**
     * Computes the hash code for this type specification.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, ref, plain, items, additionalProperties);
    }

    /**
     * Returns a string representation of this type specification.
     *
     * @return a string describing the type specification
     */
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