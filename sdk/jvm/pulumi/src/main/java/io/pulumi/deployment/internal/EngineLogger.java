package io.pulumi.deployment.internal;

import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public interface EngineLogger {
    int getErrorCount();

    boolean hasLoggedErrors();

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    default CompletableFuture<Void> debugAsync(String message) {
        return debugAsync(message, null, null, null);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    default CompletableFuture<Void> infoAsync(String message) {
        return infoAsync(message, null);
    }

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    default CompletableFuture<Void> warnAsync(String message) {
        return warnAsync(message, null);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling this method to stop the Pulumi program.
     */
    default CompletableFuture<Void> errorAsync(String message) {
        return errorAsync(message, null);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    default CompletableFuture<Void> debugAsync(String message, @Nullable Resource resource) {
        return debugAsync(message, resource, null, null);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    default CompletableFuture<Void> infoAsync(String message, @Nullable Resource resource) {
        return infoAsync(message, resource, null, null);
    }

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    default CompletableFuture<Void> warnAsync(String message, @Nullable Resource resource) {
        return warnAsync(message, resource, null, null);
    }

    /**
     * Logs a fatal condition. Consider raising an exception after calling this method to stop the Pulumi program.
     */
    default CompletableFuture<Void> errorAsync(String message, @Nullable Resource resource) {
        return errorAsync(message, resource, null, null);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    CompletableFuture<Void> debugAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral);

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    CompletableFuture<Void> infoAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral);

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    CompletableFuture<Void> warnAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral);

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling this method to stop the Pulumi program.
     */
    CompletableFuture<Void> errorAsync(String message, @Nullable Resource resource, @Nullable Integer streamId, @Nullable Boolean ephemeral);
}
