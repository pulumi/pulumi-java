package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.Engine;
import com.pulumi.deployment.internal.Monitor;
import com.pulumi.deployment.internal.GrpcEngine;
import com.pulumi.deployment.internal.GrpcMonitor;
import com.pulumi.deployment.internal.InlineDeploymentSettings;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.deployment.internal.Runner.Result;
import com.pulumi.resources.StackOptions;
import com.pulumi.resources.internal.Stack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Internal implementation of the {@link Pulumi} API, responsible for configuring and executing Pulumi stack deployments.
 *
 * @see com.pulumi.deployment.Deployment
 * @see com.pulumi.deployment.internal.DeploymentImpl
 * @see com.pulumi.resources.StackOptions
 */
@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi, Pulumi.API {

    /**
     * The internal context representing the current stack execution environment.
     */
    protected final Runner runner;
    /**
     * The internal context representing the current stack execution environment.
     */
    protected final ContextInternal stackContext;

    /**
     * Constructs a new {@code PulumiInternal} instance with the specified runner and stack context.
     *
     * @param runner the runner used to execute deployment tasks
     * @param stackContext the context representing the stack execution environment
     * @throws NullPointerException if either argument is {@code null}
     */
    @InternalUse
    public PulumiInternal(Runner runner, ContextInternal stackContext) {
        this.runner = requireNonNull(runner);
        this.stackContext = requireNonNull(stackContext);
    }

    /**
     * Creates a new {@code PulumiInternal} instance using environment-based deployment configuration.
     *
     * @param options stack options to customize the deployment
     * @return a configured {@code PulumiInternal} instance
     * @see com.pulumi.resources.StackOptions
     */
    @InternalUse
    public static PulumiInternal fromEnvironment(StackOptions options) {
        var deployment = DeploymentImpl.fromEnvironment();
        return completeConfiguration(deployment, options);
    }

    /**
     * Creates a new {@code PulumiInternal} instance using inline deployment settings.
     *
     * @param settings inline deployment settings
     * @param options stack options to customize the deployment
     * @return a configured {@code PulumiInternal} instance
     * @see com.pulumi.deployment.internal.InlineDeploymentSettings
     * @see com.pulumi.resources.StackOptions
     */
    @InternalUse
    public static PulumiInternal fromInline(InlineDeploymentSettings settings, StackOptions options) {
        var deployment = DeploymentImpl.fromInline(settings);
        return completeConfiguration(deployment, options);
    }

    /**
     * Completes the configuration of a {@code PulumiInternal} instance by initializing context and dependencies.
     *
     * @param deployment the deployment implementation to use
     * @param options stack options to customize the deployment
     * @return a fully configured {@code PulumiInternal} instance
     */
    private static PulumiInternal completeConfiguration(DeploymentImpl deployment, StackOptions options) {
        var instance = Deployment.getInstance();
        var organizationName = deployment.getOrganizationName();
        var projectName = deployment.getProjectName();
        var stackName = deployment.getStackName();
        var runner = deployment.getRunner();
        var log = deployment.getLog();

        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);

        var ctx = new ContextInternal(
                organizationName, projectName, stackName, logging, config, outputs, options.resourceTransformations()
        );
        return new PulumiInternal(runner, ctx);
    }

    /**
     * Runs the Pulumi stack synchronously, blocking until completion and exiting the process with the result code.
     *
     * @param stack a {@link Consumer} that receives the Pulumi {@link Context} for stack logic execution
     * @see com.pulumi.Context
     */
    public void run(Consumer<Context> stack) {
        System.exit(runAsync(stack).join());
    }

    /**
     * Runs the Pulumi stack asynchronously, returning a future for the exit code.
     *
     * @param stackCallback a {@link Consumer} that receives the Pulumi {@link Context} for stack logic execution
     * @return a {@link CompletableFuture} that completes with the process exit code
     */
    public CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        return runAsyncResult(stackCallback).thenApply(r -> r.exitCode());
    }

    /**
     * Runs the Pulumi stack inline with a custom asynchronous runner function.
     *
     * @param runnerFunc a function that receives the Pulumi {@link Context} and returns a future result
     * @param <T> the type of the result produced by the runner function
     * @return a {@link CompletableFuture} that completes with the runner function's result
     * @throws RuntimeException if multiple exceptions occur during execution
     * @throws IllegalStateException if no result or exceptions are available
     * @see com.pulumi.Context
     */
    @InternalUse
    public <T> CompletableFuture<T> runInlineAsync(Function<Context, CompletableFuture<T>> runnerFunc) {
        return runner.runAsync(() -> runnerFunc.apply(stackContext))
                .thenCompose(result -> result.result()
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> {
                            var exceptions = result.exceptions();
                            if (!exceptions.isEmpty()) {
                                if (exceptions.size() == 1) {
                                    return CompletableFuture.failedFuture(exceptions.get(0));
                                }
                                var composite = new RuntimeException("Multiple exceptions occurred");
                                exceptions.forEach(composite::addSuppressed);
                                return CompletableFuture.failedFuture(composite);
                            }
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("No result or exceptions available"));
                        }))
                .thenCompose(Function.identity());
    }

    /**
     * Runs the Pulumi stack asynchronously and returns a future for the stack execution result.
     *
     * @param stackCallback a {@link Consumer} that receives the Pulumi {@link Context} for stack logic execution
     * @return a {@link CompletableFuture} containing the result of the stack execution
     * @see com.pulumi.Context
     * @see com.pulumi.resources.internal.Stack
     */
    protected CompletableFuture<Result<Stack>> runAsyncResult(Consumer<Context> stackCallback) {
        // Stack must be created and set globally before running any user code
        return runner.runAsync(
                () -> Stack.factory(
                        this.stackContext.projectName(),
                        this.stackContext.stackName(),
                        this.stackContext.resourceTransformations()
                ).apply(() -> {
                    // before user code was executed
                    stackCallback.accept(this.stackContext); // MUST run before accessing mutable variables
                    // after user code was executed
                    return this.stackContext.exports();
                })
        );
    }
}
