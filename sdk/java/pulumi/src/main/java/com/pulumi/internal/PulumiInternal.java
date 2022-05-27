package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.deployment.internal.Runner.Result;
import com.pulumi.resources.internal.StackDefinition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    protected final Runner runner;
    protected final ContextInternal stackContext;

    public PulumiInternal(Runner runner, ContextInternal stackContext) {
        this.runner = requireNonNull(runner);
        this.stackContext = requireNonNull(stackContext);
    }

    @InternalUse
    public static PulumiInternal fromEnvironment() {
        var deployment = DeploymentImpl.fromEnvironment();
        var instance = Deployment.getInstance();
        var projectName = deployment.getProjectName();
        var stackName = deployment.getStackName();
        var runner = deployment.getRunner();
        var log = deployment.getLog();

        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);

        var ctx = new ContextInternal(projectName, stackName, logging, config, outputs);
        return new PulumiInternal(runner, ctx);
    }

    @InternalUse
    public CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        return runAsyncResult(stackCallback).thenApply(r -> r.exitCode());
    }

    @InternalUse
    protected CompletableFuture<Result<ContextInternal>> runAsyncResult(Consumer<Context> stackCallback) {
        var stackDefinition = new StackDefinition(
                this.stackContext.projectName(),
                this.stackContext.stackName(),
                List.of()
        );
        // TODO: simplify this call
        Internal.of(Deployment.getInstance()).getInternal().setStack(stackDefinition);
        var result = this.runner.runAsync(() -> {
            // before user code was executed
            stackCallback.accept(this.stackContext); // MUST run before accessing mutable variables
            // after user code was executed, the context might have been mutated
            return this.stackContext;
        });
        return result.thenApply(r -> {
            r.result().ifPresent(ctx -> {
                var outputs = Output.of(ctx.exports());
                stackDefinition.registerOutputs(outputs);
                this.runner.registerTask(
                        String.format(
                                "stack outputs: %s, %s",
                                stackDefinition.getResourceType(), stackDefinition.getResourceName()
                        ),
                        Internal.of(outputs).getDataAsync()

                );
            });
            return r;
        });
    }
}
