// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import com.google.gson.annotations.SerializedName;

/**
 * {@link ConfigValue} represents a configuration value.
 */
public class ConfigValue {
    private final String value;
    @SerializedName("secret")
    private final boolean isSecret;

    /**
     * Creates a new configuration value.
     *
     * @param value the value
     */
    public ConfigValue(String value) {
        this(value, false);
    }

    /**
     * Creates a new configuration value.
     *
     * @param value    the value
     * @param isSecret true if the value is secret
     */
    public ConfigValue(String value, boolean isSecret) {
        this.value = value;
        this.isSecret = isSecret;
    }

    /**
     * Gets the configuration value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * True if the configuration value is secret.
     *
     * @return true if the configuration value is secret
     */
    public boolean isSecret() {
        return isSecret;
    }
}
