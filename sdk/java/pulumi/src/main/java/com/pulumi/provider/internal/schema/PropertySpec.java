package com.pulumi.provider.internal.schema;

/**
 * Represents the specification for a property in a provider schema object.
 *
 * @see TypeSpec
 * @see ComplexTypeSpec
 */
public class PropertySpec extends TypeSpec {

    /**
     * Constructs a new {@code PropertySpec} with the specified type, reference, and additional type details.
     *
     * @param type                 the type name
     * @param ref                  the reference to another type
     * @param plain                whether the property is a plain type
     * @param items                the item type if this property is an array
     * @param additionalProperties the type of values if this property is a dictionary
     */
    public PropertySpec(String type, String ref, Boolean plain, TypeSpec items, TypeSpec additionalProperties) {
        super(type, ref, plain, items, additionalProperties);
    }

    /**
     * Creates a {@code PropertySpec} for a built-in type.
     *
     * @param type the built-in type name
     * @return a new {@code PropertySpec} instance for the specified built-in type
     */
    public static PropertySpec ofBuiltin(String type) {
        return ofBuiltin(type, null);
    }

    /**
     * Creates a {@code PropertySpec} for a built-in type with an optional plain flag.
     *
     * @param type  the built-in type name
     * @param plain whether the property is a plain type (may be {@code null})
     * @return a new {@code PropertySpec} instance for the specified built-in type
     */
    public static PropertySpec ofBuiltin(String type, Boolean plain) {
        return new PropertySpec(type, null, plain, null, null);
    }

    /**
     * Creates a {@code PropertySpec} for a reference to another type.
     *
     * @param ref the reference to another type
     * @return a new {@code PropertySpec} instance referencing the specified type
     */
    public static PropertySpec ofRef(String ref) {
        return ofRef(ref, null);
    }

    /**
     * Creates a {@code PropertySpec} for a reference to another type with an optional plain flag.
     *
     * @param ref   the reference to another type
     * @param plain whether the property is a plain type (may be {@code null})
     * @return a new {@code PropertySpec} instance referencing the specified type
     */
    public static PropertySpec ofRef(String ref, Boolean plain) {
        return new PropertySpec(null, ref, plain, null, null);
    }

    /**
     * Creates a {@code PropertySpec} for an array property with the specified item type.
     *
     * @param items the type specification for array items
     * @return a new {@code PropertySpec} instance representing an array of the specified item type
     */
    public static PropertySpec ofArray(TypeSpec items) {
        return new PropertySpec("array", null, null, items, null);
    }

    /**
     * Creates a {@code PropertySpec} for a dictionary property with the specified value type.
     *
     * @param additionalProperties the type specification for dictionary values
     * @return a new {@code PropertySpec} instance representing a dictionary with the specified value type
     */
    public static PropertySpec ofDict(TypeSpec additionalProperties) {
        return new PropertySpec("object", null, null, null, additionalProperties);
    }

    /**
     * Compares this property specification to another for equality.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    /**
     * Computes the hash code for this property specification.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this property specification.
     *
     * @return a string describing the property specification
     */
    @Override
    public String toString() {
        return "PropertySpec" + super.toString();
    }
} 