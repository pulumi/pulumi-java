package com.pulumi.automation;

public class GlobalOptions {

    protected Color color;
    protected boolean logFlow;
    protected int logVerbosity;
    protected boolean logToStdErr;
    protected String tracing;
    protected boolean debug;
    protected boolean json;

    protected GlobalOptions() { /* empty */ }

    /**
     * Colorize output
     *
     * @return output colorization option
     */
    public Color color() {
        return color;
    }

    /**
     * Flow log settings to child processes (like plugins)
     *
     * @return whether the flow log setting is active
     */
    public boolean logFlow() {
        return logFlow;
    }

    /**
     * Enable verbose logging (e.g., v=3); anything greater than 3 is very verbose
     *
     * @return the verbosity level
     */
    public int logVerbosity() {
        return logVerbosity;
    }

    /**
     * Log to stderr instead of to files
     *
     * @return whether the logging to stderr is active
     */
    public boolean logToStdErr() {
        return logToStdErr;
    }

    /**
     * Emit tracing to the specified endpoint. Use the file: scheme to write tracing data to a local file
     *
     * @return the tracing endpoint
     */
    public String tracing() {
        return tracing;
    }

    /**
     * Print detailed debugging output during resource operations
     *
     * @return whether debugging output is active
     */
    public boolean debug() {
        return debug;
    }

    /**
     * Format standard output as JSON not text.
     *
     * @return whether JSON output is active
     */
    public boolean json() {
        return json;
    }

    /**
     * Colorization options
     */
    public enum Color {
        Always,
        Never,
        Raw,
        Auto
    }

    protected static abstract class Builder<T extends GlobalOptions, B extends GlobalOptions.Builder<T, B>> {

        protected final T options;

        protected Builder(T options) {
            this.options = options;
        }

        public B color(Color color) {
            this.options.color = color;
            //noinspection unchecked
            return (B) this;
        }

        public B logFlow(boolean logFlow) {
            this.options.logFlow = logFlow;
            //noinspection unchecked
            return (B) this;
        }

        public B logVerbosity(int logVerbosity) {
            this.options.logVerbosity = logVerbosity;
            //noinspection unchecked
            return (B) this;
        }

        public B logToStdErr(boolean logToStdErr) {
            this.options.logToStdErr = logToStdErr;
            //noinspection unchecked
            return (B) this;
        }

        public B tracing(String tracing) {
            this.options.tracing = tracing;
            //noinspection unchecked
            return (B) this;
        }

        public B debug(boolean debug) {
            this.options.debug = debug;
            //noinspection unchecked
            return (B) this;
        }

        /**
         * @see GlobalOptions#json()
         * @param json if true JSON output is active
         * @return the {@link Builder} instance
         */
        public B json(boolean json) {
            this.options.json = json;
            //noinspection unchecked
            return (B) this;
        }
    }
}
