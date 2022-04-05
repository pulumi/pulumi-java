package io.pulumi;

import io.pulumi.core.Output;
import io.pulumi.deployment.Deployment;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Pulumi {

    static Integer run(Supplier<Map<String, Output<?>>> callback) {
        return runAsync(callback).join();
    }

    static CompletableFuture<Integer> runAsync(Supplier<Map<String, Output<?>>> callback) {
        return Deployment.runAsync(callback);
    }

    static <S extends Stack> Integer runStack(Supplier<S> stackFactory) {
        return runStackAsync(stackFactory).join();
    }

    static <S extends Stack> CompletableFuture<Integer> runStackAsync(Supplier<S> stackFactory) {
        return Deployment.runAsyncStack(stackFactory);
    }

}