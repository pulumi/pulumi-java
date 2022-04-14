package io.pulumi.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.Log;
import io.pulumi.Pulumi;
import io.pulumi.Stack;
import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.context.internal.ConfigContextInternal;
import io.pulumi.context.internal.ExportContextInternal;
import io.pulumi.context.internal.LoggingContextInternal;
import io.pulumi.context.internal.OutputContextInternal;
import io.pulumi.context.internal.RuntimeContext;
import io.pulumi.context.internal.StackContextInternal;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputFactory;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.internal.DeploymentImpl;
import io.pulumi.deployment.internal.DeploymentInstanceHolder;
import io.pulumi.deployment.internal.DeploymentInstanceInternal;
import io.pulumi.deployment.internal.GrpcEngine;
import io.pulumi.deployment.internal.GrpcMonitor;
import io.pulumi.deployment.internal.Runner;
import io.pulumi.resources.StackOptions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static io.pulumi.core.internal.Environment.getBooleanEnvironmentVariable;
import static io.pulumi.core.internal.Environment.getEnvironmentVariable;
import static io.pulumi.core.internal.Environment.getIntegerEnvironmentVariable;
import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    private static final Logger STANDARD_LOGGER = Logger.getLogger(PulumiInternal.class.getName());

    private final Runner runner;
    private final DeploymentImpl deployment;
    private final RuntimeContext runtimeContext;
    private final StackContextInternal stackContext;

    public PulumiInternal(
            Runner runner, DeploymentImpl deployment, RuntimeContext runtimeContext, StackContextInternal stackContext
    ) {
        this.runner = requireNonNull(runner);
        this.deployment = requireNonNull(deployment);
        this.runtimeContext = requireNonNull(runtimeContext);
        this.stackContext = requireNonNull(stackContext);
    }

    public CompletableFuture<Integer> runAsync(Function<StackContext, ExportContext> callback) {
        // this method *must* remain async
        // in order to protect the scope of the Deployment#instance we cannot elide the task (return it early)
        // if the task is returned early and not awaited, then it is possible for any code that runs before
        // the eventual await to be executed synchronously and thus have multiple calls
        // to one of the run methods affecting each other Deployment#instance's
        return runner.runAsync(
                () -> {
                    var stack = Stack.StackInternal.of(
                            () -> Output.of(callback.apply(this.stackContext).exports()),
                            StackOptions.Empty
                    );
                    this.deployment.setStack(stack);
                    DeploymentInstanceHolder.setInstance(new DeploymentInstanceInternal(deployment));
                    return stack;
                }
        );
    }

    @VisibleForTesting
    @InternalUse
    static PulumiInternal with(RuntimeContext runtimeCtx) {
        STANDARD_LOGGER.log(Level.FINEST, "Creating resource engine");
        var engine = new GrpcEngine(runtimeCtx.engineTarget());
        STANDARD_LOGGER.log(Level.FINEST, "Created resource engine");

        STANDARD_LOGGER.log(Level.FINEST, "Creating resource monitor");
        var monitor = new GrpcMonitor(runtimeCtx.monitorTarget());
        STANDARD_LOGGER.log(Level.FINEST, "Created resource monitor");

        // Use CompletableFuture and Supplier to avoid problems with cyclic dependencies and late
        final CompletableFuture<DeploymentImpl.DefaultRunner> runnerFuture = new CompletableFuture<>(); // incomplete future
        var logger = new DeploymentImpl.DefaultEngineLogger(STANDARD_LOGGER, runnerFuture::join, () -> engine);
        try {
            runnerFuture.complete(new DeploymentImpl.DefaultRunner(STANDARD_LOGGER, logger, runtimeCtx.taskTimeoutInMillis()));
        } catch (Throwable throwable) {
            runnerFuture.completeExceptionally(throwable);
        }
        var runner = runnerFuture.join();
        // End of cyclic dependencies lazy init

        var log = new Log(logger, runtimeCtx.isExcessiveDebugOutput());
        var config = new ConfigInternal(runtimeCtx.projectName(), runtimeCtx.config(), runtimeCtx.configSecretKeys());

        // TODO: finish refactoring the deployment
        var deployment = new DeploymentImpl(
                runtimeCtx.projectName(), runtimeCtx.stackName(), runtimeCtx.isDryRun(),
                runtimeCtx.isDisableResourceReferences(),
                log, engine, monitor, runner
        );

        var loggingCtx = new LoggingContextInternal(log);
        var configCtx = new ConfigContextInternal(config);
        var outputFactory = new OutputFactory(runner);
        var outputsCtx = new OutputContextInternal(outputFactory);
        var exportsCtx = new ExportContextInternal();

        var ctx = new StackContextInternal(
                runtimeCtx.projectName(), runtimeCtx.stackName(), loggingCtx, configCtx, outputsCtx, exportsCtx
        );
        return new PulumiInternal(runner, deployment, runtimeCtx, ctx);
    }

    @InternalUse
    public static PulumiInternal fromEnvironment() {
        var runtime = readEnvironment();
        return with(runtime);
    }

    /**
     * @throws IllegalArgumentException if an environment variable was required and not found
     */
    private static RuntimeContext readEnvironment() {
        var globalLogLevel = getBooleanEnvironmentVariable("PULUMI_JVM_LOG_VERBOSE").or(false)
                ? Level.FINEST
                : Level.SEVERE;
        setupRootLogger(globalLogLevel);
        STANDARD_LOGGER.setLevel(globalLogLevel);
        STANDARD_LOGGER.log(Level.FINEST, "ENV: " + System.getenv());

        Function<Exception, RuntimeException> startErrorSupplier = (Exception e) ->
                new IllegalArgumentException(
                        "Program run without the Pulumi engine available; re-run using the `pulumi` CLI", e
                );

        var monitorTarget = getEnvironmentVariable("PULUMI_MONITOR").orThrow(startErrorSupplier);
        var engineTarget = getEnvironmentVariable("PULUMI_ENGINE").orThrow(startErrorSupplier);
        var projectName = getEnvironmentVariable("PULUMI_PROJECT").orThrow(startErrorSupplier);
        var stackName = getEnvironmentVariable("PULUMI_STACK").orThrow(startErrorSupplier);
        // var pwd = getEnvironmentVariable("PULUMI_PWD");
        var dryRun = getBooleanEnvironmentVariable("PULUMI_DRY_RUN").orThrow(startErrorSupplier);
        // var queryMode = getBooleanEnvironmentVariable("PULUMI_QUERY_MODE");
        // var parallel = getIntegerEnvironmentVariable("PULUMI_PARALLEL");
        // var tracing = getEnvironmentVariable("PULUMI_TRACING");
        // TODO what to do with all the unused envvars?

        var disableResourceReferences = getBooleanEnvironmentVariable("PULUMI_DISABLE_RESOURCE_REFERENCES").or(false);
        var excessiveDebugOutput = getBooleanEnvironmentVariable("PULUMI_EXCESSIVE_DEBUG_OUTPUT").or(false);
        var taskTimeoutInMillis = getIntegerEnvironmentVariable("PULUMI_JVM_TASK_TIMEOUT_IN_MILLIS").or(-1);

        STANDARD_LOGGER.log(Level.FINEST, "Parsing configuration files");

        var config = getEnvironmentVariable("PULUMI_CONFIG")
                .map(ConfigInternal::parseConfig)
                .or(ImmutableMap.of());

        var configSecretKeys = getEnvironmentVariable("PULUMI_CONFIG_SECRET_KEYS")
                .map(ConfigInternal::parseConfigSecretKeys)
                .or(ImmutableSet.of());

        return new RuntimeContext(
                monitorTarget, engineTarget, projectName, stackName, dryRun, config, configSecretKeys, globalLogLevel,
                disableResourceReferences, excessiveDebugOutput, taskTimeoutInMillis
        );
    }

    @CanIgnoreReturnValue
    private static Logger setupRootLogger(Level globalLogLevel) {
        // Gradle uses org.gradle.api.internal.tasks.testing.junit.JULRedirector to set a ConsoleHandler
        // we want to change the logging levels to this handler to see logs in the Pulumi CLI
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(globalLogLevel);
        }

        rootLogger.log(Level.INFO, "Logger initialized with global level: " + rootLogger.getLevel());
        return rootLogger;
    }
}
