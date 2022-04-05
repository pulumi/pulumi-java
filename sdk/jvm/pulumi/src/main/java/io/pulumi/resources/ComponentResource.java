package io.pulumi.resources;

import io.pulumi.core.Output;
import io.pulumi.core.internal.Internal.InternalField;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.internal.DeploymentInternal;

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

    @SuppressWarnings("unused")
    @InternalField
    private final ComponentResourceInternal internal = new ComponentResourceInternal(this);

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean)}.
     *
     * @param type The type of the resource
     * @param name The unique name of the resource
     */
    public ComponentResource(String type, String name) {
        this(type, name, null /* no options */);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param options A bag of options that control this resource's behavior
     */
    public ComponentResource(String type, String name, @Nullable ComponentResourceOptions options) {
        this(type, name, options, false);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param options A bag of options that control this resource's behavior
     * @param remote  True if this is a remote component resource
     */
    public ComponentResource(String type, String name, @Nullable ComponentResourceOptions options, boolean remote) {
        this(type, name, ResourceArgs.Empty, options, remote);
    }

    /**
     * Creates and registers a new component resource, @see {@link #ComponentResource(String, String, ResourceArgs, ComponentResourceOptions, boolean)}.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param args    The arguments to use to populate the new resource
     * @param options A bag of options that control this resource's behavior
     */
    public ComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options) {
        this(type, name, args, options, false);
    }

    /**
     * Creates and registers a new component resource.
     * Given "type" is the fully qualified type token and "name" is the "name" part
     * to use in creating a stable and globally unique URN for the object.
     * The "options.parent" is the optional parent for this component,
     * and "options.dependsOn" is an optional list of other resources that
     * this resource depends on, controlling the order in which we perform resource operations.
     *
     * @param type    The type of the resource
     * @param name    The unique name of the resource
     * @param args    The arguments to use to populate the new resource
     * @param options A bag of options that control this resource's behavior
     * @param remote  True if this is a remote component resource
     */
    public ComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options, boolean remote) {
        super(type, name, false,
                args == null ? ResourceArgs.Empty : args,
                options == null ? ComponentResourceOptions.Empty : options,
                remote, false
        );
    }

    protected void registerOutputs(Map<String, Output<?>> outputs) {
        requireNonNull(outputs);
        registerOutputs(CompletableFuture.completedFuture(outputs));
    }

    protected void registerOutputs(CompletableFuture<Map<String, Output<?>>> outputs) {
        requireNonNull(outputs);
        registerOutputs(Output.of(outputs));
    }

    protected void registerOutputs(Output<Map<String, Output<?>>> outputs) {
        requireNonNull(outputs);
        DeploymentInternal.getInstance().registerResourceOutputs(this, outputs);
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static class ComponentResourceInternal extends ResourceInternal {

        protected ComponentResourceInternal(ComponentResource resource) {
            super(resource);
        }
    }
}
