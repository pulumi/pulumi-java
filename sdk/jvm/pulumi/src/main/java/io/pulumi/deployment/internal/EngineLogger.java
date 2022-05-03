package io.pulumi.deployment.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public interface EngineLogger extends CountingLogger {

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> debugAsync(String message) {
        return debugAsync(message, null, null, null);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> infoAsync(String message) {
        return infoAsync(message, null);
    }

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> warnAsync(String message) {
        return warnAsync(message, null);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling this method to stop the Pulumi program.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> errorAsync(String message) {
        return errorAsync(message, null);
    }

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> debugAsync(String message, @Nullable Resource resource) {
        return debugAsync(message, resource, null, null);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> infoAsync(String message, @Nullable Resource resource) {
        return infoAsync(message, resource, null, null);
    }

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> warnAsync(String message, @Nullable Resource resource) {
        return warnAsync(message, resource, null, null);
    }

    /**
     * Logs a fatal condition. Consider raising an exception after calling this method to stop the Pulumi program.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> errorAsync(String message, @Nullable Resource resource) {
        return errorAsync(message, resource, null, null);
    }

    @CanIgnoreReturnValue
    CompletableFuture<Void> logAsync(Level level,
                                     String message,
                                     @Nullable Resource resource,
                                     @Nullable Integer streamId,
                                     @Nullable Boolean ephemeral);

    /**
     * Logs a debug-level message that is generally hidden from end-users.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> debugAsync(String message,
                                               @Nullable Resource resource,
                                               @Nullable Integer streamId,
                                               @Nullable Boolean ephemeral) {
        return logAsync(Level.FINE, message, resource, streamId, ephemeral);
    }

    /**
     * Logs an informational message that is generally printed to stdout during resource operations.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> infoAsync(String message,
                                              @Nullable Resource resource,
                                              @Nullable Integer streamId,
                                              @Nullable Boolean ephemeral) {
        return logAsync(Level.INFO, message, resource, streamId, ephemeral);
    }

    /**
     * Warn logs a warning to indicate that something went wrong, but not catastrophically so.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> warnAsync(String message,
                                              @Nullable Resource resource,
                                              @Nullable Integer streamId,
                                              @Nullable Boolean ephemeral) {
        return logAsync(Level.WARNING, message, resource, streamId, ephemeral);
    }

    /**
     * Logs a fatal condition. Consider raising an exception
     * after calling this method to stop the Pulumi program.
     */
    @CanIgnoreReturnValue
    default CompletableFuture<Void> errorAsync(String message,
                                               @Nullable Resource resource,
                                               @Nullable Integer streamId,
                                               @Nullable Boolean ephemeral) {
        return logAsync(Level.SEVERE, message, resource, streamId, ephemeral);
    }

    static EngineLogger ignore() {
        return new NullLogger();
    }

    class NullLogger implements EngineLogger {
        @Override
        public CompletableFuture<Void> logAsync(Level level,
                                                String message,
                                                @Nullable Resource resource,
                                                @Nullable Integer streamId,
                                                @Nullable Boolean ephemeral) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public int getErrorCount() {
            return 0;
        }

        @Override
        public boolean hasLoggedErrors() {
            return false;
        }
    }
}
