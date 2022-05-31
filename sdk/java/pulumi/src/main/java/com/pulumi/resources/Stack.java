package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentInternal;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public final class Stack extends ComponentResource {

    /**
     * The outputs of this stack, if the <code>callback</code> callback exited normally.
     */
    private final Output<Map<String, Output<?>>> outputs;

    /**
     * Create a Stack with stack resources defined in derived class constructor.
     *
     * @param options optional stack options
     */
    private Stack(Output<Map<String, Output<?>>> outputs, StackOptions options) {
        super(
                StackInternal.RootPulumiStackTypeName,
                String.format("%s-%s", Deployment.getInstance().getProjectName(), Deployment.getInstance().getStackName()),
                convertOptions(options)
        );
        this.outputs = requireNonNull(outputs);
    }

    @Nullable
    private static ComponentResourceOptions convertOptions(@Nullable StackOptions options) {
        if (options == null) {
            return null;
        }

        return new ComponentResourceOptions(
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                options.getResourceTransformations(),
                null,
                null,
                null,
                false,
                null,
                null
        );
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public final static class StackInternal extends ComponentResourceInternal {

        private final Stack stack;

        private StackInternal(Stack stack) {
            super(stack);
            this.stack = requireNonNull(stack);
        }

        public static StackInternal from(Stack r) {
            return new StackInternal(r);
        }

        @InternalUse
        public Output<Map<String, Output<?>>> getOutputs() {
            return this.stack.outputs;
        }

        /**
         * The type name that should be used to construct the root component in the tree of Pulumi resources
         * allocated by a deployment. This must be kept up to date with
         * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
         */
        @InternalUse
        public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

        @InternalUse
        public static Supplier<Stack> of(Supplier<Map<String, Output<?>>> callback, StackOptions options) {
            // Stack initialization requires specific sequence of events:
            // - create a Stack instance (note that the super constructor calls readOrRegisterResource)
            // - set the Stack instance in the global state (user code will try to reach it via Resource constructor)
            // - register the Stack outputs in the Pulumi engine
            // - call the user code callback (asynchronously to avoid any additional cyclic dependency problems)
            // - complete the Stack initialization (by completing the lazyFuture)
            return () -> {
                // incomplete future that will be completed after the Stack is constructed
                var lazyFuture = new CompletableFuture<Map<String, Output<?>>>();
                // incomplete outputs that will be completed when lazyFuture completes
                var lazyOutputs = Output.of(lazyFuture);
                // Stack must be the first Resource to be constructed,
                // before any user code attempts to construct any Resource
                var stack = new Stack(lazyOutputs, options);
                // register the new Stack with the engine
                DeploymentInternal.getInstance().setStack(stack);
                // register the Stack outputs with the engine
                DeploymentInternal.getInstance().registerResourceOutputs(stack, lazyOutputs);
                // run the user code callback wrapped in a future (note that future is eager)
                var callbackFuture = CompletableFuture.supplyAsync(callback);
                DeploymentInternal.getInstance().getRunner().registerTask("callback", callbackFuture);
                // complete the lazyFuture (and lazyOutputs) when user callback future completes
                callbackFuture.whenComplete((value, throwable) -> {
                    if (throwable != null) {
                        lazyFuture.completeExceptionally(throwable);
                    } else {
                        lazyFuture.complete(value);
                    }
                });
                return stack;
            };
        }
    }
}
