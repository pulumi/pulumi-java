package io.pulumi.deployment.internal;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.resources.StackOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Runner {
    List<Exception> getSwallowedExceptions();

    <T> void registerTask(String description, CompletableFuture<T> task);

    CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback);

    CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options);

    <T extends Stack> CompletableFuture<Integer> runAsync(Supplier<T> stackFactory);
}
