package com.pulumi.test;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public interface PulumiTest {

    /**
     * Run a Pulumi test stack callback, wait for result and return {@link TestResult}.
     *
     * @param stack the stack to run in Pulumi test runtime
     * @return a {@link TestResult} from Pulumi test runtime after running the stack
     * @see #runTestAsync(Consumer)
     * @see #withOptions(TestOptions)
     * @see #withMocks(Mocks)
     */
    static TestResult runTest(Consumer<Context> stack) {
        return withOptions(TestOptions.Empty).runTest(stack);
    }

    /**
     * Run a Pulumi test stack callback asynchronously and return {@link TestResult}.
     *
     * @param stack the stack to run in Pulumi test runtime
     * @return a future {@link TestResult} from Pulumi test runtime after running the stack
     * @see #runTest(Consumer)
     * @see #withOptions(TestOptions)
     * @see #withMocks(Mocks)
     */
    static CompletableFuture<TestResult> runTestAsync(Consumer<Context> stack) {
        return withOptions(TestOptions.Empty).runTestAsync(stack);
    }

    /**
     * Use {@link TestOptions} in this test.
     *
     * @param options the {@link TestOptions} to use
     * @return a Pulumi test program entrypoint with given {@link TestOptions}
     * @see #runTest(Consumer)
     * @see #runTestAsync(Consumer)
     * @see #withMocks(Mocks)
     */
    static PulumiTest.API withOptions(TestOptions options) {
        return new PulumiTestInternal.APIInternal().withOptions(options);
    }

    /**
     * Use {@link Mocks} in this test.
     *
     * @param mocks the {@link com.pulumi.deployment.internal.Monitor} mocks to use
     * @return a Pulumi test program entrypoint with given {@link TestOptions}
     * @see #runTest(Consumer)
     * @see #runTestAsync(Consumer)
     * @see #withOptions(TestOptions)
     */
    static PulumiTest.API withMocks(Mocks mocks) {
        return new PulumiTestInternal.APIInternal().withMocks(mocks);
    }

    /**
     * Cleanup internal test state. <b>Must be called</b> after every run of a Pulumi test program.
     */
    static void cleanup() {
        PulumiTestInternal.cleanup();
    }

    /**
     * Extract the value from an {@link Output} blocking.
     *
     * @param output the {@link Output} to extract value from
     * @return the underlying value of the given {@link Output}
     */
    @Nullable
    static <T> T extractValue(Output<T> output) {
        return extractValueAsync(output).join();
    }

    /**
     * Extract the future value from an {@link Output} asynchronously.
     *
     * @param output the {@link Output} to extract future value from
     * @return the underlying future value of the given {@link Output}
     */
    static <T> CompletableFuture<T> extractValueAsync(Output<T> output) {
        return Internal.of(output).getValueNullable();
    }

    /**
     * Pulumi test entrypoint operations.
     */
    interface API {

        /**
         * Use {@link TestOptions} in this test.
         *
         * @param options the {@link TestOptions} to use
         * @return a Pulumi test program entrypoint with given {@link TestOptions}
         * @see #runTest(Consumer)
         * @see #runTestAsync(Consumer)
         * @see #withMocks(Mocks)
         */
        API withOptions(TestOptions options);

        /**
         * Use {@link Mocks} in this test.
         *
         * @param mocks the {@link com.pulumi.deployment.internal.Monitor} mocks to use
         * @return a Pulumi test program entrypoint with given {@link TestOptions}
         * @see #runTest(Consumer)
         * @see #runTestAsync(Consumer)
         * @see #withOptions(TestOptions)
         */
        API withMocks(Mocks mocks);

        /**
         * Run a Pulumi test stack callback, wait for result and return {@link TestResult}.
         *
         * @param stack the stack to run in Pulumi test runtime
         * @return a {@link TestResult} from Pulumi test runtime after running the stack
         */
        TestResult runTest(Consumer<Context> stack);

        /**
         * Run a Pulumi test stack callback asynchronously and return {@link TestResult}.
         *
         * @param stack the stack to run in Pulumi test runtime
         * @return a future {@link TestResult} from Pulumi test runtime after running the stack
         */
        CompletableFuture<TestResult> runTestAsync(Consumer<Context> stack);
    }
}
