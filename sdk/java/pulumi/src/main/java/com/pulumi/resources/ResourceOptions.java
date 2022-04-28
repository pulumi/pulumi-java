package com.pulumi.resources;

import com.pulumi.core.Alias;
import com.pulumi.core.Output;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pulumi.resources.Resources.mergeNullableList;

/**
 * ResourceOptions is a bag of optional settings that control a resource's behavior.
 */
public abstract class ResourceOptions {

    @Nullable
    protected Output<String> id;
    @Nullable
    protected Resource parent;
    @Nullable
    protected Output<List<Resource>> dependsOn;
    protected boolean protect;
    @Nullable
    protected List<String> ignoreChanges;
    @Nullable
    protected String version;
    @Nullable
    protected ProviderResource provider;
    @Nullable
    protected CustomTimeouts customTimeouts;
    @Nullable
    protected List<ResourceTransformation> resourceTransformations;
    @Nullable
    protected List<Output<Alias>> aliases;
    @Nullable
    protected String urn;
    @Nullable
    protected List<String> replaceOnChanges;

    protected ResourceOptions() { /* empty */ }

    protected ResourceOptions(
            @Nullable Output<String> id,
            @Nullable Resource parent,
            @Nullable Output<List<Resource>> dependsOn,
            boolean protect,
            @Nullable List<String> ignoreChanges,
            @Nullable String version,
            @Nullable ProviderResource provider,
            @Nullable CustomTimeouts customTimeouts,
            @Nullable List<ResourceTransformation> resourceTransformations,
            @Nullable List<Output<Alias>> aliases,
            @Nullable String urn,
            @Nullable List<String> replaceOnChanges
    ) {
        this.id = id;
        this.parent = parent;
        this.dependsOn = dependsOn;
        this.protect = protect;
        this.ignoreChanges = ignoreChanges;
        this.version = version;
        this.provider = provider;
        this.customTimeouts = customTimeouts;
        this.resourceTransformations = resourceTransformations;
        this.aliases = aliases;
        this.urn = urn;
    }

    protected static abstract class Builder<T extends ResourceOptions, B extends Builder<T, B>> {

        protected final T options;

        protected Builder(T options) {
            this.options = options;
        }

        public B id(@Nullable Output<String> id) {
            options.id = id;
            return (B) this;
        }

        public B id(@Nullable String id) {
            options.id = Output.ofNullable(id);
            return (B) this;
        }

        public B parent(@Nullable Resource parent) {
            options.parent = parent;
            return (B) this;
        }

        public B dependsOn(Resource... dependsOn) {
            return this.dependsOn(List.of(dependsOn));
        }

        public B dependsOn(@Nullable Output<List<Resource>> dependsOn) {
            options.dependsOn = dependsOn;
            return (B) this;
        }

        public B dependsOn(@Nullable List<Resource> dependsOn) {
            options.dependsOn = Output.ofNullable(dependsOn);
            return (B) this;
        }

        public B protect(boolean protect) {
            options.protect = protect;
            return (B) this;
        }

        public B ignoreChanges(String... ignoreChanges) {
            return this.ignoreChanges(List.of(ignoreChanges));
        }

        public B ignoreChanges(@Nullable List<String> ignoreChanges) {
            options.ignoreChanges = ignoreChanges;
            return (B) this;
        }

        public B version(@Nullable String version) {
            options.version = version;
            return (B) this;
        }

        public B provider(@Nullable ProviderResource provider) {
            options.provider = provider;
            return (B) this;
        }

        public B customTimeouts(@Nullable CustomTimeouts customTimeouts) {
            options.customTimeouts = customTimeouts;
            return (B) this;
        }

        public B resourceTransformations(ResourceTransformation... resourceTransformations) {
            return this.resourceTransformations(List.of(resourceTransformations));
        }

        public B resourceTransformations(@Nullable List<ResourceTransformation> resourceTransformations) {
            options.resourceTransformations = resourceTransformations;
            return (B) this;
        }

        public B aliases(Alias... aliases) {
            return this.aliases(List.of(aliases)
                .stream()
                .map(alias -> Output.of(alias))
                .collect(Collectors.toList()));
        }

        public B aliases(Output<Alias>... aliases) {
            return this.aliases(List.of(aliases));
        }

        public B aliases(@Nullable List<Output<Alias>> aliases) {
            options.aliases = aliases;
            return (B) this;
        }

        public B urn(@Nullable String urn) {
            options.urn = urn;
            return (B) this;
        }

        public B replaceOnChanges(String... replaceOnChanges) {
            return this.replaceOnChanges(List.of(replaceOnChanges));
        }

        public B replaceOnChanges(@Nullable List<String> replaceOnChanges) {
            options.replaceOnChanges = replaceOnChanges;
            return (B) this;
        }
    }

    /**
     * An optional existing ID to load, rather than create.
     */
    public Optional<Output<String>> getId() {
        return Optional.ofNullable(id);
    }

    /**
     * An optional parent resource to which this resource belongs.
     */
    public Optional<Resource> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Optional additional explicit dependencies on other resources.
     */
    public Output<List<Resource>> getDependsOn() {
        return this.dependsOn == null ? Output.ofList() : this.dependsOn;
    }

    /**
     * When set to true, protect ensures this resource cannot be deleted.
     */
    public boolean isProtect() {
        return protect;
    }

    /**
     * Ignore changes to any of the specified properties.
     */
    public List<String> getIgnoreChanges() {
        return this.ignoreChanges == null ? List.of() : List.copyOf(this.ignoreChanges);
    }

    /**
     * An optional version, corresponding to the version of the provider plugin that should be
     * used when operating on this resource. This version overrides the version information
     * inferred from the current package and should rarely be used.
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * An optional provider to use for this resource's CRUD operations. If no provider is
     * supplied, the default provider for the resource's package will be used. The default
     * provider is pulled from the parent's provider bag (@see {@link ComponentResourceOptions#getProviders()}).
     * <p>
     * If this is a @see {@link ComponentResourceOptions} do not provide both @see {@link #provider}
     * and @see {@link ComponentResourceOptions#getProviders()}.
     */
    public Optional<ProviderResource> getProvider() {
        return Optional.ofNullable(provider);
    }

    /**
     * An optional CustomTimeouts configuration block.
     */
    public Optional<CustomTimeouts> getCustomTimeouts() {
        return Optional.ofNullable(customTimeouts);
    }

    /**
     * Optional list of transformations to apply to this resource during construction.
     * The transformations are applied in order, and are applied prior to transformation applied to
     * parents walking from the resource up to the stack.
     */
    public List<ResourceTransformation> getResourceTransformations() {
        return this.resourceTransformations == null ? List.of() : List.copyOf(this.resourceTransformations);
    }

    /**
     * An optional list of aliases to treat this resource as matching.
     */
    public List<Output<Alias>> getAliases() {
        return this.aliases == null ? List.of() : List.copyOf(this.aliases);
    }

    /**
     * The URN of a previously-registered resource of this type to read from the engine.
     */
    public Optional<String> getUrn() {
        return Optional.ofNullable(urn);
    }

    /**
     * Changes to any of these property paths will force a replacement.
     * If this list includes `"*"`, changes to any properties will force a replacement.
     * Initialization errors from previous deployments will require replacement
     * instead of update only if `"*"` is passed.
     */
    public List<String> getReplaceOnChanges() {
        return this.replaceOnChanges == null ? List.of() : List.copyOf(this.replaceOnChanges);
    }

    protected static <T extends ResourceOptions> T mergeSharedOptions(T options1, T options2) {
        return mergeSharedOptions(options1, options2, null);
    }

    protected static <T extends ResourceOptions> T mergeSharedOptions(T options1, T options2, @Nullable Output<String> id) {
        Objects.requireNonNull(options1);
        Objects.requireNonNull(options2);

        options1.id = options2.id == null ? options1.id : options2.id;
        options1.parent = options2.parent == null ? options1.parent : options2.parent;
        options1.protect = options1.protect || options2.protect;
        options1.urn = options2.urn == null ? options1.urn : options2.urn;
        options1.version = options2.version == null ? options1.version : options2.version;
        options1.provider = options2.provider == null ? options1.provider : options2.provider;
        options1.customTimeouts = options2.customTimeouts == null ? options1.customTimeouts : options2.customTimeouts;

        options1.ignoreChanges = mergeNullableList(options1.ignoreChanges, options2.ignoreChanges);
        options1.resourceTransformations = mergeNullableList(options1.resourceTransformations, options2.resourceTransformations);
        options1.aliases = mergeNullableList(options1.aliases, options2.aliases);
        options1.replaceOnChanges = mergeNullableList(options1.replaceOnChanges, options2.replaceOnChanges);

        options1.dependsOn = Output.concatList(options1.dependsOn, options2.dependsOn);

        // Override the ID if one was specified for consistency with other language SDKs.
        options1.id = id == null ? options1.id : id;
        return options1;
    }
}
