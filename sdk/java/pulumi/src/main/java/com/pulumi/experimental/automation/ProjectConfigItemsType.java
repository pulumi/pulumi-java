// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * The project configuration items type.
 */
public class ProjectConfigItemsType {
    private final String type;
    @Nullable
    private final ProjectConfigItemsType items;

    private ProjectConfigItemsType(Builder builder) {
        type = builder.type;
        items = builder.items;
    }

    /**
     * Returns a new builder for {@link ProjectConfigItemsType}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The type of the configuration item.
     *
     * @return the type of the configuration item
     */
    public String getType() {
        return type;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectConfigItemsType) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, items);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .type(type)
                .items(items);
    }

    /**
     * Builder for {@link ProjectConfigItemsType}.
     */
    public static class Builder {
        private String type;
        @Nullable
        private ProjectConfigItemsType items;

        private Builder() {
        }

        /**
         * The type of the configuration item.
         *
         * @param type the type of the configuration item
         * @return the builder
         */
        public Builder type(String type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * The configuration items type.
         *
         * @param items the configuration items
         * @return the builder
         */
        public Builder items(ProjectConfigItemsType items) {
            this.items = items;
            return this;
        }

        /**
         * Builds the {@link ProjectConfigItemsType}.
         *
         * @return the project configuration items type
         */
        public ProjectConfigItemsType build() {
            return new ProjectConfigItemsType(this);
        }
    }
}
