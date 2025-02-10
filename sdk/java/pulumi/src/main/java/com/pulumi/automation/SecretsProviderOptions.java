// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import javax.annotation.Nullable;

/**
 * Options to pass into
 * {@link WorkspaceStack#changeSecretsProvider(String, SecretsProviderOptions)}.
 */
public class SecretsProviderOptions {

    private final String newPassphrase;

    private SecretsProviderOptions(Builder builder) {
        this.newPassphrase = builder.newPassphrase;
    }

    /**
     * The new passphrase to use in the passphrase provider.
     *
     * @return the new passphrase
     */
    @Nullable
    public String newPassphrase() {
        return newPassphrase;
    }

    /**
     * Creates a new builder for {@link SecretsProviderOptions}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SecretsProviderOptions}.
     */
    public static class Builder {
        private String newPassphrase;

        private Builder() {
        }

        /**
         * Sets the new passphrase to use in the passphrase provider.
         *
         * @param newPassphrase the passphrase to set
         * @return this builder instance
         */
        public Builder newPassphrase(String newPassphrase) {
            this.newPassphrase = newPassphrase;
            return this;
        }

        /**
         * Builds a new {@link SecretsProviderOptions} instance.
         *
         * @return a new SecretsProviderOptions instance
         */
        public SecretsProviderOptions build() {
            return new SecretsProviderOptions(this);
        }
    }
}
