package com.pulumi.test;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class EmptyMocks implements Mocks {

    @Override
    public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
        throw new IllegalArgumentException(
                "EmptyMocks have not implementation, use setMocks with a correct implementation. "
                        + format("Unknown resource '%s'", args.type)
        );
    }
}