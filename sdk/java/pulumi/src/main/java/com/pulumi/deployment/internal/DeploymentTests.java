package com.pulumi.deployment.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.deployment.MockEngine;
import com.pulumi.deployment.MockMonitor;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentImpl.DefaultEngineLogger;
import com.pulumi.exceptions.RunException;
import com.pulumi.resources.Resource;
import com.pulumi.resources.Stack;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class DeploymentTests {

    private DeploymentTests() {
        throw new UnsupportedOperationException("static class");
    }

    public static final class DeploymentMock {
        public final TestOptions options;
        public final Runner runner;
        public final Engine engine;
        public final Monitor monitor;
        public final DeploymentImpl deployment;
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
                DeploymentImpl deployment,
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

        public <T extends Stack> CompletableFuture<ImmutableList<Resource>> testAsync(Supplier<T> stackFactory) {
            return tryTestAsync(stackFactory).thenApply(r -> {
                if (!r.exceptions.isEmpty()) {
                    throw new RunException(String.format("Error count: %d, errors: %s",
                            r.exceptions.size(), r.exceptions.stream()
                                    .map(Throwable::getMessage)
                                    .collect(Collectors.joining(", "))
                    ));
                }
                return r.resources;
            });
        }

        public <T extends Stack> CompletableFuture<TestAsyncResult> tryTestAsync(Supplier<T> stackFactory) {
            if (!(engine instanceof MockEngine)) {
                throw new IllegalStateException("Expected engine to be an instanceof MockEngine");
            }
            if (!(monitor instanceof MockMonitor)) {
                throw new IllegalStateException("Expected monitor to be an instanceof MockMonitor");
            }
            var mockEngine = (MockEngine) engine;
            var mockMonitor = (MockMonitor) monitor;
            return this.runner.runAsync(stackFactory)
                    .thenApply(ignore -> new TestAsyncResult(
                            ImmutableList.copyOf(mockMonitor.resources),
                            mockEngine.getErrors().stream()
                                    .map(RunException::new).collect(toImmutableList())
                    ));
        }

        public CompletableFuture<TestAsyncResult> runAsync(Supplier<CompletableFuture<Map<String, Output<?>>>> callback) {
            if (!(engine instanceof MockEngine)) {
                throw new IllegalStateException("Expected engine to be an instanceof MockEngine");
            }
            if (!(monitor instanceof MockMonitor)) {
                throw new IllegalStateException("Expected monitor to be an instanceof MockMonitor");
            }
            var mockEngine = (MockEngine) engine;
            var mockMonitor = (MockMonitor) monitor;
            return this.runner.runAsyncFuture(callback)
                    .thenApply(ignore -> new TestAsyncResult(
                            ImmutableList.copyOf(mockMonitor.resources),
                            mockEngine.getErrors().stream()
                                    .map(RunException::new)
                                    .collect(toImmutableList())
                    ));
        }

        public static class TestAsyncResult {
            public final ImmutableList<Resource> resources;
            public final ImmutableList<Exception> exceptions;

            public TestAsyncResult(ImmutableList<Resource> resources, ImmutableList<Exception> exceptions) {
                this.resources = resources;
                this.exceptions = exceptions;
            }
        }
    }

    public static final class DeploymentMockBuilder {

        @Nullable
        private TestOptions options;
        @Nullable
        private Runner runner;
        @Nullable
        private Monitor monitor;
        @Nullable
        private DeploymentImpl deployment;
        @Nullable
        private DeploymentImpl.Config config;
        @Nullable
        private DeploymentImpl.DeploymentState state;
        @Nullable
        private Engine engine;
        @Nullable
        private EngineLogger logger;
        @Nullable
        private Logger standardLogger;
        @Nullable
        private Log log;
        @Nullable
        private Mocks mocks;

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

        public DeploymentMockBuilder setEngine(Engine engine) {
            requireNonNull(engine);
            this.engine = engine;
            return this;
        }

        public DeploymentMockBuilder setMonitor(Monitor monitor) {
            requireNonNull(monitor);
            this.monitor = monitor;
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

        private void initUnset() {
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
                this.log = new Log(this.logger);
            }
            if (this.options == null) {
                this.options = new TestOptions();
            }
            if (this.runner == null) {
                this.runner = Mockito.mock(Runner.class);
            }
            if (this.engine == null) {
                this.engine = new MockEngine();
            }
            if (this.mocks == null) {
                throw new IllegalArgumentException("mocks are required");
            }
            if (this.monitor == null) {
                this.monitor = new MockMonitor(this.mocks, this.log);
            }
            if (this.config == null) {
                this.config = new DeploymentImpl.Config(ImmutableMap.of(), ImmutableSet.of());
            }

            if (this.state == null) {
                this.state = new DeploymentImpl.DeploymentState(
                        config,
                        this.standardLogger,
                        options.getProjectName(),
                        options.getStackName(),
                        options.isPreview(),
                        engine,
                        monitor
                );
            }
        }

        public DeploymentMock setSpyGlobalInstance() {
            initUnset();

            this.deployment = Mockito.spy(new DeploymentImpl(this.state));
            this.runner = this.deployment.getRunner();

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new DeploymentMock(options, runner, engine, monitor, deployment, config, state, standardLogger, logger, log);
        }

        public DeploymentMock setMockGlobalInstance() {
            initUnset();

            var mock = Mockito.mock(DeploymentImpl.class);
            //noinspection ConstantConditions
            Mockito.when(mock.isDryRun()).thenReturn(this.state.isDryRun);
            Mockito.when(mock.getProjectName()).thenReturn(this.state.projectName);
            Mockito.when(mock.getStackName()).thenReturn(this.state.stackName);
            Mockito.when(mock.getConfig(ArgumentMatchers.anyString())).then(invocation ->
                    this.state.config.getConfig((String) invocation.getArguments()[0])
            );
            Mockito.when(mock.isConfigSecret(ArgumentMatchers.anyString())).then(invocation ->
                    this.state.config.isConfigSecret((String) invocation.getArguments()[0])
            );
            Mockito.when(mock.getRunner()).thenReturn(this.runner);

            this.deployment = mock;

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new DeploymentMock(
                    options, runner, engine, monitor, deployment, config, state, standardLogger, logger, log);
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
        return mockLog(defaultLogger(), () -> Mockito.mock(Engine.class));
    }

    public static Log mockLog(Logger logger) {
        return mockLog(logger, () -> Mockito.mock(Engine.class));
    }

    public static Log mockLog(Logger logger, Supplier<Engine> engine) {
        return new Log(new DefaultEngineLogger(logger, () -> Mockito.mock(Runner.class), engine));
    }
}
