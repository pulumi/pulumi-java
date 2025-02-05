// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Represents configuration settings for a stack.
 * This class uses the Builder pattern for object construction.
 */
public class StackSettings {
    @Nullable
    private final String secretsProvider;
    @Nullable
    private final String encryptedKey;
    @Nullable
    private final String encryptionSalt;
    private final Map<String, StackSettingsConfigValue> config;

    private StackSettings(Builder builder) {
        this.secretsProvider = builder.secretsProvider;
        this.encryptedKey = builder.encryptedKey;
        this.encryptionSalt = builder.encryptionSalt;
        this.config = builder.config == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.config);
    }

    /**
     * Returns a new builder for {@link StackSettings}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The stack's secrets provider.
     *
     * @return the secrets provider
     */
    @Nullable
    public String getSecretsProvider() {
        return secretsProvider;
    }

    /**
     * The KMS-encrypted ciphertext for the data key used for secrets encryption.
     * Only used for cloud-based secrets providers.
     *
     * @return the encrypted key
     */
    @Nullable
    public String getEncryptedKey() {
        return encryptedKey;
    }

    /**
     * The stack's base64 encoded encryption salt.
     * Only used for passphrase-based secrets providers.
     *
     * @return the encryption salt
     */
    @Nullable
    public String getEncryptionSalt() {
        return encryptionSalt;
    }

    /**
     * Optional configuration bag.
     *
     * @return optional configuration bag
     */
    public Map<String, StackSettingsConfigValue> getConfig() {
        return config;
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} instance with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .secretsProvider(secretsProvider)
                .encryptedKey(encryptedKey)
                .encryptionSalt(encryptionSalt)
                .config(config.isEmpty() ? null : config);
    }

    /**
     * Builder for {@link StackSettings}.
     */
    public static class Builder {
        @Nullable
        private String secretsProvider;
        @Nullable
        private String encryptedKey;
        @Nullable
        private String encryptionSalt;
        @Nullable
        private Map<String, StackSettingsConfigValue> config;

        private Builder() {
        }

        /**
         * The stack's secrets provider.
         *
         * @param secretsProvider the secrets provider to set
         * @return the builder
         */
        public Builder secretsProvider(@Nullable String secretsProvider) {
            this.secretsProvider = secretsProvider;
            return this;
        }

        /**
         * The KMS-encrypted ciphertext for the data key used for secrets
         * encryption. Only used for cloud-based secrets providers.
         *
         * @param encryptedKey the encrypted key to set
         * @return the builder
         */
        public Builder encryptedKey(@Nullable String encryptedKey) {
            this.encryptedKey = encryptedKey;
            return this;
        }

        /**
         * The stack's base64 encoded encryption salt.
         * Only used for passphrase-based secrets providers.
         *
         * @param encryptionSalt the encryption salt to set
         * @return the builder
         */
        public Builder encryptionSalt(@Nullable String encryptionSalt) {
            this.encryptionSalt = encryptionSalt;
            return this;
        }

        /**
         * The configuration for the stack.
         *
         * @param config the configuration to set
         * @return the builder
         */
        public Builder config(@Nullable Map<String, StackSettingsConfigValue> config) {
            this.config = config;
            return this;
        }

        /**
         * Builds the {@link StackSettings}.
         *
         * @return the stack settings
         */
        public StackSettings build() {
            return new StackSettings(this);
        }
    }
}
