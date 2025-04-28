// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Options controlling the behavior of a {@link WorkspaceStack#destroy}
 * operation.
 */
public final class DestroyOptions extends UpdateOptions {
    private final boolean targetDependents;
    private final boolean showSecrets;
    private final boolean continueOnError;
    private final boolean previewOnly;

    private DestroyOptions(Builder builder) {
        super(builder);
        this.targetDependents = builder.targetDependents;
        this.showSecrets = builder.showSecrets;
        this.continueOnError = builder.continueOnError;
        this.previewOnly = builder.previewOnly;
    }

    /**
     * Returns a new builder for {@link DestroyOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Allows updating of dependent targets discovered but not specified
     * {@link #targets()}
     *
     * @return true if dependent targets should be updated
     */
    public boolean isTargetDependents() {
        return targetDependents;
    }

    /**
     * Show config secrets when they appear.
     *
     * @return whether to show secrets
     */
    public boolean isShowSecrets() {
        return showSecrets;
    }

    /**
     * Continue to perform the update operation despite the occurrence of errors.
     *
     * @return whether to continue on error
     */
    public boolean isContinueOnError() {
        return continueOnError;
    }

    /**
     * Only show a preview of the destroy, but don't perform the destroy itself.
     *
     * @return whether this is just a preview
     */
    public boolean previewOnly() {
        return previewOnly;
    }

    /**
     * Builder for {@link DestroyOptions}.
     */
    public static final class Builder extends UpdateOptions.Builder<DestroyOptions.Builder> {
        private boolean targetDependents;
        private boolean showSecrets;
        private boolean continueOnError;
        private boolean previewOnly;

        private Builder() {
        }

        /**
         * Allows updating of dependent targets discovered but not specified
         * {@link #targets}.
         *
         * @param targetDependents true if dependent targets should be updated
         * @return the builder
         */
        public Builder targetDependents(boolean targetDependents) {
            this.targetDependents = targetDependents;
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
         * Sets whether to continue performing the destroy operation despite the
         * occurrence of errors.
         *
         * @param continueOnError the continue on error flag
         * @return the builder
         */
        public Builder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        /**
         * Sets whether to continue performing the destroy operation despite the
         * occurrence of errors.
         *
         * @param previewOnly the preview only flag
         * @return the builder
         */
        public Builder previewOnly(boolean previewOnly) {
            this.previewOnly = previewOnly;
            return this;
        }

        /**
         * Builds the {@link DestroyOptions}.
         *
         * @return the destroy options
         */
        public DestroyOptions build() {
            return new DestroyOptions(this);
        }
    }
}
