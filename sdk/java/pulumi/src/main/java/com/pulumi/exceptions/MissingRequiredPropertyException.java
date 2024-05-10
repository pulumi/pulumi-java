package com.pulumi.exceptions;

/**
 * MissingRequiredPropertyException is thrown when a required input property is missing from a resource args builder
 */
public class MissingRequiredPropertyException extends RuntimeException {
    public MissingRequiredPropertyException(String builderName, String propertyName) {
        super("Missing required property '" + propertyName + " when constructing '" + builderName + "'");
    }
}
