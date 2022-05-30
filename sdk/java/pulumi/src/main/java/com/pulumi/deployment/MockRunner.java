package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.resources.Stack;
import com.pulumi.resources.StackOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MockRunner implements Runner {

    @Override
    public List<Exception> getSwallowedExceptions() {
        return null;
    }

    @Override
    public <T> void registerTask(String description, CompletableFuture<T> task) {
        // Empty
    }

    @Override
    public CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback) {
        return null;
    }

    @Override
    public CompletableFuture<Integer> runAsyncFuture(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options) {
        return null;
    }

    @Override
    public <T extends Stack> CompletableFuture<Integer> runAsync(Supplier<T> stackFactory) {
        return null;
    }
}
