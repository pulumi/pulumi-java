package com.pulumi.test;

import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;

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
        return PulumiTestInternal.builder(options);
    }

    /**
     * The {@link PulumiTest} builder.
     */
    interface Builder {

        /**
         * @return a {@link PulumiTest} instance created from this {@link Builder}
         */
        PulumiTest build();
    }

    static void cleanup() {
        DeploymentTests.cleanupDeploymentMocks();
    }
}
