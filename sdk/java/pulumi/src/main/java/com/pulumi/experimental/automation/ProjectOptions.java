// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An optional set of project options.
 */
public class ProjectOptions {
    @Nullable
    private final String refresh;

    private ProjectOptions(Builder builder) {
        refresh = builder.refresh;
    }

    /**
     * Returns a new builder for {@link ProjectOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Set to {@code "always"} to always run a refresh as part of a pulumi update /
     * preview / destroy.
     *
     * @return {@code "always"} to always run a refresh as part of a pulumi update /
     *         preview / destroy
     */
    @Nullable
    public String refresh() {
        return refresh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectOptions) o;
        return Objects.equals(refresh, that.refresh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refresh);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder().refresh(refresh);
    }

    /**
     * Builder for {@link ProjectOptions}.
     */
    public static class Builder {
        @Nullable
        private String refresh;

        private Builder() {
        }

        /**
         * Set to {@code "always"} to always run a refresh as part of a pulumi update /
         * preview / destroy.
         *
         * @param refresh {@code "always"} to always run a refresh as part of a pulumi
         *                update / preview / destroy
         * @return the builder
         */
        public Builder refresh(String refresh) {
            this.refresh = refresh;
            return this;
        }

        /**
         * Builds the {@link ProjectOptions}.
         *
         * @return the project options
         */
        public ProjectOptions build() {
            return new ProjectOptions(this);
        }
    }
}
