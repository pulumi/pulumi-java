// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents configuration settings for a stack.
 * This class uses the Builder pattern for object construction.
 */
public class StackSettings {
    @Nullable
    private final String secretsprovider;
    @Nullable
    private final String encryptedkey;
    @Nullable
    private final String encryptionsalt;
    private final Map<String, StackSettingsConfigValue> config;

    // TODO: Add support for `environment`:
    // https://github.com/pulumi/pulumi-java/issues/1654

    private StackSettings(Builder builder) {
        this.secretsprovider = builder.secretsprovider;
        this.encryptedkey = builder.encryptedkey;
        this.encryptionsalt = builder.encryptionsalt;
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
    public String secretsProvider() {
        return secretsprovider;
    }

    /**
     * The KMS-encrypted ciphertext for the data key used for secrets encryption.
     * Only used for cloud-based secrets providers.
     *
     * @return the encrypted key
     */
    @Nullable
    public String encryptedKey() {
        return encryptedkey;
    }

    /**
     * The stack's base64 encoded encryption salt.
     * Only used for passphrase-based secrets providers.
     *
     * @return the encryption salt
     */
    @Nullable
    public String encryptionSalt() {
        return encryptionsalt;
    }

    /**
     * Optional configuration bag.
     *
     * @return optional configuration bag
     */
    public Map<String, StackSettingsConfigValue> config() {
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (StackSettings) o;
        return Objects.equals(secretsprovider, that.secretsprovider) &&
                Objects.equals(encryptedkey, that.encryptedkey) &&
                Objects.equals(encryptionsalt, that.encryptionsalt) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretsprovider, encryptedkey, encryptionsalt, config);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} instance with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .secretsProvider(secretsprovider)
                .encryptedKey(encryptedkey)
                .encryptionSalt(encryptionsalt)
                .config(config.isEmpty() ? null : config);
    }

    /**
     * Builder for {@link StackSettings}.
     */
    public static class Builder {
        @Nullable
        private String secretsprovider;
        @Nullable
        private String encryptedkey;
        @Nullable
        private String encryptionsalt;
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
            this.secretsprovider = secretsProvider;
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
            this.encryptedkey = encryptedKey;
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
            this.encryptionsalt = encryptionSalt;
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
