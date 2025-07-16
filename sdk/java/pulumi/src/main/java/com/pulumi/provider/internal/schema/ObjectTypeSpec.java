package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents the specification for an object type in provider schema.
 *
 * @see PropertySpec
 * @see ComplexTypeSpec
 */
public class ObjectTypeSpec {
    @SerializedName("type")
    private String type;

    @SerializedName("properties")
    private Map<String, PropertySpec> properties;

    @SerializedName("required")
    private Set<String> required;
    
    /**
     * Constructs a new {@code ObjectTypeSpec} with the specified type, properties, and required fields.
     *
     * @param type the type name (typically {@code "object"})
     * @param properties a map of property names to their specifications
     * @param required a set of required property names
     */
    protected ObjectTypeSpec(
        String type,
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        this.type = type;
        this.properties = properties;
        this.required = new TreeSet<>(required);
    }

    /**
     * Gets the type name of this object specification.
     *
     * @return the type name
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the map of property names to their specifications for this object type.
     *
     * @return a map of property names to {@link PropertySpec} instances, or {@code null} if not defined
     */
    public Map<String, PropertySpec> getProperties() {
        return properties;
    }

    /**
     * Gets the set of required property names for this object type.
     *
     * @return a set of required property names, or {@code null} if not defined
     */
    public Set<String> getRequired() {
        return required;
    }

    /**
     * Compares this object type specification to another for equality.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectTypeSpec that = (ObjectTypeSpec) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(properties, that.properties) &&
               Objects.equals(required, that.required);
    }

    /**
     * Computes the hash code for this object type specification.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, properties, required);
    }

    /**
     * Returns a string representation of this object type specification.
     *
     * @return a string describing the object type specification
     */
    @Override
    public String toString() {
        return "ObjectTypeSpec{" +
               "type='" + type + '\'' +
               ", properties=" + properties +
               ", required=" + required +
               '}';
    }
} 