// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A description of the Project's program runtime and associated metadata.
 */
public class ProjectRuntime {
    private final ProjectRuntimeName name;
    @Nullable
    private final ProjectRuntimeOptions options;

    private ProjectRuntime(Builder builder) {
        this.name = builder.name;
        this.options = builder.options;
    }

    /**
     * Returns a new builder for {@link ProjectRuntime}.
     *
     * @param name the runtime name
     * @return the builder
     */
    public static Builder builder(ProjectRuntimeName name) {
        return new Builder().name(name);
    }

    /**
     * Returns the runtime name.
     *
     * @return the runtime name
     */
    public ProjectRuntimeName getName() {
        return name;
    }

    /**
     * Returns the runtime options.
     *
     * @return the runtime options
     */
    @Nullable
    public ProjectRuntimeOptions getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectRuntime) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, options);
    }

    /**
     * Builder for {@link ProjectRuntime}.
     */
    public static class Builder {
        private ProjectRuntimeName name;
        @Nullable
        private ProjectRuntimeOptions options;

        private Builder() {
        }

        /**
         * The runtime name.
         *
         * @param name the runtime name
         * @return the builder
         */
        public Builder name(ProjectRuntimeName name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * The runtime options.
         *
         * @param options the runtime options
         * @return the builder
         */
        public Builder options(ProjectRuntimeOptions options) {
            this.options = options;
            return this;
        }

        /**
         * Builds the {@link ProjectRuntime}.
         *
         * @return the project runtime
         */
        public ProjectRuntime build() {
            return new ProjectRuntime(this);
        }
    }
}
