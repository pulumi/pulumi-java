package com.pulumi.test;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;

import javax.annotation.Nullable;
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


    /**
     * Extract the value from an {@link Output} blocking.
     * @param output the {@link Output} to extract value from
     * @return the underlying value of the given {@link Output}
     */
    @Nullable
    static <T> T extractValue(Output<T> output) {
        return extractValueAsync(output).join();
    }

    /**
     * Extract the future value from an {@link Output} asynchronously.
     * @param output the {@link Output} to extract future value from
     * @return the underlying future value of the given {@link Output}
     */
    static <T> CompletableFuture<T> extractValueAsync(Output<T> output) {
        return Internal.of(output).getValueNullable();
    }
}
