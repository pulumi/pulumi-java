// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * Options to configure a {@link LocalPulumiCommand} instance.
 */
public class LocalPulumiCommandOptions {
    public static final LocalPulumiCommandOptions EMPTY = LocalPulumiCommandOptions.builder().build();

    private final boolean skipVersionCheck;

    private LocalPulumiCommandOptions(Builder builder) {
        this.skipVersionCheck = builder.skipVersionCheck;
    }

    /**
     * Returns a new builder for {@link LocalPulumiCommandOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * If {@code true}, skips the version validation that checks if an existing
     * Pulumi CLI installation is compatible with the SDK.
     *
     * @return whether to skip the version check
     */
    public boolean isSkipVersionCheck() {
        return skipVersionCheck;
    }

    /**
     * A builder for {@link LocalPulumiCommandOptions}.
     */
    public static final class Builder {
        private boolean skipVersionCheck;

        private Builder() {
        }

        /**
         * Sets whether to skip the version validation that checks if an existing
         * Pulumi CLI installation is compatible with the SDK.
         *
         * @param skipVersionCheck whether to skip the version check
         * @return this builder
         */
        public Builder skipVersionCheck(boolean skipVersionCheck) {
            this.skipVersionCheck = skipVersionCheck;
            return this;
        }

        /**
         * Builds the {@link LocalPulumiCommandOptions}.
         *
         * @return the options
         */
        public LocalPulumiCommandOptions build() {
            return new LocalPulumiCommandOptions(this);
        }
    }
}
