// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * The project configuration type.
 */
public class ProjectConfigType {
    @Nullable
    private final String type;
    @Nullable
    private final String description;
    @Nullable
    private final ProjectConfigItemsType items;
    @Nullable
    private final Object defaultValue;
    @Nullable
    private final Object value;
    private final boolean secret;

    private ProjectConfigType(Builder builder) {
        type = builder.type;
        description = builder.description;
        items = builder.items;
        defaultValue = builder.defaultValue;
        value = builder.value;
        secret = builder.secret;
    }

    /**
     * Returns a new builder for {@link ProjectConfigType}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type of the configuration.
     *
     * @return the type of the configuration
     */
    @Nullable
    public String getType() {
        return type;
    }

    /**
     * The description of the configuration.
     *
     * @return the description of the configuration
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * The configuration items type.
     *
     * @return the configuration items
     */
    @Nullable
    public ProjectConfigItemsType getItems() {
        return items;
    }

    /**
     * The default value of the configuration.
     *
     * @return the default value of the configuration
     */
    @Nullable
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * The value of the configuration.
     *
     * @return the value of the configuration
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Whether the configuration is secret.
     *
     * @return whether the configuration is secret
     */
    public boolean isSecret() {
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
        var that = (ProjectConfigType) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(description, that.description) &&
                Objects.equals(items, that.items) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(value, that.value) &&
                secret == that.secret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description, items, defaultValue, value, secret);
    }

    /**
     * Builder for {@link ProjectConfigType}.
     */
    public static class Builder {
        @Nullable
        private String type;
        @Nullable
        private String description;
        @Nullable
        private ProjectConfigItemsType items;
        @Nullable
        private Object defaultValue;
        @Nullable
        private Object value;
        private boolean secret;

        private Builder() {
        }

        /**
         * The type of the configuration.
         *
         * @param type the type of the configuration
         * @return the builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * The description of the configuration.
         *
         * @param description the description of the configuration
         * @return the builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * The configuration items type.
         *
         * @param items the configuration items type
         * @return the builder
         */
        public Builder items(ProjectConfigItemsType items) {
            this.items = items;
            return this;
        }

        /**
         * The default value of the configuration.
         *
         * @param defaultValue the default value of the configuration
         * @return the builder
         */
        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * The value of the configuration.
         *
         * @param value the value of the configuration
         * @return the builder
         */
        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        /**
         * Whether the configuration is secret.
         *
         * @param secret whether the configuration is secret
         * @return the builder
         */
        public Builder secret(boolean secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Builds the {@link ProjectConfigType}.
         *
         * @return the project configuration type
         */
        public ProjectConfigType build() {
            return new ProjectConfigType(this);
        }
    }
}
