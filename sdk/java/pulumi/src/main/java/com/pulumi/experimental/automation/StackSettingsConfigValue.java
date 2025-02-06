// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

/**
 * Represents a configuration value for a stack.
 */
public class StackSettingsConfigValue {
    private final String value;
    private final boolean isSecure;

    public StackSettingsConfigValue(String value) {
        this(value, false);
    }

    public StackSettingsConfigValue(String value, boolean isSecure) {
        this.value = value;
        this.isSecure = isSecure;
    }

    /**
     * Returns the value of the configuration.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns whether the value is secure.
     *
     * @return true if the value is secure
     */
    public boolean isSecure() {
        return isSecure;
    }
}
