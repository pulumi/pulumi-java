package com.pulumi.test;

import com.pulumi.Context;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public interface PulumiTest {

    /**
     * Run a Pulumi test stack callback asynchronously and return {@link TestResult}.
     *
     * @param stack the stack to run in Pulumi test runtime
     * @return a future {@link TestResult} from Pulumi test runtime after running the stack
     */
    CompletableFuture<TestResult> runTestAsync(Consumer<Context> stack);

}
