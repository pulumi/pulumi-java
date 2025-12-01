package com.pulumi.test;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Represents a {@link Mocks} implementation that throws for all resource mocks.
 */
public class EmptyMocks implements Mocks {

    /**
     * Throws an exception indicating that no resource mocks are implemented.
     *
     * @param args Resource arguments.
     * @return This method never returns normally.
     * @throws IllegalArgumentException always thrown to indicate missing mocks.
     */
    @Override
    public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
        throw new IllegalArgumentException(
                "EmptyMocks have not implementation, use setMocks with a correct implementation. "
                        + format("Unknown resource '%s'", args.type)
        );
    }
}