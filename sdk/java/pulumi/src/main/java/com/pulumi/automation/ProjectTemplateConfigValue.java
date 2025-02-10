// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Objects;

import javax.annotation.Nullable;

import com.pulumi.automation.serialization.internal.SkipIfFalse;

/**
 * A placeholder config value for a project template.
 */
public class ProjectTemplateConfigValue {
    @Nullable
    private String description;
    // We can't use `default` as a field name because it's a reserved keyword in
    // Java. We use `default_` instead and our serializer automatically strips the
    // underscore.
    @Nullable
    private String default_;
    @SkipIfFalse
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
     * @param default_ the default value
     */
    public ProjectTemplateConfigValue(
            @Nullable String description,
            @Nullable String default_) {
        this(description, default_, false);
    }

    /**
     * Creates a new {@link ProjectTemplateConfigValue}.
     *
     * @param description  the description of the config value
     * @param default_ the default value
     * @param secret       whether the value should be treated as secret
     */
    public ProjectTemplateConfigValue(
            @Nullable String description,
            @Nullable String default_,
            boolean secret) {
        this.description = description;
        this.default_ = default_;
        this.secret = secret;
    }

    /**
     * The description of the config value.
     *
     * @return the description
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * The default value of the config value.
     *
     * @return the default value
     */
    @Nullable
    public String default_() {
        return default_;
    }

    /**
     * Whether the value should be treated as secret.
     *
     * @return whether the value should be treated as secret
     */
    @Nullable
    public Boolean secret() {
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
                && Objects.equals(default_, that.default_);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, default_, secret);
    }
}
