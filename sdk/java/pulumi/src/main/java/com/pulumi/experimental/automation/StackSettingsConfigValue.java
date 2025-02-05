// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

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

    public String getValue() {
        return value;
    }

    public boolean isSecure() {
        return isSecure;
    }
}
