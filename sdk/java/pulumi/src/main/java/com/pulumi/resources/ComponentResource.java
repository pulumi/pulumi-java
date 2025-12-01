package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentInternal;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A @see {@link Resource} that aggregates one or more other child resources into a higher
 * level abstraction. The component resource itself is a resource, but does not require custom
 * CRUD operations for provisioning.
 */
public class ComponentResource extends Resource {

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean, CompletableFuture)}.
     *
     * @param type The type of the resource
     * @param name The unique name of the resource
     */
    public ComponentResource(String type, String name) {
        this(type, name, ResourceArgs.Empty, null /* no options */, false, null);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean, CompletableFuture)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param options A bag of options that control this resource's behavior
     */
    public ComponentResource(String type, String name, @Nullable ComponentResourceOptions options) {
        this(type, name, ResourceArgs.Empty, options, false, null);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean, CompletableFuture)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param options A bag of options that control this resource's behavior
     * @param remote  True if this is a remote component resource
     */
    public ComponentResource(String type, String name, @Nullable ComponentResourceOptions options, boolean remote) {
        this(type, name, ResourceArgs.Empty, options, remote, null);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean, CompletableFuture)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param args    The arguments to use to populate the new resource
     * @param options A bag of options that control this resource's behavior
     */
    public ComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options) {
        this(type, name, args, options, false, null);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean, CompletableFuture)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param args    The arguments to use to populate the new resource
     * @param remote  True if this is a remote component resource
     * @param options A bag of options that control this resource's behavior
     */
    public ComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options, boolean remote) {
        this(type, name, args, options, remote, null);
    }

    /**
     * Creates and registers a new component resource.
     * Given "type" is the fully qualified type token and "name" is the "name" part
     * to use in creating a stable and globally unique URN for the object.
     * The "options.parent" is the optional parent for this component,
     * and "options.dependsOn" is an optional list of other resources that
     * this resource depends on, controlling the order in which we perform resource operations.
     *
     * @param type       The type of the resource
     * @param name       The unique name of the resource
     * @param args       The arguments to use to populate the new resource
     * @param options    A bag of options that control this resource's behavior
     * @param remote     True if this is a remote component resource
     * @param packageRef The package reference to use for this resource
     */
    public ComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options, boolean remote, CompletableFuture<String> packageRef) {
        super(type, name, false,
                args == null ? ResourceArgs.Empty : args,
                options == null ? ComponentResourceOptions.Empty : options,
                remote, false, packageRef
        );
    }

    /**
     * Registers the output properties for this component resource.
     *
     * @param outputs a map of output property names to their corresponding {@link Output} values; must not be null
     * @see #registerOutputs(CompletableFuture)
     * @see #registerOutputs(Output)
     */
    protected void registerOutputs(Map<String, Output<?>> outputs) {
        requireNonNull(outputs);
        registerOutputs(CompletableFuture.completedFuture(outputs));
    }

    /**
     * Registers the output properties for this component resource asynchronously.
     *
     * @param outputs a {@link CompletableFuture} that will complete with a map of output property names to their {@link Output} values; must not be null
     * @see #registerOutputs(Map)
     * @see #registerOutputs(Output)
     */
    protected void registerOutputs(CompletableFuture<Map<String, Output<?>>> outputs) {
        requireNonNull(outputs);
        registerOutputs(Output.of(outputs));
    }

    /**
     * Registers the output properties for this component resource as an {@link Output}.
     *
     * @param outputs an {@link Output} containing a map of output property names to their {@link Output} values; must not be null
     */
    protected void registerOutputs(Output<Map<String, Output<?>>> outputs) {
        requireNonNull(outputs);
        DeploymentInternal.getInstance().registerResourceOutputs(this, outputs);
    }

    /**
     * Internal helper class of {@link ComponentResource}.
     *
     * @see ComponentResource
     * @see ResourceInternal
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static class ComponentResourceInternal extends ResourceInternal {
        /**
         * Constructs a new internal accessor for the given {@link ComponentResource}.
         *
         * @param resource the {@link ComponentResource} instance to wrap
         */
        protected ComponentResourceInternal(ComponentResource resource) {
            super(resource);
        }

        /**
         * Creates a new {@link ComponentResourceInternal} accessor for the specified resource.
         *
         * @param r the {@link ComponentResource} to wrap
         * @return a new {@link ComponentResourceInternal} instance for the given resource
         */
        public static ComponentResourceInternal from(ComponentResource r) {
            return new ComponentResourceInternal(r);
        }
    }
}