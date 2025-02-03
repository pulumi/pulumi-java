// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * A placeholder config value for a project template.
 */
public class ProjectTemplateConfigValue {
    @Nullable
    private String description;
    @SerializedName("default")
    @Nullable
    private String defaultValue;
    private boolean secret;

    /**
     * Creates a new {@link ProjectTemplateConfigValue}.
     *
     * @param description the description of the config value
     */
    public ProjectTemplateConfigValue(
            @Nullable String description) {
        this(description, null);
    }

    /**
     * Creates a new {@link ProjectTemplateConfigValue}.
     *
     * @param description  the description of the config value
     * @param defaultValue the default value
     */
    public ProjectTemplateConfigValue(
            @Nullable String description,
            @Nullable String defaultValue) {
        this(description, defaultValue, false);
    }

    /**
     * Creates a new {@link ProjectTemplateConfigValue}.
     *
     * @param description  the description of the config value
     * @param defaultValue the default value
     * @param secret       whether the value should be treated as secret
     */
    public ProjectTemplateConfigValue(
            @Nullable String description,
            @Nullable String defaultValue,
            boolean secret) {
        this.description = description;
        this.defaultValue = defaultValue;
        this.secret = secret;
    }

    /**
     * The description of the config value.
     *
     * @return the description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * The default value of the config value.
     *
     * @return the default value
     */
    @Nullable
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Whether the value should be treated as secret.
     *
     * @return whether the value should be treated as secret
     */
    @Nullable
    public Boolean getSecret() {
        return secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectTemplateConfigValue) o;
        return secret == that.secret
                && Objects.equals(description, that.description)
                && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, defaultValue, secret);
    }
}
