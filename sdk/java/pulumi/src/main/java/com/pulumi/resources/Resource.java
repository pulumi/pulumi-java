package com.pulumi.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Alias;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.Strings;
import com.pulumi.core.internal.Urn;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.exceptions.ResourceException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.core.internal.Objects.exceptionSupplier;
import static com.pulumi.core.internal.Objects.require;
import static com.pulumi.resources.Resources.mergeNullableList;
import static com.pulumi.resources.internal.Stack.RootPulumiStackTypeName;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Resource represents a class whose CRUD operations are implemented by a provider plugin.
 */
@ParametersAreNonnullByDefault
public abstract class Resource {

    private final CompletableFuture<Output<String>> urnFuture = new CompletableFuture<>();

    @Export(name = Constants.UrnPropertyName, refs = {String.class})
    private final Output<String> urn = Output.of(urnFuture).apply(urn -> urn);

    private final String type;
    private final String name;

    /**
     * When set to true, protect ensures this resource cannot be deleted.
     */
    private final boolean protect;

    private final List<ResourceTransformation> transformations;

    private final Map<String, ProviderResource> providers;

    protected final Set<Resource> childResources = Collections.synchronizedSet(new HashSet<>());

    protected final boolean remote;

    @Nullable
    private final ProviderResource provider;

    @Nullable
    private final String version;

    /**
     * @see Resource#Resource(String, String, boolean, ResourceArgs, ResourceOptions, boolean, boolean, CompletableFuture)
     */
    protected Resource(String type, String name, boolean custom,
                       ResourceArgs args, ResourceOptions options) {
        this(type, name, custom, args, options, false, false, null);
    }

    /**
     * @see Resource#Resource(String, String, boolean, ResourceArgs, ResourceOptions, boolean, boolean, CompletableFuture)
     */
    protected Resource(
            String type, String name, boolean custom,
            ResourceArgs args, ResourceOptions options,
            boolean remote, boolean dependency
    ) {
        this(type, name, custom, args, options, remote, dependency, null);
    }

    /**
     * Creates and registers a new resource object. The "type" is the fully qualified type token
     * and "name" is the "name" part to of a stable and globally unique URN for the object,
     * "dependsOn" is an optional list of other resources that this resource depends on,
     * controlling the order in which we perform resource operations.
     *
     * @param type       the type of the resource
     * @param name       the unique name of the resource
     * @param custom     true to indicate that this is a custom resource, managed by a plugin
     * @param args       the arguments to use to populate the new resource
     * @param options    a bag of options that control this resource's behavior
     * @param remote     true if this is a remote component resource
     * @param dependency true if this is a synthetic resource used internally for dependency tracking
     * @param packageRef the package reference to use if this resource belongs to a parameterized provider
     */
    protected Resource(
            String type, String name, boolean custom,
            ResourceArgs args, ResourceOptions options,
            boolean remote, boolean dependency,
            CompletableFuture<String> packageRef
    ) {
        var lazy = new LazyFields(
                () -> this.urnFuture,
                this.idFuture().map(f -> () -> f) // this 'idFuture' call must be on top of this constructor to avoid NPEs
        );
        this.remote = remote;

        if (dependency) {
            // this.urn will be set using setter in the subtype constructor after this supertype constructor finishes
            this.type = "";
            this.name = "";
            this.protect = false;
            this.transformations = List.of();
            this.providers = Map.of();
            this.provider = null;
            this.version = null;
            return;
        }

        var exceptionSupplier = exceptionSupplier(
                () -> new ResourceException(this),
                (String msg) -> new ResourceException(msg, this)
        );
        this.type = require(Strings::isNonEmptyOrNull, type,
                () -> "expected a resource type, got empty or null",
                exceptionSupplier
        );
        this.name = require(Strings::isNonEmptyOrNull, name,
                () -> "expected a resource name, got empty or null",
                exceptionSupplier
        );

        // TODO: C# initializes OutputCompletionSource fields here, the fact that we don't might be a bug

        // Before anything else - if there are transformations registered, invoke them in order
        // to transform the properties and options assigned to this resource.
        var parent = Objects.equals(type, RootPulumiStackTypeName)
                ? null
                : (options.parent == null ? DeploymentInternal.getInstance().getStack() : options.parent);

        var transformations = new ArrayList<>(options.getResourceTransformations());
        if (parent != null) {
            transformations.addAll(parent.transformations);
        }
        this.transformations = List.copyOf(transformations);

        for (var transformation : transformations) {
            var tres = transformation.apply(
                    new ResourceTransformation.Args(this, args, options)
            );
            if (tres.isPresent()) {
                if (tres.get().options().parent != options.parent) {
                    // This is currently not allowed because the parent tree is needed to
                    // establish what transformation to apply in the first place, and to compute
                    // inheritance of other resource options in the Resource constructor before
                    // transformations are run (so modifying it here would only even partially
                    // take effect).
                    // It's theoretically possible this restriction could be
                    // lifted in the future, but for now just disallow re-parenting resources in
                    // transformations to be safe.
                    throw new IllegalArgumentException("Transformations cannot currently be used to change the 'parent' of a resource.");
                }

                args = tres.get().args();
                options = tres.get().options();
            }
        }

        // Make a shallow clone of options to ensure we don't modify the value passed in.

        @Nullable
        var componentOpts = options instanceof ComponentResourceOptions
                ? ((ComponentResourceOptions) options).copy()
                : null;
        @Nullable
        var customOpts = options instanceof CustomResourceOptions
                ? ((CustomResourceOptions) options).copy()
                : null;

        if (options.provider != null
                && componentOpts != null
                && componentOpts.getProviders().size() > 0) {
            throw new ResourceException("Do not supply both 'provider' and 'providers' options to a ComponentResource.", options.parent);
        }

        // Check the parent type if one exists and fill in any default options.
        var thisProviders = new HashMap<String, ProviderResource>();

        if (options.parent != null) {
            var parentResource = options.parent;
            // the 'childResources' is a Synchronized Collection, so this is safe operation
            parentResource.childResources.add(this);

            options.protect = options.protect || parentResource.protect; // TODO: is this logic good?
            thisProviders.putAll(options.parent.providers);
        }

        // TODO: most of this logic is avoidable by just removing the 'provider' and using 'providers' instead
        if (custom) {
            var provider = customOpts == null ? null : customOpts.provider;
            if (provider == null) {
                if (options.parent != null) {
                    // If no provider was given, but we have a parent, then inherit the provider from our parent.
                    options.provider = ResourceInternal.getProvider(options.parent, this.type);
                }
            } else {
                // If a provider was specified, add it to the providers map under this type's package so that
                // any children of this resource inherit its provider.
                var typeComponents = Urn.Type.parse(this.type);
                if (typeComponents.module.isPresent()) {
                    thisProviders.put(typeComponents.package_, provider);
                }
            }
        } else {
            // Note: we've checked above that at most one of options.provider or options.providers is set.

            // If options.provider is set, treat that as if we were given a array of provider
            // with that single value in it. Otherwise, take the array of providers, convert it
            // to a map and combine with any providers we've already set from our parent.
            var providerList = options.provider != null
                    ? List.of(options.provider)
                    : componentOpts == null ? null : componentOpts.getProviders();

            thisProviders.putAll(convertToProvidersMap(providerList));
        }

        this.protect = options.protect;
        this.provider = custom ? options.provider : null;
        this.version = options.version;
        this.providers = Map.copyOf(thisProviders);

        // Finish initialisation with reflection asynchronously
        DeploymentInternal.getInstance().readOrRegisterResource(
                this, remote, DependencyResource::new, args, options, lazy,
                packageRef
        );
    }

    /**
     * Lazy Initialization method called at the beginning of the constructor.
     * Resources with the id field must override this method.
     */
    protected Optional<CompletableFuture<Output<String>>> idFuture() {
        return Optional.empty();
    }

    /**
     * The type assigned to the resource at construction.
     *
     * @return the type of the resource
     * @deprecated use {@link #pulumiResourceType()}
     */
    @Deprecated
    public String getResourceType() {
        return type;
    }

    /**
     * The Pulumi type assigned to the resource at construction.
     *
     * @return the type of the Pulumi resource
     */
    public String pulumiResourceType() {
        return type;
    }

    /**
     * The name assigned to the resource at construction.
     *
     * @return the name of the resource
     * @deprecated use {@link #pulumiResourceName()}
     */
    @Deprecated
    public String getResourceName() {
        return name;
    }

    /**
     * The Pulumi name assigned to the resource at construction.
     *
     * @return the name of the Pulumi resource
     */
    public String pulumiResourceName() {
        return name;
    }

    /**
     * The child resources of this resource. We use these (only from a @see {@link ComponentResource}) to
     * allow code to "dependOn" a @see {@link ComponentResource} and have that effectively mean that it is
     * depending on all the @see {@link ComponentResource} children of that component.
     * <p>
     * Important! We only walk through @see {@link ComponentResource}s. They're the only resources that
     * serve as an aggregation of other primitive (i.e.custom) resources.
     * While a custom resource can be a parent of other resources, we don't want to ever depend
     * on those child resource.
     * If we do, it's simple to end up in a situation where we end up depending on a
     * child resource that has a data cycle dependency due to the data passed into it.
     * This would be pretty nonsensical as there is zero need for a custom resource to
     * ever need to reference the urn of a component resource.
     * So it's acceptable if that sort of pattern failed in practice.
     *
     * @return the child resources of this resource
     * @deprecated use {@link #pulumiChildResources()}
     */
    @Deprecated
    public Set<Resource> getChildResources() {
        return childResources;
    }

    /**
     * The child resources of this Pulumi resource. We use these (only from a @see {@link ComponentResource}) to
     * allow code to "dependOn" a @see {@link ComponentResource} and have that effectively mean that it is
     * depending on all the @see {@link ComponentResource} children of that component.
     * <p>
     * Important! We only walk through @see {@link ComponentResource}s. They're the only resources that
     * serve as an aggregation of other primitive (i.e.custom) resources.
     * While a custom resource can be a parent of other resources, we don't want to ever depend
     * on those child resource.
     * If we do, it's simple to end up in a situation where we end up depending on a
     * child resource that has a data cycle dependency due to the data passed into it.
     * This would be pretty nonsensical as there is zero need for a custom resource to
     * ever need to reference the urn of a component resource.
     * So it's acceptable if that sort of pattern failed in practice.
     *
     * @return the child resources of this Pulumi resource
     */
    public Set<Resource> pulumiChildResources() {
        return childResources;
    }

    /**
     * Urn is the stable logical URN used to distinctly address a resource, both before and after deployments.
     *
     * @return the stable logical URN
     * @deprecated use {@link #urn()}
     */
    @Deprecated
    public Output<String> getUrn() {
        return this.urn;
    }

    /**
     * Pulumi URN is the stable logical identifier used to distinctly address a resource,
     * both before and after deployments.
     *
     * @return the stable logical Pulumi URN
     */
    public Output<String> urn() {
        return this.urn;
    }

    private static ImmutableMap<String, ProviderResource> convertToProvidersMap(@Nullable List<ProviderResource> providers) {
        var result = ImmutableMap.<String, ProviderResource>builder();
        if (providers != null) {
            for (var provider : providers) {
                var pkg = Internal.from(provider).getPackage();
                result.put(pkg, provider);
            }
        }

        return result.build();
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static class ResourceInternal {

        protected final Resource resource;

        protected ResourceInternal(Resource resource) {
            this.resource = requireNonNull(resource);
        }

        public static ResourceInternal from(Resource r) {
            return new ResourceInternal(r);
        }

        @InternalUse
        public boolean getRemote() {
            return this.resource.remote;
        }

        /**
         * The specified provider or provider determined from the parent for custom resources.
         */
        @InternalUse
        public Optional<ProviderResource> getProvider() {
            return Optional.ofNullable(this.resource.provider);
        }

        /**
         * Fetches the provider for the given module member, if any.
         */
        @InternalUse
        public Optional<ProviderResource> getProvider(String moduleMember) {
            return Optional.ofNullable(getProvider(this.resource, moduleMember));
        }

        /**
         * The specified provider version.
         */
        @InternalUse
        public Optional<String> getVersion() {
            return Optional.ofNullable(this.resource.version);
        }

        @InternalUse
        public void setUrn(Output<String> urn) {
            if (!trySetUrn(urn)) {
                throw new IllegalStateException("urn cannot be set twice, must be 'null' for 'setUrn' to work");
            }
        }

        @InternalUse
        public boolean trySetUrn(Output<String> urn) {
            requireNonNull(urn);
            return this.resource.urnFuture.complete(urn);
        }

        /**
         * Fetches the provider for the given module member, if any.
         *
         * @param moduleMember the module member to look for
         * @return the @see {@link ProviderResource} or empty if not found
         */
        @Nullable
        @InternalUse
        static public ProviderResource getProvider(Resource resource, String moduleMember) {
            var memComponents = moduleMember.split(":");
            if (memComponents.length != 3) {
                // TODO: why do we silently ignore invalid type?
                return null;
            }

            return resource.providers.getOrDefault(memComponents[0], null);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @InternalUse
    @ParametersAreNonnullByDefault
    public static class LazyFields {
        private final LazyField<String> urn;
        private final Optional<LazyField<String>> id;

        protected LazyFields(LazyField<String> urn, Optional<LazyField<String>> id) {
            this.urn = requireNonNull(urn);
            this.id = requireNonNull(id);
        }

        public LazyField<String> urn() {
            return this.urn;
        }

        public Optional<LazyField<String>> id() {
            return this.id;
        }
    }

    @InternalUse
    public interface LazyField<T> {
        CompletableFuture<Output<T>> future();

        default void completeOrThrow(Output<T> value) {
            if (!complete(value)) {
                throw new IllegalStateException("lazy field cannot be set twice, must be 'null' for 'set' to work");
            }
        }

        default boolean complete(Output<T> value) {
            requireNonNull(value);
            return future().complete(value);
        }

        default boolean fail(Throwable throwable) {
            requireNonNull(throwable);
            return complete(Output.of(CompletableFuture.failedFuture(throwable)));
        }
    }
}