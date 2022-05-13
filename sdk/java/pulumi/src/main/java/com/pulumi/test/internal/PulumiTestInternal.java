package com.pulumi.test.internal;

import com.pulumi.Log;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.test.TestOptions;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.mock.EmptyMocks;
import com.pulumi.test.mock.MonitorMocks;

import static java.util.Objects.requireNonNull;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public class PulumiTestInternal extends PulumiInternal implements PulumiTest {

    private final Log log;

    public PulumiTestInternal(Runner runner, Log log, ContextInternal stackContext) {
        super(runner, stackContext);
        this.log = requireNonNull(log);
    }

    public Runner runner() {
        return this.runner;
    }

    /**
     * @return a {@link PulumiTestInternal} {@link PulumiTestInternal.Builder} with default options
     */
    public static PulumiTestInternal.Builder withDefaults() {
        return withOptions(new TestOptions());
    }

    /**
     * @param options the test options to use
     * @return a {@link PulumiTestInternal} {@link PulumiTestInternal.Builder} with the given options
     */
    public static PulumiTestInternal.Builder withOptions(TestOptions options) {
        return new PulumiTestInternal.Builder(options);
    }

    /**
     * @return return the logging interface used by the test
     */
    public Log log() {
        return this.log;
    }

    /**
     * The {@link PulumiTestInternal} builder.
     */
    @SuppressWarnings("unused")
    public static final class Builder implements PulumiTest.Builder {

        private final TestOptions options;
        private MonitorMocks mocks;

        /**
         * @param options the test options to use
         */
        public Builder(TestOptions options) {
            this.options = requireNonNull(options);
        }

        /**
         * {@inheritDoc}
         */
        public PulumiTestInternal.Builder mocks(MonitorMocks mocks) {
            this.mocks = requireNonNull(mocks);
            return this;
        }

        @Override
        public PulumiTestInternal build() {
            if (this.mocks == null) {
                this.mocks = new EmptyMocks();
            }
            var mock = DeploymentTests.DeploymentMockBuilder.builder()
                    .setOptions(this.options)
                    .setMocks(this.mocks)
                    .setMockGlobalInstance();
            var ctx = PulumiInternal.contextFromDeployment(mock.deployment);
            return new PulumiTestInternal(mock.runner, mock.log, ctx);
        }

        // TODO: port com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder
    }
}
