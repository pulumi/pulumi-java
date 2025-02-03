// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

/**
 * Config values for Importing Pending Create operations.
 */
public final class PendingCreateValue {
    private final String urn;
    private final String id;

    private PendingCreateValue(Builder builder) {
        this.urn = builder.urn;
        this.id = builder.id;
    }

    /**
     * Returns a new builder for {@link PendingCreateValue}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the URN of the resource.
     *
     * @return the logical URN used by Pulumi
     */
    public String getUrn() {
        return urn;
    }

    /**
     * Gets the ID of the resource.
     *
     * @return the ID used by the provider
     */
    public String getId() {
        return id;
    }

    /**
     * Builder for {@link PendingCreateValue}.
     */
    public static class Builder {
        private String urn;
        private String id;

        /**
         * Sets the URN of the resource.
         *
         * @param urn the logical URN to set
         * @return the builder
         */
        public Builder setUrn(String urn) {
            this.urn = Objects.requireNonNull(urn);
            return this;
        }

        /**
         * Sets the ID of the resource.
         *
         * @param id the ID to set
         * @return the builder
         */
        public Builder setId(String id) {
            this.id = Objects.requireNonNull(id);
            return this;
        }

        /**
         * Builds the {@link PendingCreateValue}.
         *
         * @return the new PendingCreateValue instance
         */
        public PendingCreateValue build() {
            return new PendingCreateValue(this);
        }
    }
}
