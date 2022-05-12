package com.pulumi.test.mock;

import com.pulumi.deployment.MockCallArgs;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class EmptyMocks implements MonitorMocks {

    @Override
    public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
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