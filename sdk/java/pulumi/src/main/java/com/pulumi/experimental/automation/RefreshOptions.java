// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.List;

/**
 * Options controlling the behavior of a {@link WorkspaceStack#refresh}
 * operation.
 */
public final class RefreshOptions extends UpdateOptions {
    private final boolean expectNoChanges;
    private final boolean showSecrets;
    private final boolean skipPendingCreates;
    private final boolean clearPendingCreates;
    private final List<PendingCreateValue> importPendingCreates;

    private RefreshOptions(Builder builder) {
        super(builder);
        this.expectNoChanges = builder.expectNoChanges;
        this.showSecrets = builder.showSecrets;
        this.skipPendingCreates = builder.skipPendingCreates;
        this.clearPendingCreates = builder.clearPendingCreates;
        this.importPendingCreates = builder.importPendingCreates == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.importPendingCreates);
    }

    /**
     * Returns a new builder for {@link RefreshOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return an error if any changes are proposed by this preview.
     *
     * @return true if the preview should fail if any changes are proposed
     */
    public boolean expectNoChanges() {
        return expectNoChanges;
    }

    /**
     * Show config secrets when they appear.
     *
     * @return whether to show secrets
     */
    public boolean showSecrets() {
        return showSecrets;
    }

    /**
     * Ignores any pending create operations
     *
     * @return whether to skip pending creates
     */
    public boolean skipPendingCreates() {
        return skipPendingCreates;
    }

    /**
     * Removes any pending create operations from the stack
     *
     * @return whether to clear pending creates
     */
    public boolean clearPendingCreates() {
        return clearPendingCreates;
    }

    /**
     * Values to import into the stack
     *
     * @return the list of pending create values
     */
    public List<PendingCreateValue> importPendingCreates() {
        return importPendingCreates;
    }

    /**
     * Builder for {@link RefreshOptions}.
     */
    public static final class Builder extends UpdateOptions.Builder<RefreshOptions.Builder> {
        private boolean expectNoChanges;
        private boolean showSecrets;
        private boolean skipPendingCreates;
        private boolean clearPendingCreates;
        private List<PendingCreateValue> importPendingCreates;

        private Builder() {
        }

        /**
         * Return an error if any changes are proposed by this preview.
         *
         * @param expectNoChanges true if the preview should fail if any changes are
         *                        proposed
         * @return the builder
         */
        public Builder expectNoChanges(boolean expectNoChanges) {
            this.expectNoChanges = expectNoChanges;
            return this;
        }

        /**
         * Sets whether to show config secrets when they appear.
         *
         * @param showSecrets the show secrets flag
         * @return the builder
         */
        public Builder showSecrets(boolean showSecrets) {
            this.showSecrets = showSecrets;
            return this;
        }

        /**
         * Ignores any pending create operations
         *
         * @param skipPendingCreates whether to skip pending creates
         * @return the builder
         */
        public Builder skipPendingCreates(boolean skipPendingCreates) {
            this.skipPendingCreates = skipPendingCreates;
            return this;
        }

        /**
         * Removes any pending create operations from the stack
         *
         * @param clearPendingCreates whether to clear pending creates
         * @return the builder
         */
        public Builder clearPendingCreates(boolean clearPendingCreates) {
            this.clearPendingCreates = clearPendingCreates;
            return this;
        }

        /**
         * Sets the values to import into the stack
         *
         * @param importPendingCreates the list of pending create values
         * @return the builder
         */
        public Builder importPendingCreates(List<PendingCreateValue> importPendingCreates) {
            this.importPendingCreates = importPendingCreates;
            return this;
        }

        /**
         * Builds the {@link RefreshOptions}.
         *
         * @return the refresh options
         */
        public RefreshOptions build() {
            return new RefreshOptions(this);
        }
    }
}
