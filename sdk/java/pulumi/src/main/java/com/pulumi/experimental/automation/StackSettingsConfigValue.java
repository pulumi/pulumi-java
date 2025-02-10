// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

/**
 * Represents a configuration value for a stack.
 */
public class StackSettingsConfigValue {
    private final Object value;
    private final boolean secure;

    public StackSettingsConfigValue(Object value) {
        this(value, false);
    }

    public StackSettingsConfigValue(Object value, boolean isSecure) {
        this.value = value;
        this.secure = isSecure;
    }

    /**
     * Returns the value of the configuration.
     *
     * @return the value
     */
    public Object value() {
        return value;
    }

    /**
     * Returns whether the value is secure.
     *
     * @return true if the value is secure
     */
    public boolean isSecure() {
        return secure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (StackSettingsConfigValue) o;
        return Objects.equals(value, that.value) &&
                secure == that.secure;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, secure);
    }
}
