package io.pulumi.internal;

import io.pulumi.Config;
import io.pulumi.Context;
import io.pulumi.Exports;
import io.pulumi.Pulumi;
import io.pulumi.context.internal.ConfigContextInternal;
import io.pulumi.context.internal.ContextInternal;
import io.pulumi.context.internal.ExportsInternal;
import io.pulumi.context.internal.LoggingContextInternal;
import io.pulumi.context.internal.OutputContextInternal;
import io.pulumi.core.internal.OutputFactory;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.DeploymentImpl;
import io.pulumi.deployment.internal.Runner;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    private final Runner runner;
    private final ContextInternal stackContext;

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
        Function<String, Config> configFactory = (name) -> new Config(instance, name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);
        var exports = new ExportsInternal();

        var ctx = new ContextInternal(stackName, logging, config, outputs, exports);
        return new PulumiInternal(runner, ctx);
    }

    @InternalUse
    public CompletableFuture<Integer> runAsync(Function<Context, Exports> stack) {
        return runner.runAsyncFuture(
                () -> CompletableFuture.supplyAsync(
                        () -> stack.apply(stackContext).exports()
                )
        );
    }
}
