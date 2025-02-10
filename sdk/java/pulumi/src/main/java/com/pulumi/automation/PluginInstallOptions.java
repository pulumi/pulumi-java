// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import javax.annotation.Nullable;

/**
 * Options for installing plugins.
 */
public class PluginInstallOptions {
    /**
     * An empty set of options.
     */
    public static final PluginInstallOptions Empty = PluginInstallOptions.builder().build();

    private final PluginKind kind;
    private final boolean exactVersion;
    @Nullable
    private final String serverUrl;

    private PluginInstallOptions(Builder builder) {
        this.kind = builder.kind;
        this.exactVersion = builder.exactVersion;
        this.serverUrl = builder.serverUrl;
    }

    /**
     * Returns a new builder for {@link PluginInstallOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The kind of plugin to install. Defaults to {@link PluginKind#RESOURCE}.
     *
     * @return the kind of plugin
     */
    public PluginKind kind() {
        return kind;
    }

    /**
     * If {@code true}, force installation of an exact version match (usually &gt;=
     * is accepted).
     * <p>
     * Defaults to {@code false}.
     *
     * @return whether to force exact version match
     */
    public boolean isExactVersion() {
        return exactVersion;
    }

    /**
     * A URL to download plugins from.
     *
     * @return the server URL
     */
    @Nullable
    public String serverUrl() {
        return serverUrl;
    }

    /**
     * Builder for {@link PluginInstallOptions}.
     */
    public static class Builder {
        private PluginKind kind = PluginKind.RESOURCE;
        private boolean exactVersion;
        @Nullable
        private String serverUrl;

        private Builder() {
        }

        /**
         * The kind of plugin to install. Defaults to {@link PluginKind#RESOURCE}.
         *
         * @param kind the kind of plugin
         * @return the builder
         */
        public Builder kind(PluginKind kind) {
            this.kind = kind;
            return this;
        }

        /**
         * Whether to force installation of an exact version match (usually &gt;= is
         * accepted).
         * <p>
         * Defaults to {@code false}.
         *
         * @param exactVersion whether to force exact version match
         * @return the builder
         */
        public Builder exactVersion(boolean exactVersion) {
            this.exactVersion = exactVersion;
            return this;
        }

        /**
         * Sets the server URL for plugin downloads.
         *
         * @param serverUrl the server URL
         * @return the builder
         */
        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        /**
         * Builds the {@link PluginInstallOptions}.
         *
         * @return the options
         */
        public PluginInstallOptions build() {
            return new PluginInstallOptions(this);
        }
    }
}
