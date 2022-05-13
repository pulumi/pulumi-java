package com.pulumi.test.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.pulumi.Context;
import com.pulumi.Log;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.Invoke;
import com.pulumi.test.mock.MockEngine;
import com.pulumi.test.mock.MockMonitor;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.Engine;
import com.pulumi.deployment.internal.Monitor;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.TestOptions;
import com.pulumi.test.TestResult;
import com.pulumi.test.mock.EmptyMocks;
import com.pulumi.test.mock.MonitorMocks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.pulumi.deployment.internal.DeploymentTests.defaultLogger;
import static java.util.Objects.requireNonNull;

/**
 * Provides a test Pulumi runner and exposes various internals for the testing purposes.
 */
public class PulumiTestInternal extends PulumiInternal implements PulumiTest {

    // TODO: when refactoring Deployment, move those fields to PulumiInternal
    private final Log log;
    private final Engine engine;
    private final Monitor monitor;

    private final Invoke invoke;

    public PulumiTestInternal(
            Runner runner,
            Engine engine,
            Monitor monitor,
            Log log,
            Invoke invoke,
            ContextInternal stackContext
    ) {
        super(runner, stackContext);
        this.log = requireNonNull(log);
        this.engine = requireNonNull(engine);
        this.monitor = requireNonNull(monitor);
        this.invoke = requireNonNull(invoke);
    }

    /**
     * @return return the {@link Runner} used by the test
     */
    public Runner runner() {
        return this.runner;
    }

    /**
     * @return return the {@link Engine} used by the test
     */
    public Engine engine() {
        return this.engine;
    }

    /**
     * @return return the {@link Monitor} used by the test
     */
    public Monitor monitor() {
        return this.monitor;
    }

    /**
     * @return return the {@link Invoke} used by the test
     */
    public Invoke invoke() {
        return this.invoke;
    }

    /**
     * @return return the {@link Log} used by the test
     */
    public Log log() {
        return this.log;
    }

    @Override
    public CompletableFuture<TestResult> runTestAsync(Consumer<Context> stackCallback) {
        if (!(this.engine instanceof MockEngine)) {
            throw new IllegalStateException("Expected 'engine' to be an instanceof MockEngine");
        }
        if (!(this.monitor instanceof MockMonitor)) {
            throw new IllegalStateException("Expected 'monitor' to be an instanceof MockMonitor");
        }
        var mockEngine = (MockEngine) this.engine;
        var mockMonitor = (MockMonitor) this.monitor;
        return runAsyncResult(userProgram(stackCallback)).thenApply(
                result -> new TestResult(
                        result.exitCode(),
                        result.exceptions(),
                        ImmutableList.copyOf(mockMonitor.resources),
                        ImmutableList.copyOf(mockEngine.getErrors()),
                        result.result().orElseThrow()
                )
        );
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
     * The {@link PulumiTestInternal} builder.
     */
    @SuppressWarnings("unused")
    public static final class Builder implements PulumiTest.Builder {

        private final TestOptions options;
        private MonitorMocks mocks;
        private Logger standardLogger;
        private boolean useRealRunner;
        private DeploymentImpl.Config internalConfig;

        /**
         * @param options the test options to use
         */
        public Builder(TestOptions options) {
            this.options = requireNonNull(options);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PulumiTestInternal.Builder mocks(MonitorMocks mocks) {
            this.mocks = requireNonNull(mocks);
            return this;
        }

        public PulumiTestInternal.Builder standardLogger(Logger standardLogger) {
            this.standardLogger = requireNonNull(standardLogger);
            return this;
        }

        /**
         * Set configuration to use for this test
         *
         * @param allConfig        the configuration key-value map
         * @param configSecretKeys the secret key names
         * @return this {@link PulumiTest.Builder}
         */
        public PulumiTest.Builder config(Map<String, String> allConfig, Set<String> configSecretKeys) {
            return internalConfig(new DeploymentImpl.Config(
                    ImmutableMap.copyOf(allConfig), ImmutableSet.copyOf(configSecretKeys)
            ));
        }

        /**
         * Set configuration to use for this test
         *
         * @param allConfig the configuration key-value map
         * @return this {@link PulumiTest.Builder}
         */
        public PulumiTest.Builder config(Map<String, String> allConfig) {
            return internalConfig(new DeploymentImpl.Config(
                    ImmutableMap.copyOf(allConfig), ImmutableSet.of()
            ));
        }

        private PulumiTest.Builder internalConfig(DeploymentImpl.Config internalConfig) {
            this.internalConfig = requireNonNull(internalConfig);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PulumiTestInternal.Builder useRealRunner() {
            this.useRealRunner = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PulumiTestInternal build() {
            if (this.standardLogger == null) {
                this.standardLogger = defaultLogger();
            }
            if (this.mocks == null) {
                this.mocks = new EmptyMocks();
            }
            if (this.internalConfig == null) {
                this.internalConfig = new DeploymentImpl.Config(ImmutableMap.of(), ImmutableSet.of());
            }
            var mockBuilder = DeploymentTests.DeploymentMockBuilder.builder()
                    .setConfig(this.internalConfig)
                    .setOptions(this.options)
                    .setMocks(this.mocks)
                    .setStandardLogger(this.standardLogger);
            final DeploymentTests.DeploymentMock mock;
            if (this.useRealRunner) {
                mock = mockBuilder.setSpyGlobalInstance();
            } else {
                mock = mockBuilder.setMockGlobalInstance();
            }
            var ctx = PulumiInternal.contextFromDeployment(mock.deployment);
            return new PulumiTestInternal(
                    mock.runner, mock.engine, mock.monitor, mock.log, mock.deployment, ctx
            );
        }

        // TODO: port com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder
    }
}
