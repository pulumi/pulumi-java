package io.pulumi.resources;

import io.pulumi.core.Output;
import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.annotations.OutputExport;

import javax.annotation.Nullable;

/**
 * CustomResource is a resource whose create, read, update, and delete (CRUD) operations are
 * managed by performing external operations on some physical entity. The engine understands
 * how to diff and perform partial updates of them, and these CRUD operations are implemented
 * in a dynamically loaded plugin for the defining package.
 */
public class CustomResource extends Resource {

    @OutputExport(name = Constants.IdPropertyName, type = String.class)
    private /* final-ish */ Output<String> id; // this can be set only once with the setter or reflection

    /**
     * Creates and registers a new managed resource. @see {@link CustomResource#CustomResource(String, String, ResourceArgs, CustomResourceOptions, boolean)}
     *
     * @param type       The type of the resource.
     * @param name       The unique name of the resource.
     * @param args       The arguments to use to populate the new resource.
     * @param dependency True if this is a synthetic resource used internally for dependency tracking.
     */
    protected CustomResource(String type, String name, @Nullable ResourceArgs args, boolean dependency) {
        this(type, name, args, null, dependency);
    }

    /**
     * Creates and registers a new managed resource. @see {@link CustomResource#CustomResource(String, String, ResourceArgs, CustomResourceOptions, boolean)}
     *
     * @param type    The type of the resource.
     * @param name    The unique name of the resource.
     * @param args    The arguments to use to populate the new resource.
     * @param options A bag of options that control this resource's behavior.
     */
    public CustomResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
        this(type, name, args, options, false);
    }

    /**
     * Creates and registers a new managed resource. Parameter {@code type} is the fully
     * qualified type token and {@code name} is the "name" part to use in creating a
     * stable and globally unique URN for the object. @see {@link ResourceOptions#getDependsOn()}
     * is an optional list of other resources that this resource depends on, controlling the
     * order in which we perform resource operations. Creating an instance does not necessarily
     * perform a create on the physical entity which it represents, and instead, this is
     * dependent upon the diffing of the new goal state compared to the current known resource
     * state.
     *
     * @param type       The type of the resource.
     * @param name       The unique name of the resource.
     * @param args       The arguments to use to populate the new resource.
     * @param options    A bag of options that control this resource's behavior.
     * @param dependency True if this is a synthetic resource used internally for dependency tracking.
     */
    protected CustomResource(
            String type,
            String name,
            @Nullable ResourceArgs args,
            @Nullable CustomResourceOptions options,
            boolean dependency
    ) {
        super(type, name, true,
                args == null ? ResourceArgs.Empty : args,
                options == null ? CustomResourceOptions.Empty : options,
                false, dependency);
    }

    /**
     * Id is the provider-assigned unique ID for this managed resource. It is set during
     * deployments and may be missing (unknown) during planning phases.
     */
    public Output<String> getId() {
        return Output.ofNullable(this.id);
    }

    /**
     * More: @see {@link #getId()}
     * @param id the the provider-assigned unique ID to set
     */
    protected void setId(@Nullable Output<String> id) {
        if (this.id == null) {
            this.id = id;
        } else {
            throw new IllegalStateException("id cannot be set twice, must be null for setId to work");
        }
    }
}
