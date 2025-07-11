package com.pulumi.provider.internal.schema;

/**
 * Defines constant values for the built-in types supported by the provider schema.
 * @see com.pulumi.provider.internal.schema.TypeSpec
 */
public final class BuiltinTypeSpec {
    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String BOOLEAN = "boolean";
    public static final String OBJECT = "object";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private BuiltinTypeSpec() {} // Prevent instantiation
} 