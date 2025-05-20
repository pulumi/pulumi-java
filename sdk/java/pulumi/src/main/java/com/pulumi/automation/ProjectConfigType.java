// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Objects;

import javax.annotation.Nullable;

import com.pulumi.automation.serialization.internal.SkipIfFalse;

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
    // We can't use `default` as a field name because it's a reserved keyword in
    // Java. We use `default_` instead and our serializer automatically strips the
    // underscore.
    @Nullable
    private final Object default_;
    @Nullable
    private final Object value;
    @SkipIfFalse
    private final boolean secret;

    private ProjectConfigType(Builder builder) {
        type = builder.type;
        description = builder.description;
        items = builder.items;
        default_ = builder.default_;
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
    public String type() {
        return type;
    }

    /**
     * The description of the configuration.
     *
     * @return the description of the configuration
     */
    @Nullable
    public String description() {
        return description;
    }

    /**
     * The configuration items type.
     *
     * @return the configuration items
     */
    @Nullable
    public ProjectConfigItemsType items() {
        return items;
    }

    /**
     * The default value of the configuration.
     *
     * @return the default value of the configuration
     */
    @Nullable
    public Object default_() {
        return default_;
    }

    /**
     * The value of the configuration.
     *
     * @return the value of the configuration
     */
    @Nullable
    public Object value() {
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
                Objects.equals(default_, that.default_) &&
                Objects.equals(value, that.value) &&
                secret == that.secret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description, items, default_, value, secret);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .type(type)
                .description(description)
                .items(items)
                .default_(default_)
                .value(value)
                .secret(secret);
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
        private Object default_;
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
         * @param default_ the default value of the configuration
         * @return the builder
         */
        public Builder default_(Object default_) {
            this.default_ = default_;
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
