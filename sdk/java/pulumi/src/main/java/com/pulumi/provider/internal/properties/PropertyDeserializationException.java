package com.pulumi.provider.internal.properties;

/**
 * Exception thrown when a PropertyValue cannot be deserialized to the requested type.
 * Contains details about the failed deserialization including the property path,
 * target type, and original value.
 */
public class PropertyDeserializationException extends RuntimeException {
    private final String[] path;
    private final Class<?> targetType;
    private final PropertyValue value;

    /**
     * Creates a new PropertyDeserializationException.
     *
     * @param message the error message
     * @param path the property path where deserialization failed
     * @param targetType the type that was being deserialized to
     * @param value the PropertyValue that failed to deserialize
     * @param cause the underlying cause, if any
     */
    public PropertyDeserializationException(String message, String[] path, Class<?> targetType, PropertyValue value, Throwable cause) {
        super(String.format("%s (path: %s, targetType: %s)", 
              message, String.join(".", path), targetType.getName()), cause);
        this.path = path.clone();
        this.targetType = targetType;
        this.value = value;
    }

    /**
     * Gets the property path where deserialization failed.
     *
     * @return a copy of the property path array
     */
    public String[] getPath() {
        return path.clone();
    }

    /**
     * Gets the target type that was being deserialized to.
     *
     * @return the target type class
     */
    public Class<?> getTargetType() {
        return targetType;
    }

    /**
     * Gets the PropertyValue that failed to deserialize.
     *
     * @return the original PropertyValue
     */
    public PropertyValue getValue() {
        return value;
    }
} 