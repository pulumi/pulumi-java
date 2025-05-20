// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * Represents a stack output value.
 */
public final class OutputValue {
    private final Object value;
    private final boolean isSecret;

    OutputValue(Object value, boolean isSecret) {
        this.value = value;
        this.isSecret = isSecret;
    }

    /**
     * Returns the value of the output.
     *
     * @return the value
     */
    public Object value() {
        return value;
    }

    /**
     * Returns whether the output is a secret.
     *
     * @return true if the output is a secret
     */
    public boolean isSecret() {
        return isSecret;
    }
}
