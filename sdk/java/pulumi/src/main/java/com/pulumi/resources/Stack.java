package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentInternal;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public final class Stack extends ComponentResource {

    /**
     * The type name that should be used to construct the root component in the tree of Pulumi resources
     * allocated by a deployment. This must be kept up to date with
     * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
     */
    @InternalUse
    public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

    /**
     * The outputs of this stack, if the <code>init</code> callback exited normally.
     */
    private Output<Map<String, Output<?>>> outputs = Output.of(Map.of());

    /**
     * Create a Stack with stack resources created by the <code>init</code> callback.
     * An instance of this will be automatically created when
     * {@link com.pulumi.internal.PulumiInternal#runAsync(Consumer)} is called.
     */
    @InternalUse
    private Stack(Supplier<CompletableFuture<Map<String, Output<?>>>> init, @Nullable StackOptions options) {
        super(
                RootPulumiStackTypeName,
                String.format("%s-%s", Deployment.getInstance().getProjectName(), Deployment.getInstance().getStackName()),
                convertOptions(options)
        );
        // set a derived class as the deployment stack
        DeploymentInternal.getInstance().setStack(this);
        this.outputs = Output.of(runInitAsync(init));
        DeploymentInternal.getInstance().registerResourceOutputs(this, this.outputs);
    }

    private static CompletableFuture<Map<String, Output<?>>> runInitAsync(
            Supplier<CompletableFuture<Map<String, Output<?>>>> init
    ) {
        return CompletableFuture.supplyAsync(init).thenCompose(Function.identity());
    }

    /**
     * @return the stack outputs a.k.a. exports
     */
    public Output<Map<String, Output<?>>> outputs() {
        return this.outputs;
    }

    /**
     * @param name the output (export) name
     * @param shape the type shape to case the output value to
     * @param <T> the output type
     * @return the stack output (a.k.a. exports) for a given name
     */
    public <T> Output<T> output(String name, TypeShape<T> shape) {
        var output = this.outputs.apply(os -> os.get(name));
        return output.applyValue(o -> {
            if (shape.getType().isAssignableFrom(o.getClass())) {
                return shape.getType().cast(o);
            }
            throw new IllegalArgumentException(String.format(
                    "Cannot cast '%s' to the given shape: '%s",
                    o.getClass().getTypeName(),
                    shape.getTypeName()
            ));
        });
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
    public final static class StackInternal {

        @InternalUse
        public static Stack of(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options) {
            return new Stack(callback, options);
        }
    }
}
