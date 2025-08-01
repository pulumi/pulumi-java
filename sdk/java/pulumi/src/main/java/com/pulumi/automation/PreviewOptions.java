// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.pulumi.Context;

/**
 * Options controlling the behavior of an {@link WorkspaceStack#preview}
 * operation.
 */
public final class PreviewOptions extends UpdateOptions {
    private final boolean expectNoChanges;
    private final boolean diff;
    private final List<String> replaces;
    private final boolean excludeDependents;
    private final boolean targetDependents;
    @Nullable
    private final Consumer<Context> program;
    @Nullable
    private final String plan;
    @Nullable
    private final Logger logger;

    private PreviewOptions(Builder builder) {
        super(builder);
        this.expectNoChanges = builder.expectNoChanges;
        this.diff = builder.diff;
        this.replaces = builder.replaces == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.replaces);
        this.excludeDependents = builder.excludeDependents;
        this.targetDependents = builder.targetDependents;
        this.program = builder.program;
        this.plan = builder.plan;
        this.logger = builder.logger;
    }

    /**
     * Returns a new builder for {@link PreviewOptions}.
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
    public List<String> replaces() {
        return replaces;
    }

    /**
     * Allows exclusion of dependent targets discovered but not specified
     * {@link #excludes()}
     *
     * @return true if dependent targets should be excluded
     */
    public boolean excludeDependents() {
        return excludeDependents;
    }

    /**
     * Allows updating of dependent targets discovered but not specified
     * {@link #targets()}
     *
     * @return true if dependent targets should be updated
     */
    public boolean targetDependents() {
        return targetDependents;
    }

    /**
     * The program to execute as part of the update.
     *
     * @return the program to execute
     */
    @Nullable
    public Consumer<Context> program() {
        return program;
    }

    /**
     * The path to where the update plan will be stored.
     *
     * @return the path to the update plan
     */
    @Nullable
    public String plan() {
        return plan;
    }

    /**
     * A custom logger instance that will be used for the action. Note that it will
     * only be used if {@link #program()} is also provided.
     *
     * @return the logger
     */
    @Nullable
    public Logger logger() {
        return logger;
    }

    /**
     * Builder for {@link PreviewOptions}.
     */
    public static final class Builder extends UpdateOptions.Builder<PreviewOptions.Builder> {
        private boolean expectNoChanges;
        private boolean diff;
        @Nullable
        private List<String> replaces;
        private boolean excludeDependents;
        private boolean targetDependents;
        @Nullable
        private Consumer<Context> program;
        @Nullable
        private String plan;
        @Nullable
        private Logger logger;

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
         * Allows exclusion of dependent targets discovered but not specified
         * {@link #excludes()}
         *
         * @param excludeDependents true if dependent targets should be excluded
         * @return the builder
         */
        public Builder excludeDependents(boolean excludeDependents) {
            this.excludeDependents = excludeDependents;
            return this;
        }

        /**
         * Allows updating of dependent targets discovered but not specified
         * {@link #targets()}
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
         * Save the operations proposed by the preview to a plan file at the given path.
         *
         * @param plan the path to the update plan
         * @return the builder
         */
        public Builder plan(String plan) {
            this.plan = plan;
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
         * Builds the {@link PreviewOptions}.
         *
         * @return the preview options
         */
        public PreviewOptions build() {
            return new PreviewOptions(this);
        }
    }
}
