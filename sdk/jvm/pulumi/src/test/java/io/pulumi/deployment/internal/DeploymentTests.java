package io.pulumi.deployment.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.Stack;
import io.pulumi.deployment.MockEngine;
import io.pulumi.deployment.MockMonitor;
import io.pulumi.deployment.MyMocks;
import io.pulumi.resources.Resource;
import io.pulumi.test.internal.TestOptions;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DeploymentTests {

    public static final Logger Log = Logger.getLogger(DeploymentTests.class.getName());

    static {
        Log.setLevel(Level.FINEST);
    }

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
        public final EngineLogger logger;

        private DeploymentMock(
                TestOptions options,
                Runner runner,
                Engine engine,
                Monitor monitor,
                DeploymentImpl deployment,
                DeploymentImpl.Config config,
                DeploymentImpl.DeploymentState state,
                EngineLogger logger
        ) {
            this.options = Objects.requireNonNull(options);
            this.runner = Objects.requireNonNull(runner);
            this.engine = Objects.requireNonNull(engine);
            this.monitor = Objects.requireNonNull(monitor);
            this.deployment = Objects.requireNonNull(deployment);
            this.config = Objects.requireNonNull(config);
            this.state = Objects.requireNonNull(state);
            this.logger = Objects.requireNonNull(logger);
        }

        public void overrideConfig(String key, String value) {
            this.config.setConfig(key, value);
        }

        public void overrideConfig(ImmutableMap<String, String> config, @Nullable Iterable<String> secretKeys) {
            this.config.setAllConfig(config, secretKeys);
        }

        public Runner getRunner() {
            return this.runner;
        }

        public <T extends Stack> CompletableFuture<ImmutableList<Resource>> testAsync(Class<T> stackType) {
            if (!(engine instanceof MockEngine)) {
                throw new IllegalStateException("Expected engine to be an instanceof MockEngine");
            }
            if (!(monitor instanceof MockMonitor)) {
                throw new IllegalStateException("Expected monitor to be an instanceof MockMonitor");
            }
            var mockEngine = (MockEngine) engine;
            var mockMonitor = (MockMonitor) monitor;
            return this.runner.runAsync(stackType)
                    .thenApply(ignore -> {
                        if (!mockEngine.errors.isEmpty()) {
                            throw new RuntimeException(String.format("Error count: %d, errors: %s",
                                    mockEngine.errors.size(), String.join(", ", mockEngine.errors)
                            ));
                        }
                        return ImmutableList.copyOf(mockMonitor.resources);
                    });
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

        private DeploymentMockBuilder() {
            // Empty
        }

        public static DeploymentMockBuilder builder() {
            return new DeploymentMockBuilder();
        }

        public DeploymentMockBuilder setOptions(TestOptions options) {
            Objects.requireNonNull(options);
            this.options = options;
            return this;
        }

        public DeploymentMockBuilder setRunner(Runner runner) {
            Objects.requireNonNull(runner);
            this.runner = runner;
            return this;
        }

        public DeploymentMockBuilder setEngine(Engine engine) {
            Objects.requireNonNull(engine);
            this.engine = engine;
            return this;
        }

        public DeploymentMockBuilder setMonitor(Monitor monitor) {
            Objects.requireNonNull(monitor);
            this.monitor = monitor;
            return this;
        }

        public DeploymentMockBuilder setConfig(DeploymentImpl.Config config) {
            Objects.requireNonNull(config);
            this.config = config;
            return this;
        }

        public DeploymentMockBuilder setState(DeploymentImpl.DeploymentState state) {
            Objects.requireNonNull(state);
            this.state = state;
            return this;
        }

        public DeploymentMockBuilder setLogger(EngineLogger logger) {
            Objects.requireNonNull(logger);
            this.logger = logger;
            return this;
        }

        private void initUnset() {
            if (this.options == null) {
                this.options = new TestOptions();
            }
            if (this.runner == null) {
                this.runner = mock(Runner.class);
            }
            if (this.engine == null) {
                this.engine = new MockEngine();
            }
            if (this.monitor == null) {
                this.monitor = new MockMonitor(new MyMocks());
            }
            if (this.config == null) {
                this.config = new DeploymentImpl.Config(ImmutableMap.of(), ImmutableSet.of());
            }

            if (this.state == null) {
                this.state = new DeploymentImpl.DeploymentState(
                        config,
                        DeploymentTests.Log,
                        options.getProjectName(),
                        options.getStackName(),
                        options.isPreview(),
                        engine,
                        monitor
                );
            }

            if (this.logger == null) {
                this.logger = new DeploymentImpl.DefaultEngineLogger(state, Log);
            }
        }

        public DeploymentMock setSpyGlobalInstance() {
            initUnset();

            this.deployment = spy(new DeploymentImpl(this.state));
            this.runner = this.deployment.getRunner();

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new DeploymentMock(
                    options, runner, engine, monitor, deployment, config, state, logger);
        }

        public DeploymentMock setMockGlobalInstance() {
            initUnset();

            var mock = mock(DeploymentImpl.class);
            when(mock.isDryRun()).thenReturn(this.state.isDryRun);
            when(mock.getProjectName()).thenReturn(this.state.projectName);
            when(mock.getStackName()).thenReturn(this.state.stackName);
            when(mock.getConfig(anyString())).then(invocation ->
                    this.state.config.getConfig((String) invocation.getArguments()[0])
            );
            when(mock.isConfigSecret(anyString())).then(invocation ->
                    this.state.config.isConfigSecret((String) invocation.getArguments()[0])
            );
            when(mock.getRunner()).thenReturn(this.runner);
            when(mock.getLogger()).thenReturn(this.logger);

            this.deployment = mock;

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new DeploymentMock(
                    options, runner, engine, monitor, deployment, config, state, logger);
        }
    }

    public static void cleanupDeploymentMocks() {
        // ensure we don't get the error:
        //   java.lang.IllegalStateException: Deployment.getInstance should only be set once at the beginning of a 'run' call.
        DeploymentImpl.internalUnsafeDestroyInstance(); // FIXME: how to avoid this?
    }

    public static void printErrorCount(EngineLogger logger) {
        if (logger.hasLoggedErrors()) {
            System.out.println("logger.errorCount=" + logger.getErrorCount());
        }
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
}