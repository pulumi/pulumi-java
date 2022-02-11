package io.pulumi;

import io.pulumi.deployment.internal.CountingLogger;
import io.pulumi.deployment.internal.EngineLogger;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Logging functions that can be called from a Java application that will be logged to the {@code Pulumi} log stream.
 * These events will be printed in the terminal while the Pulumi app runs,
 * and will be available from the CLI and Web console afterwards.
 */
@ParametersAreNonnullByDefault
public class Log implements CountingLogger {

    private final EngineLogger logger;
    private final boolean excessiveDebugOutput;

    public Log(EngineLogger logger) {
        this(logger, false);
    }

    public Log(EngineLogger logger, boolean excessiveDebugOutput) {
        this.logger = Objects.requireNonNull(logger);
        this.excessiveDebugOutput = excessiveDebugOutput;
    }

    public void excessive(String message, Object... args) {
        if (excessiveDebugOutput) {
            debug(String.format(message, args));
        }
    }

    public void debugOrExcessive(String debugMessage, String excessiveMessage) {
        debug(debugMessage + (excessiveDebugOutput ? excessiveMessage : ""));
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public void debug(String message) {
        this.logger.debugAsync(message);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public void debug(String message, @Nullable Resource resource) {
        this.logger.debugAsync(message, resource);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public void debug(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        this.logger.debugAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public void info(String message) {
        this.logger.infoAsync(message);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public void info(String message, @Nullable Resource resource) {
        this.logger.infoAsync(message, resource);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public void info(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        this.logger.infoAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public void warn(String message) {
        this.logger.warnAsync(message);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public void warn(String message, @Nullable Resource resource) {
        this.logger.warnAsync(message, resource);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public void warn(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        this.logger.warnAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public void error(String message) {
        this.logger.errorAsync(message);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public void error(String message, @Nullable Resource resource) {
        this.logger.errorAsync(message, resource);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public void error(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        this.logger.errorAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public void exception(Exception exception) {
        this.error(exception.getLocalizedMessage());
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public void exception(Exception exception, @Nullable Resource resource) {
        this.error(exception.getLocalizedMessage(), resource);
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public void exception(Exception exception, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        this.error(exception.getLocalizedMessage(), resource, streamId, ephemeral);
    }

    @Override
    public int getErrorCount() {
        return this.logger.getErrorCount();
    }

    @Override
    public boolean hasLoggedErrors() {
        return this.logger.hasLoggedErrors();
    }
}
