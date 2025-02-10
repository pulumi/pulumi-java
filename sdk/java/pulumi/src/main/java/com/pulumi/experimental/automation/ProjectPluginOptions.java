// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Plugin options.
 */
public class ProjectPluginOptions {
    private final String name;
    @Nullable
    private final String version;
    private final String path;

    private ProjectPluginOptions(Builder builder) {
        name = builder.name;
        version = builder.version;
        path = builder.path;
    }

    /**
     * Returns a new builder for {@link ProjectPluginOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The name of the plugin.
     *
     * @return the name of the plugin
     */
    public String name() {
        return name;
    }

    /**
     * The version of the plugin.
     *
     * @return the version of the plugin
     */
    @Nullable
    public String version() {
        return version;
    }

    /**
     * The path of the plugin.
     *
     * @return the path of the plugin
     */
    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectPluginOptions) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, path);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .version(version)
                .path(path);
    }

    /**
     * Builder for {@link ProjectPluginOptions}.
     */
    public static class Builder {
        @Nullable
        private String name;
        @Nullable
        private String version;
        @Nullable
        private String path;

        private Builder() {
        }

        /**
         * The name of the plugin.
         *
         * @param name the name of the plugin
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The version of the plugin.
         *
         * @param version the version of the plugin
         * @return the builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * The path of the plugin.
         *
         * @param path the path of the plugin
         * @return the builder
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Builds the {@link ProjectPluginOptions}.
         *
         * @return the plugin options
         */
        public ProjectPluginOptions build() {
            return new ProjectPluginOptions(this);
        }
    }
}
