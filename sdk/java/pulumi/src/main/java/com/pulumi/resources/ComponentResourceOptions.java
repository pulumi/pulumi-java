package com.pulumi.resources;

import com.pulumi.core.Alias;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Copyable;
import com.pulumi.core.internal.Objects;

import javax.annotation.Nullable;
import java.util.List;

import static com.pulumi.resources.Resources.copyNullable;
import static com.pulumi.resources.Resources.copyNullableList;
import static com.pulumi.resources.Resources.mergeNullableList;

/**
 * A bag of optional settings that control a @see {@link ComponentResource} behavior.
 */
public final class ComponentResourceOptions extends ResourceOptions implements Copyable<ComponentResourceOptions> {

    public static final ComponentResourceOptions Empty = new ComponentResourceOptions();

    @Nullable
    private List<ProviderResource> providers;

    private ComponentResourceOptions() {
        // Empty
    }

    public ComponentResourceOptions(
            @Nullable Output<String> id,
            @Nullable Resource parent,
            @Nullable Output<List<Resource>> dependsOn,
            @Nullable boolean protect,
            @Nullable List<String> ignoreChanges,
            @Nullable String version,
            @Nullable CustomTimeouts customTimeouts,
            @Nullable List<ResourceTransformation> resourceTransformations,
            @Nullable List<Output<Alias>> aliases,
            @Nullable String urn,
            @Nullable List<String> replaceOnChanges,
            @Nullable boolean retainOnDelete,
            @Nullable String pluginDownloadURL,
            @Nullable List<String> hideDiffs,
            @Nullable List<ProviderResource> providers,
            @Nullable List<Resource> replaceWith,
            @Nullable Output<?> replacementTrigger
    ) {
        super(id, parent, dependsOn, protect, ignoreChanges, version, null /* use providers instead */, customTimeouts,
                resourceTransformations, aliases, urn, replaceOnChanges, retainOnDelete, pluginDownloadURL, hideDiffs, replaceWith, replacementTrigger);
        this.providers = providers;
        Objects.requireNullState(this.provider, () -> "expected 'provider' to be null, use 'providers' instead");
    }

    public static Builder builder() {
        return new Builder(new ComponentResourceOptions());
    }

    /**
     * The {@link ComponentResourceOptions} builder.
     */
    public static final class Builder extends ResourceOptions.Builder<ComponentResourceOptions, Builder> {

        private Builder(ComponentResourceOptions options) {
            super(options);
        }

        /**
         * @param providers optional list of providers to use for child resources
         * @return the {@link Builder}
         */
        public Builder providers(ProviderResource... providers) {
            return this.providers(List.of(providers));
        }

        /**
         * @param providers optional list of providers to use for child resources
         * @return the {@link Builder}
         */
        public Builder providers(@Nullable List<ProviderResource> providers) {
            options.providers = providers;
            return this;
        }

        public ComponentResourceOptions build() {
            return this.options;
        }
    }

    /**
     * An optional list of providers to use for child resources.
     *
     * @return list of providers or empty
     */
    public List<ProviderResource> getProviders() {
        return providers == null ? List.of() : List.copyOf(providers);
    }

    public ComponentResourceOptions copy() {
        return new ComponentResourceOptions(
                this.id,
                this.parent,
                this.getDependsOn().copy(),
                this.protect,
                copyNullableList(this.ignoreChanges),
                this.version,
                copyNullable(this.customTimeouts),
                copyNullableList(this.resourceTransformations),
                copyNullableList(this.aliases),
                this.urn,
                copyNullableList(this.replaceOnChanges),
                this.retainOnDelete,
                this.pluginDownloadURL,
                copyNullableList(this.hideDiffs),
                copyNullableList(this.providers), // TODO: should we also invoke copy() on the items?
                copyNullableList(this.replaceWith),
                this.replacementTrigger
        );
    }

    /**
     * Takes two "ComponentResourceOptions" values and produces a new "ComponentResourceOptions"
     * with the respective properties of "options2" merged over the same properties in "options1".
     * <p>
     * The original options objects will be unchanged.
     * A new instance will always be returned.
     * <p>
     * Conceptually property merging follows these basic rules:
     * 1. If the property is a collection, the final value will be a collection containing the
     * values from each options object.
     * 2. Simple scalar values from "options2" (i.e. Strings, Integers, Booleans)
     * will replace the values of "options1".
     * 3. "null" values in "options2" will be ignored.
     */
    public static ComponentResourceOptions merge(
            @Nullable ComponentResourceOptions options1,
            @Nullable ComponentResourceOptions options2
    ) {
        return merge(options1, options2, null);
    }

    public static ComponentResourceOptions merge(
            @Nullable ComponentResourceOptions options1,
            @Nullable ComponentResourceOptions options2,
            @Nullable Output<String> id
    ) {
        options1 = options1 != null ? options1.copy() : Empty;
        options2 = options2 != null ? options2.copy() : Empty;

        if (options1.provider != null) {
            throw new IllegalStateException("unexpected non-null 'provider', should use only 'providers'");
        }
        if (options2.provider != null) {
            throw new IllegalStateException("unexpected non-null 'provider', should use only 'providers'");
        }

        // first, merge all the normal option values over
        options1 = mergeSharedOptions(options1, options2, id);

        options1.providers = mergeNullableList(options1.providers, options2.providers);

        return options1;
    }
}
