// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.pulumi.Context;

/**
 * Options controlling the behavior of an {@link WorkspaceStack#up} operation.
 */
public final class UpOptions extends UpdateOptions {
    private final boolean expectNoChanges;
    private final boolean diff;
    private final List<String> replaces;
    private final boolean targetDependents;
    @Nullable
    private final Consumer<Context> program;
    @Nullable
    private final String plan;
    private final boolean showSecrets;
    @Nullable
    private final Logger logger;
    private final boolean continueOnError;

    private UpOptions(Builder builder) {
        super(builder);
        this.expectNoChanges = builder.expectNoChanges;
        this.diff = builder.diff;
        this.replaces = builder.replaces;
        this.targetDependents = builder.targetDependents;
        this.program = builder.program;
        this.plan = builder.plan;
        this.showSecrets = builder.showSecrets;
        this.logger = builder.logger;
        this.continueOnError = builder.continueOnError;
    }

    /**
     * Returns a new builder for {@link UpOptions}.
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
    public boolean isExpectNoChanges() {
        return expectNoChanges;
    }

    /**
     * Display the operation as a rich diff showing the overall change.
     *
     * @return true if a rich diff should be displayed
     */
    public boolean isDiff() {
        return diff;
    }

    /**
     * Resources to replace.
     *
     * @return the list of resource URNs to replace
     */
    public List<String> getReplaces() {
        return replaces;
    }

    /**
     * Allows updating of dependent targets discovered but not specified
     * {@link #getTargets()}
     *
     * @return true if dependent targets should be updated
     */
    public boolean isTargetDependents() {
        return targetDependents;
    }

    /**
     * The program to execute as part of the update.
     *
     * @return the program to execute
     */
    @Nullable
    public Consumer<Context> getProgram() {
        return program;
    }

    /**
     * The path to an update plan to use for the update.
     *
     * @return the path to the update plan
     */
    @Nullable
    public String getPlan() {
        return plan;
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
     * A custom logger instance that will be used for the action. Note that it will
     * only be used if {@link #getProgram} is also provided.
     *
     * @return the logger
     */
    @Nullable
    public Logger getLogger() {
        return logger;
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
     * Builder for {@link UpOptions}.
     */
    public static final class Builder extends UpdateOptions.Builder<UpOptions.Builder> {
        private boolean expectNoChanges;
        private boolean diff;
        @Nullable
        private List<String> replaces;
        private boolean targetDependents;
        @Nullable
        private Consumer<Context> program;
        @Nullable
        private String plan;
        private boolean showSecrets;
        @Nullable
        private Logger logger;
        private boolean continueOnError;

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
         * Display the operation as a rich diff showing the overall change.
         *
         * @param diff true if a rich diff should be displayed
         * @return the builder
         */
        public Builder diff(boolean diff) {
            this.diff = diff;
            return this;
        }

        /**
         * Resources to replace.
         *
         * @param replaces the list of resource URNs to replace
         * @return the builder
         */
        public Builder replaces(List<String> replaces) {
            this.replaces = replaces;
            return this;
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
         * The program to execute as part of the update.
         *
         * @param program the program to execute
         * @return the builder
         */
        public Builder program(Consumer<Context> program) {
            this.program = program;
            return this;
        }

        /**
         * The path to an update plan to use for the update.
         *
         * @param plan the path to the update plan
         * @return the builder
         */
        public Builder plan(String plan) {
            this.plan = plan;
            return this;
        }

        /**
         * Show config secrets when they appear.
         *
         * @param showSecrets whether to show secrets
         * @return the builder
         */
        public Builder showSecrets(boolean showSecrets) {
            this.showSecrets = showSecrets;
            return this;
        }

        /**
         * A custom logger instance that will be used for the action. Note that it will
         * only be used if {@link #program} is also provided.
         *
         * @param logger the logger
         * @return the builder
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Continue to perform the update operation despite the occurrence of errors.
         *
         * @param continueOnError whether to continue on error
         * @return the builder
         */
        public Builder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        /**
         * Builds the {@link UpOptions}.
         *
         * @return the up options
         */
        public UpOptions build() {
            return new UpOptions(this);
        }
    }
}
