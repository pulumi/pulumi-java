package io.pulumi.resources;

import io.pulumi.core.Alias;
import io.pulumi.core.Input;
import io.pulumi.core.InputList;
import io.pulumi.core.internal.Copyable;
import io.pulumi.core.internal.Objects;

import javax.annotation.Nullable;
import java.util.List;

import static io.pulumi.resources.Resources.*;

/**
 * A bag of optional settings that control a @see {@link ComponentResource} behavior.
 */
public final class ComponentResourceOptions extends ResourceOptions implements Copyable<ComponentResourceOptions> {

    public static final ComponentResourceOptions Empty = new ComponentResourceOptions();

    @Nullable
    private List<ProviderResource> providers;

    protected ComponentResourceOptions() { /* empty */ }

    public ComponentResourceOptions(
            @Nullable Input<String> id,
            @Nullable Resource parent,
            @Nullable InputList<Resource> dependsOn,
            boolean protect,
            @Nullable List<String> ignoreChanges,
            @Nullable String version,
            @Nullable CustomTimeouts customTimeouts,
            @Nullable List<ResourceTransformation> resourceTransformations,
            @Nullable List<Input<Alias>> aliases,
            @Nullable String urn,
            @Nullable List<ProviderResource> providers
    ) {
        super(id, parent, dependsOn, protect, ignoreChanges, version, null /* use providers instead */, customTimeouts,
                resourceTransformations, aliases, urn);
        this.providers = providers;
        Objects.requireNullState(this.provider, () -> "expected 'provider' to be null, use 'providers' instead");
    }

    public static Builder builder() {
        return new Builder(new ComponentResourceOptions());
    }

    public static final class Builder extends ResourceOptions.Builder<ComponentResourceOptions, Builder> {

        private final ComponentResourceOptions options;

        private Builder(ComponentResourceOptions options) {
            super(options);
            this.options = options;
        }

        public Builder setProvider(@Nullable List<ProviderResource> providers) {
            options.providers = providers;
            return this;
        }

        public ComponentResourceOptions build() {
            return this.options;
        }
    }

    /**
     * An optional set of providers to use for child resources.
     *
     * @return set of providers or empty
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
                copyNullableList(this.providers) // TODO: should we also invoke copy() on the items?
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
        options1 = options1 != null ? options1.copy() : Empty;
        options2 = options2 != null ? options2.copy() : Empty;

        if (options1.provider != null) {
            throw new IllegalStateException("unexpected non-null 'provider', should use only 'providers'");
        }
        if (options2.provider != null) {
            throw new IllegalStateException("unexpected non-null 'provider', should use only 'providers'");
        }

        // first, merge all the normal option values over
        options1 = mergeSharedOptions(options1, options2);

        options1.providers = mergeNullableList(options1.providers, options2.providers);

        return options1;
    }
}
