// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public final class OutputValue {
    private final Object value;
    private final boolean isSecret;

    OutputValue(Object value, boolean isSecret) {
        this.value = value;
        this.isSecret = isSecret;
    }

    public Object getValue() {
        return value;
    }

    public boolean isSecret() {
        return isSecret;
    }
}
