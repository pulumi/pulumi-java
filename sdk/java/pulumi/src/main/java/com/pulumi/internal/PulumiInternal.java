package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Log;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.DeploymentInstance;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.deployment.internal.Runner.Result;
import com.pulumi.resources.StackOptions;
import com.pulumi.resources.internal.Stack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    protected final Runner runner;
    protected final ContextInternal stackContext;

    @InternalUse
    public PulumiInternal(Runner runner, ContextInternal stackContext) {
        this.runner = requireNonNull(runner);
        this.stackContext = requireNonNull(stackContext);
    }

    protected CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        return runAsyncResult(stackCallback).thenApply(r -> r.exitCode());
    }

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

    private static ContextInternal contextInternal(
            StackOptions options,
            DeploymentInstance instance,
            String projectName,
            String stackName,
            Runner runner,
            Log log
    ) {
        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);

        return new ContextInternal(
                projectName, stackName, logging, config, outputs, options.resourceTransformations()
        );
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class APIInternal extends PulumiInternal implements Pulumi.API {

        public APIInternal(Runner runner, ContextInternal stackContext) {
            super(runner, stackContext);
        }

        @Override
        public void run(Consumer<Context> stackCallback) {
            System.exit(runAsync(stackCallback).join());
        }

        @Override
        public CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
            return runAsyncResult(stackCallback).thenApply(r -> r.exitCode());
        }

        public static APIInternal fromEnvironment(StackOptions options) {
            var deployment = DeploymentImpl.fromEnvironment();
            var instance = Deployment.getInstance();
            var projectName = deployment.getProjectName();
            var stackName = deployment.getStackName();
            var runner = deployment.getRunner();
            var log = deployment.getLog();

            ContextInternal ctx = contextInternal(options, instance, projectName, stackName, runner, log);
            return new APIInternal(runner, ctx);
        }
    }
}
