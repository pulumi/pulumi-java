package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.resources.Stack;
import com.pulumi.resources.StackOptions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pulumi.resources.Stack.StackInternal;
import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    protected final Runner runner;
    private final ContextInternal stackContext;

    public PulumiInternal(Runner runner, ContextInternal stackContext) {
        this.runner = requireNonNull(runner);
        this.stackContext = requireNonNull(stackContext);
    }

    @InternalUse
    public static PulumiInternal fromEnvironment() {
        var deployment = DeploymentImpl.fromEnvironment();
        var runner = deployment.getRunner();
        var ctx = contextFromDeployment(deployment);
        return new PulumiInternal(runner, ctx);
    }

    // TODO: remove after refactoring Deployment
    protected static ContextInternal contextFromDeployment(DeploymentImpl deployment) {
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
        var exports = Map.<String, Output<?>>of();

        return new ContextInternal(projectName, stackName, logging, config, outputs, exports);
    }

    @InternalUse
    public CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        return runAsyncResult(userProgram(stackCallback)).thenApply(Result::exitCode);
    }

    @InternalUse
    protected CompletableFuture<Result> runAsyncResult(Function<ContextInternal, Map<String, Output<?>>> stackCallback) {
        // Create and initialize the stack in the context of the Runner's error handler
        var stack = implicitStack(stackCallback);
        return this.runner.registerAndRunAsync(stack::join).thenCompose(
                exitCode -> stack.thenApply(s -> new Result(exitCode, this.runner.getSwallowedExceptions(), s))
        );
    }

    protected Function<ContextInternal, Map<String, Output<?>>> userProgram(Consumer<Context> stackCallback) {
        return (ctx) -> {
            // before user code was executed
            stackCallback.accept(ctx); // MUST run before accessing mutable variables
            // after user code was executed
            return ctx.exports();
        };
    }

    private static CompletableFuture<Map<String, Output<?>>> runInitAsync(
            Supplier<CompletableFuture<Map<String, Output<?>>>> exports
    ) {
        return CompletableFuture.supplyAsync(exports).thenCompose(Function.identity());
    }

    private CompletableFuture<Stack> implicitStack(Function<ContextInternal, Map<String, Output<?>>> stackCallback) {
        /*
          There is an internal circular dependency between any Resource and Stack.
          As a consequence, the user code will need to be run after Stack instance is fully initialized,
          because user code will create new Resources, that require this circular dependency to be already in place.
          Any Resource subclass constructor call before Stack is initialized will fail at runtime.

          To solve this issue the user code will be run after the Stack outputs are registered.
         */
        return CompletableFuture.supplyAsync(() -> {
            var stack = StackInternal.of( // TODO: simplify the Stack construction
                    () -> CompletableFuture.supplyAsync(() -> {
                        return CompletableFuture.supplyAsync(
                                () -> {
                                    return stackCallback.apply(this.stackContext);
                                }
                        );
                    }).thenCompose(Function.identity()),
                    StackOptions.Empty
            );

            // Set a derived class as the deployment stack, this is needed in various places before user code runs.
            DeploymentInternal.getInstance().setStack(stack);
            return stack;
        });
    }

    @InternalUse
    protected static class Result {
        private final int exitCode;
        private final List<Exception> exceptions;
        private final Stack stack;

        protected Result(
                int exitCode,
                List<Exception> exceptions,
                Stack stack
        ) {
            this.exitCode = exitCode;
            this.exceptions = requireNonNull(exceptions);
            this.stack = requireNonNull(stack);
        }

        public int exitCode() {
            return exitCode;
        }

        public List<Exception> exceptions() {
            return exceptions;
        }

        public Stack stack() {
            return this.stack;
        }
    }
}
