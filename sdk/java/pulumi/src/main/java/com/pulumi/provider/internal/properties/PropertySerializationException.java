package com.pulumi.provider.internal.properties;

/**
 * Exception thrown when a property value cannot be serialized.
 */
public class PropertySerializationException extends RuntimeException {
    private final String[] path;
    private final Class<?> targetType;

    public PropertySerializationException(String message, String[] path, Class<?> targetType, Throwable cause) {
        super(String.format("%s (path: %s, type: %s)", 
            message, 
            String.join("/", path), 
            targetType.getName()
        ), cause);
        this.path = path.clone();
        this.targetType = targetType;
    }

    public String[] getPath() {
        return path.clone();
    }

    public Class<?> getTargetType() {
        return targetType;
    }
}
