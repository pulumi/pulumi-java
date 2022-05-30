package com.pulumi.deployment;

import com.pulumi.core.Tuples;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class EmptyMocks implements Mocks {

    @Override
    public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
        throw new IllegalArgumentException(
                "EmptyMocks have not implementation, use setMocks with a correct implementation. "
                        + format("Unknown resource '%s'", args.type)
        );
    }

    @Override
    public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
        return CompletableFuture.completedFuture(null);
    }
}