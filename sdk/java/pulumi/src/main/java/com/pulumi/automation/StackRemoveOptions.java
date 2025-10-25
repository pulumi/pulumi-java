// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * Options for removing stacks.
 */
public final class StackRemoveOptions {
    /**
     * An empty set of options.
     */
    public static final StackRemoveOptions Empty = StackRemoveOptions.builder().build();

    private final boolean force;
    private final boolean preserveConfig;
    private final boolean removeBackups;

    private StackRemoveOptions(Builder builder) {
        this.force = builder.force;
        this.preserveConfig = builder.preserveConfig;
        this.removeBackups = builder.removeBackups;
    }

    /**
     * Returns a new builder for {@link StackRemoveOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Forces deletion of the stack, leaving behind any resources managed by the stack
     *
     * @return whether to force deletion
     */
    public boolean force() {
        return force;
    }

    /**
     * Do not delete the corresponding {@code Pulumi.<stack-name>.yaml} configuration file for the stack
     *
     * @return whether to preserve the configuration
     */
    public boolean preserveConfig() {
        return preserveConfig;
    }

    /**
     * Remove backups of the stack, if using the DIY backend
     *
     * @return whether to remove backups
     */
    public boolean removeBackups() {
        return removeBackups;
    }

    /**
     * Builder for {@link StackRemoveOptions}.
     */
    public static class Builder {
        private boolean force;
        private boolean preserveConfig;
        private boolean removeBackups;

        private Builder() {
        }

        /**
         * Forces deletion of the stack, leaving behind any resources managed by the stack
         *
         * @param force whether to force deletion
         * @return the builder
         */
        public Builder force(boolean force) {
            this.force = force;
            return this;
        }

        /**
         * Do not delete the corresponding {@code Pulumi.<stack-name>.yaml} configuration file for the stack
         *
         * @param preserveConfig whether to preserve the configuration
         * @return the builder
         */
        public Builder preserveConfig(boolean preserveConfig) {
            this.preserveConfig = preserveConfig;
            return this;
        }

        /**
         * Remove backups of the stack, if using the DIY backend
         *
         * @param removeBackups whether to remove backups
         * @return the builder
         */
        public Builder removeBackups(boolean removeBackups) {
            this.removeBackups = removeBackups;
            return this;
        }

        /**
         * Builds the {@link StackRemoveOptions}.
         *
         * @return the options
         */
        public StackRemoveOptions build() {
            return new StackRemoveOptions(this);
        }
    }
}
