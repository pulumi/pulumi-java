package com.pulumi.resources;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * CustomResource is a resource whose create, read, update, and delete (CRUD) operations are
 * managed by performing external operations on some physical entity. The engine understands
 * how to diff and perform partial updates of them, and these CRUD operations are implemented
 * in a dynamically loaded plugin for the defining package.
 */
@ParametersAreNonnullByDefault
public class CustomResource extends Resource {

    private CompletableFuture<Output<String>> idFuture; // effectively final, lazy init

    @Export(name = Constants.IdPropertyName, refs = {String.class})
    private Output<String> id; // effectively final, lazy init

    /**
     * Creates and registers a new managed resource. @see {@link CustomResource#CustomResource(String, String, ResourceArgs, CustomResourceOptions, boolean, CompletableFuture)}
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
     * Creates and registers a new managed resource. @see {@link CustomResource#CustomResource(String, String, ResourceArgs, CustomResourceOptions, boolean, CompletableFuture)}
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
     * Creates and registers a new managed resource. @see {@link CustomResource#CustomResource(String, String, ResourceArgs, CustomResourceOptions, boolean, CompletableFuture)}
     *
     * @param type    The type of the resource.
     * @param name    The unique name of the resource.
     * @param args    The arguments to use to populate the new resource.
     * @param options A bag of options that control this resource's behavior.
     * @param dependency True if this is a synthetic resource used internally for dependency tracking.
     */
    protected CustomResource(
            String type,
            String name,
            @Nullable ResourceArgs args,
            @Nullable CustomResourceOptions options,
            boolean dependency
    ) {
        this(type, name, args, options, dependency, null);
    }

    /**
     * Creates and registers a new managed resource. Parameter {@code type} is the fully
     * qualified type token and {@code name} is the "name" part to use in creating a
     * stable and globally unique URN for the object. @see {@link ResourceOptions#getDependsOn()}
     * is an optional list of other resources that this resource depends on, controlling the
     * order in which we perform resource operations. Creating an instance does not necessarily
     * perform a creation on the physical entity which it represents, and instead, this is
     * dependent upon the diffing of the new goal state compared to the current known resource
     * state.
     *
     * @param type       The type of the resource.
     * @param name       The unique name of the resource.
     * @param args       The arguments to use to populate the new resource.
     * @param options    A bag of options that control this resource's behavior.
     * @param dependency True if this is a synthetic resource used internally for dependency tracking.
     * @param packageRef The package reference to use for this resource.
     */
    protected CustomResource(
            String type,
            String name,
            @Nullable ResourceArgs args,
            @Nullable CustomResourceOptions options,
            boolean dependency,
            @Nullable CompletableFuture<String> packageRef
    ) {
        super(type, name, true,
                args == null ? ResourceArgs.Empty : args,
                options == null ? CustomResourceOptions.Empty : options,
                false, dependency, packageRef);
    }

    protected Optional<CompletableFuture<Output<String>>> idFuture() {
        // TODO: move initialization to fields and make them final
        //       if registration is ever moved outside of the constructor
        this.idFuture = new CompletableFuture<>();
        this.id = Output.of(idFuture).apply(id -> id);
        return Optional.of(this.idFuture);
    }

    /**
     * ID is the provider-assigned unique ID for this managed resource. It is set during
     * deployments and may be missing (unknown) during planning phases.
     *
     * @return the provider-assigned unique ID
     * @deprecated use {@link #id()}
     */
    @Deprecated
    public Output<String> getId() {
        return id();
    }

    /**
     * Pulumi ID is the provider-assigned unique ID for this managed resource.
     * It is set during deployments and may be missing (unknown) during planning phases.
     *
     * @return the provider-assigned unique Pulumi ID
     */
    public Output<String> id() {
        return this.id;
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static class CustomResourceInternal extends ResourceInternal {

        private final CustomResource resource;

        protected CustomResourceInternal(CustomResource resource) {
            super(resource);
            this.resource = requireNonNull(resource);
        }

        public static CustomResourceInternal from(CustomResource r) {
            return new CustomResourceInternal(r);
        }

        /**
         * More: {@link #id()}
         *
         * @param id the the provider-assigned unique ID to set
         * @throws NullPointerException if id is null
         */
        @InternalUse
        public void setId(Output<String> id) {
            if (!trySetId(id)) {
                throw new IllegalStateException("'id' cannot be set twice, must be 'null' for 'setId' to work");
            }
        }

        @InternalUse
        public boolean trySetId(Output<String> id) {
            requireNonNull(id);
            return this.resource.idFuture.complete(id);
        }
    }
}