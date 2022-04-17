package com.pulumi.context;

import com.pulumi.Config;

/**
 * Provides {@link Config} in current context.
 */
public interface LoggingContext {

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    void debug(String message);

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    void debug(String format, Object... args);

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    void info(String message);

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    void info(String format, Object... args);

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    void warn(String message);

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    void warn(String format, Object... args);

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    void error(String message);

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    void error(String format, Object... args);
}
