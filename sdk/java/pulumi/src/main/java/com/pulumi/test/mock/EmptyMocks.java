package com.pulumi.test.mock;

import com.pulumi.core.Tuples;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class EmptyMocks implements Mocks {

    @Override
    public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
        requireNonNull(args.type);
        switch (args.type) {
            default:
                throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }
    }

    @Override
    public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
        return CompletableFuture.completedFuture(null);
    }
}