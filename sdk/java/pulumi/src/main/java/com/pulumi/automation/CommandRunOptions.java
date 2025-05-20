// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.pulumi.automation.events.EngineEvent;

/**
 * Configuration options for running a Pulumi command.
 */
public final class CommandRunOptions {
    /**
     * An empty set of options.
     */
    public static final CommandRunOptions Empty = CommandRunOptions.builder().build();

    @Nullable
    private final Path workingDir;
    private final Map<String, String> additionalEnv;
    @Nullable
    private final String standardInput;
    @Nullable
    private final Consumer<String> onStandardOutput;
    @Nullable
    private final Consumer<String> onStandardError;
    @Nullable
    private final Consumer<EngineEvent> onEngineEvent;

    private CommandRunOptions(Builder builder) {
        this.workingDir = builder.workingDir;
        this.additionalEnv = builder.additionalEnv == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.additionalEnv);
        this.standardInput = builder.standardInput;
        this.onStandardOutput = builder.onStandardOutput;
        this.onStandardError = builder.onStandardError;
        this.onEngineEvent = builder.onEngineEvent;
    }

    /**
     * Returns a new builder for {@link CommandRunOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the working directory for the command.
     *
     * @return the working directory
     */
    @Nullable
    public Path workingDir() {
        return workingDir;
    }

    /**
     * Returns the additional environment variables for the command.
     *
     * @return the additional environment variables
     */
    public Map<String, String> additionalEnv() {
        return additionalEnv;
    }

    /**
     * Returns the standard input for the command.
     *
     * @return the standard input
     */
    @Nullable
    public String standardInput() {
        return standardInput;
    }

    /**
     * Returns a consumer that will be called with each line of standard output from
     * the command.
     *
     * @return the consumer for standard output
     */
    @Nullable
    public Consumer<String> onStandardOutput() {
        return onStandardOutput;
    }

    /**
     * Returns a consumer that will be called with each line of standard error from
     * the command.
     *
     * @return the consumer for standard error
     */
    @Nullable
    public Consumer<String> onStandardError() {
        return onStandardError;
    }

    /**
     * Returns a consumer that will be called with each engine event from the
     * command.
     *
     * @return the consumer for engine events
     */
    @Nullable
    public Consumer<EngineEvent> onEngineEvent() {
        return onEngineEvent;
    }

    /**
     * Returns a new {@link CommandRunOptions} with the given additional environment
     * variables.
     *
     * @param newEnv the additional environment variables to add
     * @return the new {@link CommandRunOptions}
     */
    public CommandRunOptions withAdditionalEnv(Map<String, String> newEnv) {
        var mergedEnv = new HashMap<>(additionalEnv);
        mergedEnv.putAll(newEnv);
        return toBuilder().additionalEnv(mergedEnv).build();
    }

    /**
     * Returns a new {@link CommandRunOptions} with the given working directory.
     *
     * @param workingDir the working directory
     * @return the new {@link CommandRunOptions}
     */
    public CommandRunOptions withWorkingDir(Path workingDir) {
        return toBuilder().workingDir(workingDir).build();
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    private Builder toBuilder() {
        return CommandRunOptions.builder()
                .workingDir(workingDir)
                .additionalEnv(additionalEnv)
                .standardInput(standardInput)
                .onStandardOutput(onStandardOutput)
                .onStandardError(onStandardError)
                .onEngineEvent(onEngineEvent);
    }

    /**
     * Builder for {@link CommandRunOptions}.
     */
    public static class Builder {
        @Nullable
        private Path workingDir;
        @Nullable
        private Map<String, String> additionalEnv;
        @Nullable
        private String standardInput;
        @Nullable
        private Consumer<String> onStandardOutput;
        @Nullable
        private Consumer<String> onStandardError;
        @Nullable
        private Consumer<EngineEvent> onEngineEvent;

        private Builder() {
        }

        /**
         * Sets the working directory for the command.
         *
         * @param workingDir the working directory
         * @return the builder
         */
        public Builder workingDir(Path workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        /**
         * Sets the additional environment variables for the command.
         *
         * @param additionalEnv the additional environment variables
         * @return the builder
         */
        public Builder additionalEnv(Map<String, String> additionalEnv) {
            this.additionalEnv = additionalEnv;
            return this;
        }

        /**
         * Sets the standard input for the command.
         *
         * @param standardInput the standard input
         * @return the builder
         */
        public Builder standardInput(String standardInput) {
            this.standardInput = standardInput;
            return this;
        }

        /**
         * Sets a consumer that will be called with each line of standard output from
         * the command.
         *
         * @param onStandardOutput the consumer for standard output
         * @return the builder
         */
        public Builder onStandardOutput(Consumer<String> onStandardOutput) {
            this.onStandardOutput = onStandardOutput;
            return this;
        }

        /**
         * Sets a consumer that will be called with each line of standard error from
         * the command.
         *
         * @param onStandardError the consumer for standard error
         * @return the builder
         */
        public Builder onStandardError(Consumer<String> onStandardError) {
            this.onStandardError = onStandardError;
            return this;
        }

        /**
         * Sets a consumer that will be called with each engine event from the command.
         *
         * @param onEngineEvent the consumer for engine events
         * @return the builder
         */
        public Builder onEngineEvent(Consumer<EngineEvent> onEngineEvent) {
            this.onEngineEvent = onEngineEvent;
            return this;
        }

        /**
         * Builds the {@link CommandRunOptions}.
         *
         * @return the options
         */
        public CommandRunOptions build() {
            return new CommandRunOptions(this);
        }
    }
}
