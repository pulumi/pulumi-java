// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public class ConfigValue {
    private final String value;
    private final boolean isSecret;

    public ConfigValue(String value) {
        this(value, false);
    }

    public ConfigValue(String value, boolean isSecret) {
        this.value = value;
        this.isSecret = isSecret;
    }

    public String getValue() {
        return value;
    }

    public boolean isSecret() {
        return isSecret;
    }
}
