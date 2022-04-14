package io.pulumi.deployment.internal;

import io.pulumi.Stack;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Runner {
    List<Exception> getSwallowedExceptions();

    <T> void registerTask(String description, CompletableFuture<T> task);

    CompletableFuture<Integer> runAsync(Supplier<Stack> stack);
}
