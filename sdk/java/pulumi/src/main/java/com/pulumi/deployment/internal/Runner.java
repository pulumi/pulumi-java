package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.resources.Stack;
import com.pulumi.resources.StackOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
     *
     * Keep track if we already logged the information about an unhandled error to the user.
     * If so, we end with a different exit code. The language host recognizes this and will not print
     * any further messages to the user since we already took care of it.
     *
     * 32 was picked to be very unlikely to collide with any other error codes.
     */
    int ProcessExitedAfterLoggingUserActionableMessage = 32;

    /**
     * @return exceptions encountered handled by the runner
     */
    List<Exception> getSwallowedExceptions();

    /**
     * Register a task to run asynchronously in a fire-and-forget manner.
     * @param description the task description
     * @param task the task future
     * @param <T> the task type
     */
    <T> void registerTask(String description, CompletableFuture<T> task);

    CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback);

    CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options);

    <T extends Stack> CompletableFuture<Integer> runAsync(Supplier<T> stackFactory);
}
