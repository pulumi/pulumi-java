package io.pulumi.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.Log;
import io.pulumi.Pulumi;
import io.pulumi.Stack;
import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.context.internal.ConfigContextInternal;
import io.pulumi.context.internal.ExportContextInternal;
import io.pulumi.context.internal.LoggingContextInternal;
import io.pulumi.context.internal.OutputContextInternal;
import io.pulumi.context.internal.StackContextInternal;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputFactory;
import io.pulumi.deployment.MockEngine;
import io.pulumi.deployment.MockMonitor;
import io.pulumi.deployment.Mocks;
import io.pulumi.deployment.internal.DeploymentImpl;
import io.pulumi.deployment.internal.DeploymentInstanceInternal;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.Engine;
import io.pulumi.deployment.internal.EngineLogger;
import io.pulumi.deployment.internal.Runner;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.Resource;
import io.pulumi.resources.StackOptions;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class PulumiMock implements Pulumi {

    public final TestRuntimeContext runtimeContext;
    public final Runner runner;
    public final MockEngine engine;
    public final MockMonitor monitor;
    public final DeploymentImpl deployment;
    public final ConfigInternal config;
    public final Logger standardLogger;
    public final EngineLogger logger;
    public final Log log;
    public final StackContext stackContext;

    public PulumiMock(
            TestRuntimeContext runtimeContext, Runner runner, MockEngine engine, MockMonitor monitor,
            DeploymentImpl deployment, ConfigInternal config, Logger standardLogger, EngineLogger logger,
            Log log, StackContext ctx) {
        this.runtimeContext = requireNonNull(runtimeContext);
        this.runner = requireNonNull(runner);
        this.engine = requireNonNull(engine);
        this.monitor = requireNonNull(monitor);
        this.deployment = requireNonNull(deployment);
        this.config = requireNonNull(config);
        this.standardLogger = requireNonNull(standardLogger);
        this.logger = requireNonNull(logger);
        this.log = requireNonNull(log);
        this.stackContext = requireNonNull(ctx);
    }

    @Override
    public CompletableFuture<Integer> runAsync(Function<StackContext, ExportContext> callback) {
        return this.runner.runAsync(() -> Stack.StackInternal.of(
                () -> Output.of(callback.apply(this.stackContext).exports()),
                StackOptions.Empty
        ));
    }

    public CompletableFuture<TestAsyncResult> testAsyncOutputs(Supplier<Output<Map<String, Output<?>>>> outputs) {
        return testAsync(() -> Stack.StackInternal.of(outputs, StackOptions.Empty));
    }

    public <T extends Stack> CompletableFuture<TestAsyncResult> testAsync(Supplier<T> stackFactory) {
        return this.runner.runAsync(stackFactory::get)
                .thenApply(ignore -> new TestAsyncResult(
                        ImmutableList.copyOf(this.monitor.resources),
                        this.engine.getErrors().stream().map(RunException::new).collect(toImmutableList())
                ));
    }

    public <T extends Stack> CompletableFuture<ImmutableList<Resource>> testAsyncOrThrow(Supplier<T> stackFactory) {
        return testAsync(stackFactory).thenApply(r -> {
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

    public static class TestAsyncResult {
        public final ImmutableList<Resource> resources;
        public final ImmutableList<Exception> exceptions;

        public TestAsyncResult(ImmutableList<Resource> resources, ImmutableList<Exception> exceptions) {
            this.resources = resources;
            this.exceptions = exceptions;
        }
    }

    public static PulumiMockBuilder builder() {
        return new PulumiMockBuilder();
    }

    public static final class PulumiMockBuilder {

        @Nullable
        private TestRuntimeContext runtimeCtx;
        @Nullable
        private Runner runner;
        @Nullable
        private MockMonitor monitor;
        @Nullable
        private DeploymentImpl deployment;
        @Nullable
        private ConfigInternal config;
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
        private StackContext ctx;

        private PulumiMockBuilder() { /* Empty */ }

        public PulumiMockBuilder setRuntimeContext(TestRuntimeContext runtimeCtx) {
            requireNonNull(runtimeCtx);
            this.runtimeCtx = runtimeCtx;
            return this;
        }

        public PulumiMockBuilder setRunner(Runner runner) {
            requireNonNull(runner);
            this.runner = runner;
            return this;
        }

        public PulumiMockBuilder setEngine(MockEngine engine) {
            requireNonNull(engine);
            this.engine = engine;
            return this;
        }

        public PulumiMockBuilder setMonitor(MockMonitor monitor) {
            requireNonNull(monitor);
            this.monitor = monitor;
            return this;
        }

        public PulumiMockBuilder setMocks(Mocks mocks) {
            requireNonNull(mocks);
            this.mocks = mocks;
            return this;
        }

        public PulumiMockBuilder setConfig(ConfigInternal config) {
            requireNonNull(config);
            this.config = config;
            return this;
        }

        public PulumiMockBuilder setLogger(EngineLogger logger) {
            requireNonNull(logger);
            this.logger = logger;
            return this;
        }

        public PulumiMockBuilder setStandardLogger(Logger logger) {
            requireNonNull(logger);
            this.standardLogger = logger;
            return this;
        }

        public PulumiMockBuilder setLog(Log log) {
            requireNonNull(log);
            this.log = log;
            return this;
        }

        private void initUnset() {
            if (this.standardLogger == null) {
                this.standardLogger = defaultLogger();
            }
            if (this.logger == null) {
                this.logger = new DeploymentImpl.DefaultEngineLogger(
                        this.standardLogger,
                        () -> this.runner,
                        () -> this.engine
                );
            }
            if (this.log == null) {
                this.log = new Log(this.logger);
            }
            if (this.runtimeCtx == null) {
                this.runtimeCtx = TestRuntimeContext.builder().build();
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
                this.config = new ConfigInternal(this.runtimeCtx.projectName(), ImmutableMap.of(), ImmutableSet.of());
            }

            var loggingCtx = new LoggingContextInternal(log);
            var configCtx = new ConfigContextInternal(config);
            var outputFactory = new OutputFactory(runner);
            var outputsCtx = new OutputContextInternal(outputFactory);
            var exportsCtx = new ExportContextInternal();

            this.ctx = new StackContextInternal(
                    runtimeCtx.projectName(), runtimeCtx.stackName(),
                    loggingCtx, configCtx, outputsCtx, exportsCtx
            );
        }

        public PulumiMock buildSpyGlobalInstance() {
            initUnset();

            this.deployment = Mockito.spy(new DeploymentImpl(
                    this.runtimeCtx.projectName(), this.runtimeCtx.stackName(), this.runtimeCtx.isDryRun(),
                    this.runtimeCtx.isDisableResourceReferences(),
                    this.log, this.engine, this.monitor, this.runner
            ));
            this.runner = this.deployment.getRunner();

            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new PulumiMock(
                    this.runtimeCtx, this.runner, this.engine, this.monitor, this.deployment,
                    this.config, this.standardLogger, this.logger, this.log, this.ctx
            );
        }

        public PulumiMock buildMockGlobalInstance() {
            initUnset();

            var mock = Mockito.mock(DeploymentImpl.class);
            Mockito.when(mock.isDryRun()).thenReturn(this.runtimeCtx.isDryRun());
            Mockito.when(mock.getProjectName()).thenReturn(this.runtimeCtx.projectName());
            Mockito.when(mock.getStackName()).thenReturn(this.runtimeCtx.stackName());
            Mockito.when(mock.getRunner()).thenReturn(this.runner);

            this.deployment = mock;
            DeploymentImpl.setInstance(new DeploymentInstanceInternal(this.deployment));
            return new PulumiMock(
                    this.runtimeCtx, this.runner, this.engine, this.monitor, this.deployment,
                    this.config, this.standardLogger, this.logger, this.log, this.ctx
            );
        }
    }

    public static void cleanupDeploymentMocks() {
        // ensure we don't get the error:
        //   java.lang.IllegalStateException: Deployment.getInstance should only be set once at the beginning of a 'run' call.
        DeploymentImpl.internalUnsafeDestroyInstance(); // FIXME: how to avoid this?
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
        return new Log(new DeploymentImpl.DefaultEngineLogger(logger, () -> Mockito.mock(Runner.class), engine));
    }
}