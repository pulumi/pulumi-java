// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import javax.annotation.Nullable;

/**
 * Options for removing plugins.
 */
public final class PluginRemoveOptions {
    @Nullable
    private final String name;
    @Nullable
    private final String versionRange;
    private final PluginKind kind;

    private PluginRemoveOptions(Builder builder) {
        this.name = builder.name;
        this.versionRange = builder.versionRange;
        this.kind = builder.kind;
    }

    /**
     * Returns a new builder for {@link PluginRemoveOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The name of the plugin to remove.
     *
     * @return the name of the plugin
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * The semver range to check when removing plugins matching the given name e.g.
     * "1.0.0", "&gt;1.0.0".
     *
     * @return the semver range
     */
    @Nullable
    public String getVersionRange() {
        return versionRange;
    }

    /**
     * The kind of plugin to remove. Defaults to {@link PluginKind#RESOURCE}.
     *
     * @return the kind of plugin
     */
    public PluginKind getKind() {
        return kind;
    }

    /**
     * Builder for {@link PluginRemoveOptions}.
     */
    public static class Builder {
        @Nullable
        private String name;
        @Nullable
        private String versionRange;
        private PluginKind kind = PluginKind.RESOURCE;

        private Builder() {
        }

        /**
         * The name of the plugin to remove.
         *
         * @param name the name of the plugin
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The semver range to check when removing plugins matching the given name e.g.
         * "1.0.0", "&gt;1.0.0".
         *
         * @param versionRange the semver range
         * @return the builder
         */
        public Builder versionRange(String versionRange) {
            this.versionRange = versionRange;
            return this;
        }

        /**
         * The kind of plugin to remove. Defaults to {@link PluginKind#RESOURCE}.
         *
         * @param kind the kind of plugin
         * @return the builder
         */
        public Builder kind(PluginKind kind) {
            this.kind = kind;
            return this;
        }

        /**
         * Builds the {@link PluginRemoveOptions}.
         *
         * @return the options
         */
        public PluginRemoveOptions build() {
            return new PluginRemoveOptions(this);
        }
    }
}
