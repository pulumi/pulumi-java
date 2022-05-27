package com.pulumi.deployment;

import com.pulumi.deployment.internal.Runner;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MockRunner implements Runner {

    @Override
    public <T> void registerTask(String description, CompletableFuture<T> task) {
        // Empty
    }

    @Override
    public <T> CompletableFuture<Result<T>> runAsync(Supplier<T> callback) {
        return null;
    }
}
