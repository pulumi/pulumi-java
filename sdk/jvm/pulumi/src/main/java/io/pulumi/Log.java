package io.pulumi;

import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;

/**
 * Logging functions that can be called from a .NET application that will be logged to the {@code Pulumi} log stream.
 * These events will be printed in the terminal while the Pulumi app runs,
 * and will be available from the CLI and Web console afterwards.
 */
public class Log {

    private Log() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public static void debug(String message) {
        DeploymentInternal.getInstance().getLogger().debugAsync(message);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public static void debug(String message, @Nullable Resource resource) {
        DeploymentInternal.getInstance().getLogger().debugAsync(message, resource);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    public static void debug(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        DeploymentInternal.getInstance().getLogger().debugAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public static void info(String message) {
        DeploymentInternal.getInstance().getLogger().infoAsync(message);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public static void info(String message, @Nullable Resource resource) {
        DeploymentInternal.getInstance().getLogger().infoAsync(message, resource);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource
     * operations.
     */
    public static void info(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        DeploymentInternal.getInstance().getLogger().infoAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public static void warn(String message) {
        DeploymentInternal.getInstance().getLogger().warnAsync(message);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public static void warn(String message, @Nullable Resource resource) {
        DeploymentInternal.getInstance().getLogger().warnAsync(message, resource);
    }

    /**
     * Logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    public static void warn(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        DeploymentInternal.getInstance().getLogger().warnAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public static void error(String message) {
        DeploymentInternal.getInstance().getLogger().errorAsync(message);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public static void error(String message, @Nullable Resource resource) {
        DeploymentInternal.getInstance().getLogger().errorAsync(message, resource);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling Error to stop the Pulumi program.
     */
    public static void error(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        DeploymentInternal.getInstance().getLogger().errorAsync(message, resource, streamId, ephemeral);
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public static void exception(Exception exception) {
        error(exception.getLocalizedMessage());
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public static void exception(Exception exception, @Nullable Resource resource) {
        error(exception.getLocalizedMessage(), resource);
    }

    /**
     * Logs an exception. Consider raising the exception after calling this method to stop the Pulumi program.
     */
    public static void exception(Exception exception, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral) {
        error(exception.getLocalizedMessage(), resource, streamId, ephemeral);
    }
}
