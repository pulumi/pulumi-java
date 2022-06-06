package com.pulumi.deployment.internal;

import com.pulumi.core.internal.annotations.InternalUse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A task {@link CompletableFuture} runner.
 */
@InternalUse
public interface Runner {

    /**
     * Exit code indicating a success.
     */
    int ProcessExitedSuccessfully = 0;

    /**
     * Exit code indicating a failure before user visible logging was possible.
     */
    int ProcessExitedBeforeLoggingUserActionableMessage = 1;

    /**
     * Exit code indicating a failure that was properly logged.
     * <p>
     * Keep track if we already logged the information about an unhandled error to the user.
     * If so, we end with a different exit code. The language host recognizes this and will not print
     * any further messages to the user since we already took care of it.
     * <p>
     * 32 was picked to be very unlikely to collide with any other error codes.
     */
    int ProcessExitedAfterLoggingUserActionableMessage = 32;

    /**
     * Register a task to run asynchronously in a fire-and-forget manner.
     *
     * @param description the task description
     * @param task        the task future
     * @param <T>         the task type
     */
    @InternalUse
    <T> void registerTask(String description, CompletableFuture<T> task);

    /**
     * Run the callback and start the main loop (handling any errors)
     * and go through all the registered tasks in a sequence:
     * <ol>
     *     <li>run the callback handling any errors</li>
     *     <li>start the runner loop</li>
     *     <li>run until all registered futures in the list complete successfully, or any future throws an exception</li>
     * </ol>
     *
     * @param callback a callback to call in the context of the error handler
     * @param <T>      the type of the result
     * @return a {@link Result<T>} with a value, exceptions and an exit status code
     * @see #registerTask(String, CompletableFuture)
     */
    @InternalUse
    <T> CompletableFuture<Result<T>> runAsync(Supplier<T> callback);

    /**
     * Runner's result containing the result value and/or any exceptions, and the process exit code.
     * @param <T> the type of the result value
     */
    @InternalUse
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    class Result<T> {
        private final int exitCode;
        private final List<Exception> exceptions;
        private final Optional<T> result;

        protected Result(
                int exitCode,
                List<Exception> exceptions,
                Optional<T> result
        ) {
            this.exitCode = exitCode;
            this.exceptions = requireNonNull(exceptions);
            this.result = requireNonNull(result);
        }

        /**
         * @return exit status code, one of: {@link #ProcessExitedSuccessfully},
         * {@link #ProcessExitedAfterLoggingUserActionableMessage},
         * {@link #ProcessExitedBeforeLoggingUserActionableMessage}
         */
        public int exitCode() {
            return exitCode;
        }

        /**
         * @return all exceptions swallowed during the execution of all tasks
         */
        public List<Exception> exceptions() {
            return exceptions;
        }

        /**
         * @return the result of the callback execution
         */
        public Optional<T> result() {
            return this.result;
        }
    }
}
