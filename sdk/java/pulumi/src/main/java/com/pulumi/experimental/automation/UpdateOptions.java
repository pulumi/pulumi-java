// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.pulumi.experimental.automation.events.EngineEvent;

/**
 * Common options controlling the behavior of update actions taken
 * against an instance of {@link WorkspaceStack}.
 */
public abstract class UpdateOptions {
    @Nullable
    private final Integer parallel;
    @Nullable
    private final String message;
    private final List<String> targets;
    private final List<String> policyPacks;
    private final List<String> policyPackConfigs;
    @Nullable
    private final Consumer<String> onStandardOutput;
    @Nullable
    private final Consumer<String> onStandardError;
    @Nullable
    private final Consumer<EngineEvent> onEvent;
    @Nullable
    private final String color;
    private final boolean logFlow;
    @Nullable
    private final Integer logVerbosity;
    private final boolean logToStdErr;
    @Nullable
    private final String tracing;
    private final boolean debug;
    private final boolean json;

    protected UpdateOptions(Builder<?> builder) {
        parallel = builder.parallel;
        message = builder.message;
        targets = builder.targets == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.targets);
        policyPacks = builder.policyPacks == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.policyPacks);
        policyPackConfigs = builder.policyPackConfigs == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.policyPackConfigs);
        onStandardOutput = builder.onStandardOutput;
        onStandardError = builder.onStandardError;
        onEvent = builder.onEvent;
        color = builder.color;
        logFlow = builder.logFlow;
        logVerbosity = builder.logVerbosity;
        logToStdErr = builder.logToStdErr;
        tracing = builder.tracing;
        debug = builder.debug;
        json = builder.json;
    }

    /**
     * Allow resource operations to run in parallel at once. Specify 1 for no
     * parallelism.
     *
     * @return the number of resource operations to run in parallel
     */
    @Nullable
    public Integer getParallel() {
        return parallel;
    }

    /**
     * Optional message to associate with the operation.
     *
     * @return the message
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * A list of resource URNs to target during the operation. Wildcards (*, **) are
     * also supported.
     *
     * @return the list of target resource URNs
     */
    public List<String> getTargets() {
        return targets;
    }

    /**
     * A list of paths to policy packs to run during the operation.
     *
     * @return the list of paths to policy packs
     */
    public List<String> getPolicyPacks() {
        return policyPacks;
    }

    /**
     * A list of paths to policy pack JSON configuration files to use during the
     * operation.
     *
     * @return the list of paths to policy pack JSON configuration files
     */
    public List<String> getPolicyPackConfigs() {
        return policyPackConfigs;
    }

    /**
     * Optional callback which is invoked whenever StandardOutput is written into.
     *
     * @return the callback
     */
    @Nullable
    public Consumer<String> getOnStandardOutput() {
        return onStandardOutput;
    }

    /**
     * Optional callback which is invoked whenever StandardError is written into.
     *
     * @return the callback
     */
    @Nullable
    public Consumer<String> getOnStandardError() {
        return onStandardError;
    }

    /**
     * Optional callback which is invoked with the engine events.
     *
     * @return the callback
     */
    @Nullable
    public Consumer<EngineEvent> getOnEvent() {
        return onEvent;
    }

    /**
     * Colorize output. Choices are: always, never, raw, auto (default "auto")
     *
     * @return the colorize output setting
     */
    @Nullable
    public String getColor() {
        return color;
    }

    /**
     * Flow log settings to child processes (like plugins)
     *
     * @return the flow log settings
     */
    public boolean isLogFlow() {
        return logFlow;
    }

    /**
     * Enable verbose logging (e.g., v=3); anything &gt;3 is very verbose
     *
     * @return the log verbosity
     */
    @Nullable
    public Integer getLogVerbosity() {
        return logVerbosity;
    }

    /**
     * Log to stderr instead of to files
     *
     * @return the log to stderr setting
     */
    public boolean isLogToStdErr() {
        return logToStdErr;
    }

    /**
     * Emit tracing to the specified endpoint. Use the file: scheme to write tracing
     * data to a local file
     *
     * @return the tracing endpoint
     */
    @Nullable
    public String getTracing() {
        return tracing;
    }

    /**
     * Print detailed debugging output during resource operations
     *
     * @return the debug setting
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Format standard output as JSON not text.
     *
     * @return the JSON output setting
     */
    public boolean isJson() {
        return json;
    }

    /**
     * Base builder for all update options.
     */
    public static abstract class Builder<B extends Builder<B>> {
        @Nullable
        private Integer parallel;
        @Nullable
        private String message;
        @Nullable
        private List<String> targets;
        @Nullable
        private List<String> policyPacks;
        @Nullable
        private List<String> policyPackConfigs;
        @Nullable
        private Consumer<String> onStandardOutput;
        @Nullable
        private Consumer<String> onStandardError;
        @Nullable
        private Consumer<EngineEvent> onEvent;
        @Nullable
        private String color;
        private boolean logFlow;
        @Nullable
        private Integer logVerbosity;
        private boolean logToStdErr;
        @Nullable
        private String tracing;
        private boolean debug;
        private boolean json;

        protected Builder() {
        }

        /**
         * Allow resource operations to run in parallel at once. Specify 1 for no
         * parallelism.
         *
         * @param parallel the number of resource operations to run in parallel
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B parallel(Integer parallel) {
            this.parallel = parallel;
            return (B) this;
        }

        /**
         * Optional message to associate with the operation.
         *
         * @param message the message
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B message(String message) {
            this.message = message;
            return (B) this;
        }

        /**
         * A list of resource URNs to target during the operation. Wildcards (*, **) are
         * also supported.
         *
         * @param targets the list of target resource URNs
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B targets(List<String> targets) {
            this.targets = targets;
            return (B) this;
        }

        /**
         * A list of paths to policy packs to run during the operation.
         *
         * @param policyPacks the list of paths to policy packs
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B policyPacks(List<String> policyPacks) {
            this.policyPacks = policyPacks;
            return (B) this;
        }

        /**
         * A list of paths to policy pack JSON configuration files to use during the
         * operation.
         *
         * @param policyPackConfigs the list of paths to policy pack JSON configuration
         *                          files
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B policyPackConfigs(List<String> policyPackConfigs) {
            this.policyPackConfigs = policyPackConfigs;
            return (B) this;
        }

        /**
         * Optional callback which is invoked whenever StandardOutput is written into.
         *
         * @param onStandardOutput the callback
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B onStandardOutput(Consumer<String> onStandardOutput) {
            this.onStandardOutput = onStandardOutput;
            return (B) this;
        }

        /**
         * Optional callback which is invoked whenever StandardError is written into.
         *
         * @param onStandardError the callback
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B onStandardError(Consumer<String> onStandardError) {
            this.onStandardError = onStandardError;
            return (B) this;
        }

        /**
         * Optional callback which is invoked with the engine events.
         *
         * @param onEvent the callback
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B onEvent(Consumer<EngineEvent> onEvent) {
            this.onEvent = onEvent;
            return (B) this;
        }

        /**
         * Colorize output. Choices are: always, never, raw, auto (default "auto")
         *
         * @param color the colorize output setting
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B color(String color) {
            this.color = color;
            return (B) this;
        }

        /**
         * Flow log settings to child processes (like plugins)
         *
         * @param logFlow the flow log settings
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B logFlow(boolean logFlow) {
            this.logFlow = logFlow;
            return (B) this;
        }

        /**
         * Enable verbose logging (e.g., v=3); anything &gt;3 is very verbose
         *
         * @param logVerbosity the log verbosity
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B logVerbosity(Integer logVerbosity) {
            this.logVerbosity = logVerbosity;
            return (B) this;
        }

        /**
         * Log to stderr instead of to files
         *
         * @param logToStdErr the log to stderr setting
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B logToStdErr(boolean logToStdErr) {
            this.logToStdErr = logToStdErr;
            return (B) this;
        }

        /**
         * Emit tracing to the specified endpoint. Use the file: scheme to write tracing
         * data to a local file
         *
         * @param tracing the tracing endpoint
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B tracing(String tracing) {
            this.tracing = tracing;
            return (B) this;
        }

        /**
         * Print detailed debugging output during resource operations
         *
         * @param debug the debug setting
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B debug(boolean debug) {
            this.debug = debug;
            return (B) this;
        }

        /**
         * Format standard output as JSON not text.
         *
         * @param json the JSON output setting
         * @return the builder
         */
        @SuppressWarnings("unchecked")
        public B json(boolean json) {
            this.json = json;
            return (B) this;
        }
    }
}
