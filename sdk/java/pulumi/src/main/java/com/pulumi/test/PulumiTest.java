package com.pulumi.test;

import com.pulumi.Context;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.Monitor;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.mock.MonitorMocks;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public interface PulumiTest {

    /**
     * Run a Pulumi test stack callback asynchronously and return an exit code.
     *
     * @param stack the stack to run in Pulumi test runtime
     * @return a future exit code from Pulumi test runtime after running the stack
     */
    CompletableFuture<Integer> runAsync(Consumer<Context> stack);

    /**
     * Run a Pulumi test stack callback asynchronously and return {@link TestResult}.
     *
     * @param stack the stack to run in Pulumi test runtime
     * @return a future {@link TestResult} from Pulumi test runtime after running the stack
     */
    CompletableFuture<TestResult> runTestAsync(Consumer<Context> stack);

    /**
     * @return a {@link PulumiTest} {@link Builder} with default options
     */
    static PulumiTest.Builder withDefaults() {
        return withOptions(new TestOptions());
    }

    /**
     * @param options the test options to use
     * @return a {@link PulumiTest} {@link Builder} with the given options
     */
    static PulumiTest.Builder withOptions(TestOptions options) {
        return PulumiTestInternal.withOptions(options);
    }

    /**
     * The {@link PulumiTest} builder.
     */
    interface Builder {

        /**
         * @param mocks the {@link MonitorMocks} to use in the {@link Monitor} mock
         * @return this {@link Builder}
         */
        Builder mocks(MonitorMocks mocks);

        /**
         * Configure the test with a real task runner.
         * Needed for all tests that use {@link PulumiTest#runAsync(Consumer)}
         * or {@link PulumiTest#runTestAsync(Consumer)}
         *
         * @return this {@link Builder}
         */
        Builder useRealRunner();

        /**
         * @return a {@link PulumiTest} instance created from this {@link Builder}
         */
        PulumiTest build();
    }

    static void cleanup() {
        // ensure we don't get the error:
        //   java.lang.IllegalStateException: Deployment.getInstance should only be set once at the beginning of a 'run' call.
        DeploymentImpl.internalUnsafeDestroyInstance(); // FIXME: how to avoid this?
    }
}
