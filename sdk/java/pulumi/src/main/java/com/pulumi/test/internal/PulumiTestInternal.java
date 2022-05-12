package com.pulumi.test.internal;

import com.pulumi.context.internal.ContextInternal;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.mock.EmptyMocks;

import static java.util.Objects.requireNonNull;

public class PulumiTestInternal extends PulumiInternal implements PulumiTest {

    public PulumiTestInternal(Runner runner, ContextInternal stackContext) {
        super(runner, stackContext);
    }

    public Runner runner() {
        return this.runner;
    }

    public static Builder builder(TestOptions options) {
        return new Builder(options);
    }

    /**
     * The {@link PulumiTestInternal} builder.
     */
    @SuppressWarnings("unused")
    public static final class Builder implements PulumiTest.Builder {

        private final TestOptions options;

        /**
         * @param options the test options to use
         */
        public Builder(TestOptions options) {
            this.options = requireNonNull(options);
        }

        @Override
        public PulumiTestInternal build() {
            var mock = DeploymentTests.DeploymentMockBuilder.builder()
                    .setOptions(this.options)
                    .setMocks(new EmptyMocks())
                    .setMockGlobalInstance();
            var ctx = PulumiInternal.contextFromDeployment(mock.deployment);
            return new PulumiTestInternal(mock.runner, ctx);
        }

        // TODO: port com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder
    }
}
