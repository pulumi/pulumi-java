package com.pulumi.test;

import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.Monitor;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.mock.MonitorMocks;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public interface PulumiTest {

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
         * @return a {@link PulumiTest} instance created from this {@link Builder}
         */
        PulumiTest build();
    }

    static void cleanup() {
        DeploymentTests.cleanupDeploymentMocks();
    }
}
