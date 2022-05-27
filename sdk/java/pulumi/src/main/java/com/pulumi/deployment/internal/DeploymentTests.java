package com.pulumi.deployment.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Log;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.deployment.EmptyMocks;
import com.pulumi.deployment.MockEngine;
import com.pulumi.deployment.MockMonitor;
import com.pulumi.deployment.MockRunner;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentImpl.DefaultEngineLogger;
import com.pulumi.test.TestResult;
import com.pulumi.test.internal.PulumiTestInternal;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class DeploymentTests {

    private DeploymentTests() {
        throw new UnsupportedOperationException("static class");
    }

    // TODO: should be possible to merge this with MockDeployment
    public static final class DeploymentMock {
        public final TestOptions options;
        public final Runner runner;
        public final Engine engine;
        public final Monitor monitor;
        public final DeploymentInternal deployment;
        public final DeploymentImpl.Config config;
        public final DeploymentImpl.DeploymentState state;
        public final Logger standardLogger;
        public final EngineLogger logger;
        public final Log log;

        private DeploymentMock(
                TestOptions options,
                Runner runner,
                Engine engine,
                Monitor monitor,
                DeploymentInternal deployment,
                DeploymentImpl.Config config,
                DeploymentImpl.DeploymentState state,
                Logger standardLogger,
                EngineLogger logger,
                Log log
        ) {
            this.options = requireNonNull(options);
            this.runner = requireNonNull(runner);
            this.engine = requireNonNull(engine);
            this.monitor = requireNonNull(monitor);
            this.deployment = requireNonNull(deployment);
            this.config = requireNonNull(config);
            this.state = requireNonNull(state);
            this.standardLogger = requireNonNull(standardLogger);
            this.logger = requireNonNull(logger);
            this.log = requireNonNull(log);
        }

        public void overrideConfig(String key, String value) {
            this.config.setConfig(key, value);
        }

        public void overrideConfig(ImmutableMap<String, String> config, @Nullable Iterable<String> secretKeys) {
            this.config.setAllConfig(config, secretKeys);
        }

        public CompletableFuture<TestResult> runTestAsync(Consumer<Context> stackCallback) {
            if (!(this.engine instanceof MockEngine)) {
                throw new IllegalStateException("Expected engine to be an instanceof MockEngine");
            }
            if (!(this.monitor instanceof MockMonitor)) {
                throw new IllegalStateException("Expected monitor to be an instanceof MockMonitor");
            }
            var mockEngine = (MockEngine) engine;
            var mockMonitor = (MockMonitor) monitor;

            Function<String, Config> configFactory = (name) -> new Config(this.config, name);
            var configContext = new ConfigContextInternal(this.options.getProjectName(), configFactory);
            var loggingContext = new LoggingContextInternal(this.log);
            var outputFactory = new OutputFactory(this.runner);
            var outputsContext = new OutputContextInternal(outputFactory);

            var context = new ContextInternal(
                    this.options.getProjectName(),
                    this.options.getStackName(),
                    loggingContext, configContext, outputsContext
            );
            var pulumi = new PulumiTestInternal(this.runner, mockEngine, mockMonitor, context);
            return pulumi.runTestAsync(stackCallback);
        }
    }

    public static final class DeploymentMockBuilder {

        @Nullable
        private TestOptions options;
        @Nullable
        private Runner runner;
        @Nullable
        private MockMonitor monitor;
        @Nullable
        private DeploymentImpl.Config config;
        @Nullable
        private DeploymentImpl.DeploymentState state;
        @Nullable
        private MockEngine engine;
        @Nullable
        private EngineLogger logger;
        @Nullable
        private Logger standardLogger;
        @Nullable
        private Log log;
        @Nullable
        private Mocks mocks;
        @Nullable
        private Function<DeploymentImpl.DeploymentState, DeploymentInternal> deploymentFactory;

        private DeploymentMockBuilder() { /* Empty */ }

        public static DeploymentMockBuilder builder() {
            return new DeploymentMockBuilder();
        }

        public DeploymentMockBuilder setOptions(TestOptions options) {
            requireNonNull(options);
            this.options = options;
            return this;
        }

        public DeploymentMockBuilder setRunner(Runner runner) {
            requireNonNull(runner);
            this.runner = runner;
            return this;
        }

        public DeploymentMockBuilder setMocks(Mocks mocks) {
            requireNonNull(mocks);
            this.mocks = mocks;
            return this;
        }

        public DeploymentMockBuilder setConfig(DeploymentImpl.Config config) {
            requireNonNull(config);
            this.config = config;
            return this;
        }

        public DeploymentMockBuilder setState(DeploymentImpl.DeploymentState state) {
            requireNonNull(state);
            this.state = state;
            return this;
        }

        public DeploymentMockBuilder setLogger(EngineLogger logger) {
            requireNonNull(logger);
            this.logger = logger;
            return this;
        }

        public DeploymentMockBuilder setStandardLogger(Logger logger) {
            requireNonNull(logger);
            this.standardLogger = logger;
            return this;
        }

        public DeploymentMockBuilder setLog(Log log) {
            requireNonNull(log);
            this.log = log;
            return this;
        }

        public DeploymentMockBuilder deploymentFactory(
                Function<DeploymentImpl.DeploymentState, DeploymentInternal> deploymentFactory
        ) {
            this.deploymentFactory = requireNonNull(deploymentFactory);
            return this;
        }

        public DeploymentMock build() {
            if (this.standardLogger == null) {
                this.standardLogger = defaultLogger();
            }
            if (this.logger == null) {
                this.logger = new DefaultEngineLogger(
                        this.standardLogger,
                        () -> this.runner,
                        () -> this.engine
                );
            }
            if (this.log == null) {
                this.log = new Log(this.logger, DeploymentImpl.DeploymentState.ExcessiveDebugOutput);
            }
            if (this.options == null) {
                this.options = new TestOptions();
            }
            // FIXME: this runner is being ignored right now in DeploymentState
            if (this.runner == null) {
                this.runner = new MockRunner();
            }
            if (this.engine == null) {
                this.engine = new MockEngine();
            }
            if (this.mocks == null) {
                this.mocks = new EmptyMocks();
            }
            if (this.monitor == null) {
                this.monitor = new MockMonitor(this.mocks, this.log);
            }
            if (this.config == null) {
                this.config = new DeploymentImpl.Config(ImmutableMap.of(), ImmutableSet.of());
            }

            if (this.state == null) {
                this.state = new DeploymentImpl.DeploymentState(
                        this.config,
                        this.standardLogger,
                        this.options.getProjectName(),
                        this.options.getStackName(),
                        this.options.isPreview(),
                        this.engine,
                        this.monitor
                );
            }

            if (this.deploymentFactory == null) {
                this.deploymentFactory = DeploymentImpl::new;
            }
            var deployment = deploymentFactory.apply(this.state);
            // FIXME: this is needed because we create runner inside DeploymentState currently
            this.runner = deployment.getRunner();

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(deployment));
            return new DeploymentMock(
                    this.options, this.runner, this.engine, this.monitor, deployment,
                    this.config, this.state, this.standardLogger, this.logger, this.log
            );
        }
    }

    public static void cleanupDeploymentMocks() {
        // ensure we don't get the error:
        //   java.lang.IllegalStateException: Deployment.getInstance should only be set once at the beginning of a 'run' call.
        DeploymentImpl.internalUnsafeDestroyInstance(); // FIXME: how to avoid this?
    }

    public static DeploymentImpl.Config config(ImmutableMap<String, String> allConfig, ImmutableSet<String> configSecretKeys) {
        return new DeploymentImpl.Config(allConfig, configSecretKeys);
    }

    public static ImmutableMap<String, String> parseConfig(String configJson) {
        return DeploymentImpl.Config.parseConfig(configJson);
    }

    public static ImmutableSet<String> parseConfigSecretKeys(String secretKeysJson) {
        return DeploymentImpl.Config.parseConfigSecretKeys(secretKeysJson);
    }

    public static Logger defaultLogger() {
        var standardLogger = Logger.getLogger(DeploymentTests.class.getName());
        standardLogger.setLevel(Level.INFO);
        return standardLogger;
    }

    public static Log mockLog() {
        return mockLog(defaultLogger(), MockEngine::new);
    }

    public static Log mockLog(Logger logger) {
        return mockLog(logger, MockEngine::new);
    }

    public static Log mockLog(Logger logger, Supplier<Engine> engine) {
        return new Log(new DefaultEngineLogger(logger, MockRunner::new, engine));
    }
}
