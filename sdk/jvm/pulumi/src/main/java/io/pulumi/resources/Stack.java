package io.pulumi.resources;

import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.DeploymentInternal;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class Stack extends ComponentResource {

    /**
     * The outputs of this stack, if the <code>init</code> callback exited normally.
     */
    private Output<Map<String, Output<?>>> outputs = Output.of(Map.of());

    /**
     * Create a Stack with stack resources defined in derived class constructor.
     * Also @see {@link #Stack(StackOptions)}
     */
    public Stack() {
        this(null);
    }

    /**
     * Create a Stack with stack resources defined in derived class constructor.
     *
     * @param options optional stack options
     */
    public Stack(@Nullable StackOptions options) {
        super(
                StackInternal.RootPulumiStackTypeName,
                String.format("%s-%s", Deployment.getInstance().getProjectName(), Deployment.getInstance().getStackName()),
                convertOptions(options)
        );
        // set a derived class as the deployment stack
        DeploymentInternal.getInstance().setStack(this);
    }

    /**
     * Create a Stack with stack resources created by the <code>init</code> callback.
     * An instance of this will be automatically created when
     * any @see {@link Deployment#runAsync(Supplier)} overload is called.
     */
    @InternalUse
    private Stack(Supplier<CompletableFuture<Map<String, Output<?>>>> init, @Nullable StackOptions options) {
        this(options);
        try {
            this.outputs = Output.of(runInitAsync(init));
        } finally {
            this.registerOutputs(this.outputs);
        }
    }

    private static CompletableFuture<Map<String, Output<?>>> runInitAsync(
            Supplier<CompletableFuture<Map<String, Output<?>>>> init
    ) {
        return CompletableFuture.supplyAsync(init).thenCompose(Function.identity());
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
         * Validate the values and register them as stack outputs.
         */
        @InternalUse
        public void registerPropertyOutputs() {
            this.stack.outputs = Output.of(findOutputs(this.stack));
            this.stack.registerOutputs(this.stack.outputs);
        }

        /**
         * The type name that should be used to construct the root component in the tree of Pulumi resources
         * allocated by a deployment. This must be kept up to date with
         * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
         */
        @InternalUse
        public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

        @InternalUse
        public static Stack of(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options) {
            return new Stack(callback, options);
        }
    }
}
