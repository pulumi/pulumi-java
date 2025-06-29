package com.pulumi.provider.internal.schema;


import java.util.Map;
import java.util.Set;

/**
 * Represents a complex type specification within the Pulumi provider schema.
 * @see ObjectTypeSpec
 * @see PropertySpec
 */
public class ComplexTypeSpec extends ObjectTypeSpec {

    /**
     * Constructs a new {@code ComplexTypeSpec} with the specified type, properties, and required fields.
     *
     * @param type the type name
     * @param properties a map of property names to their specifications
     * @param required a set of required property names
     */
    private ComplexTypeSpec(
        String type,
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        super(type, properties, required);
    }

    /**
     * Creates a {@code ComplexTypeSpec} with the given type and no properties or required fields.
     *
     * @param type the type name
     * @return a new {@code ComplexTypeSpec} instance with the specified type
     */
    public static ComplexTypeSpec of(String type) {
        return new ComplexTypeSpec(type, null, null);
    }

    /**
     * Creates a {@code ComplexTypeSpec} for an object type with the specified properties and required fields.
     *
     * @param properties a map of property names to their specifications
     * @param required a set of required property names
     * @return a new {@code ComplexTypeSpec} instance
     */
    public static ComplexTypeSpec ofObject(
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        return new ComplexTypeSpec("object", properties, required);
    }
} 