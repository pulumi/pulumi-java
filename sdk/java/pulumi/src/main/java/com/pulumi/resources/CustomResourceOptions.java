package com.pulumi.resources;

import com.pulumi.core.Alias;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Copyable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.pulumi.resources.Resources.copyNullable;
import static com.pulumi.resources.Resources.copyNullableList;
import static com.pulumi.resources.Resources.mergeNullableList;

/**
 * A bag of optional settings that control a @see {@link ComponentResource} behavior.
 */
public final class CustomResourceOptions extends ResourceOptions implements Copyable<CustomResourceOptions> {

    public static final CustomResourceOptions Empty = CustomResourceOptions.builder().build();

    private boolean deleteBeforeReplace;
    @Nullable
    private List<String> additionalSecretOutputs;
    @Nullable
    private String importId;

    private CustomResourceOptions() { /* empty */ }

    private CustomResourceOptions(
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
            boolean deleteBeforeReplace,
            @Nullable List<String> additionalSecretOutputs,
            @Nullable String importId,
            @Nullable List<String> replaceOnChanges,
            boolean retainOnDelete,
            @Nullable String pluginDownloadURL,
            @Nullable List<String> hideDiffs
    ) {
        super(id, parent, dependsOn, protect, ignoreChanges, version, provider, customTimeouts,
              resourceTransformations, aliases, urn, replaceOnChanges, retainOnDelete, pluginDownloadURL, hideDiffs);
        this.deleteBeforeReplace = deleteBeforeReplace;
        this.additionalSecretOutputs = additionalSecretOutputs;
        this.importId = importId;
    }

    public static Builder builder() {
        return new Builder(new CustomResourceOptions());
    }

    public static final class Builder extends ResourceOptions.Builder<CustomResourceOptions, Builder> {

        private Builder(CustomResourceOptions options) {
            super(options);
        }

        /**
         * When set to "true", indicates that this resource should be deleted before its
         * replacement is created when replacement is necessary.
         */
        public Builder deleteBeforeReplace(boolean deleteBeforeReplace) {
            options.deleteBeforeReplace = deleteBeforeReplace;
            return this;
        }

        /**
         * The names of outputs for this resource that should be treated as secrets. This augments
         * the list that the resource provider and pulumi engine already determine based on inputs
         * to your resource. It can be used to mark certain outputs as a secrets on a per-resource
         * basis.
         *
         */
        public Builder additionalSecretOutputs(String... additionalSecretOutputs) {
            return this.additionalSecretOutputs(List.of(additionalSecretOutputs));
        }

        /**
         * @see #additionalSecretOutputs(String...)
         */
        public Builder additionalSecretOutputs(@Nullable List<String> additionalSecretOutputs) {
            options.additionalSecretOutputs = additionalSecretOutputs;
            return this;
        }

        /**
         * When provided with a resource ID, import indicates that this resource's provider should
         * import its state from the cloud resource with the given ID.The inputs to the resource's
         * constructor must align with the resource's current state.Once a resource has been
         * imported, the import property must be removed from the resource's options.
         */
        public Builder importId(@Nullable String importId) {
            options.importId = importId;
            return this;
        }

        public CustomResourceOptions build() {
            return this.options;
        }
    }

    /**
     * @see Builder#deleteBeforeReplace(boolean)
     */
    public boolean getDeleteBeforeReplace() {
        return this.deleteBeforeReplace;
    }

    /**
     * @see Builder#additionalSecretOutputs(String...)
     */
    public List<String> getAdditionalSecretOutputs() {
        return this.additionalSecretOutputs == null ? List.of() : List.copyOf(this.additionalSecretOutputs);
    }

    /**
     * @see Builder#importId(String)
     */
    public Optional<String> getImportId() {
        return Optional.ofNullable(importId);
    }

    public CustomResourceOptions copy() {
        return new CustomResourceOptions(
                this.id,
                this.parent,
                this.getDependsOn().copy(),
                this.protect,
                copyNullableList(this.ignoreChanges),
                this.version,
                this.provider,
                copyNullable(this.customTimeouts),
                copyNullableList(this.resourceTransformations),
                copyNullableList(this.aliases),
                this.urn,
                this.deleteBeforeReplace,
                copyNullableList(this.additionalSecretOutputs),
                this.importId,
                copyNullableList(this.replaceOnChanges),
                this.retainOnDelete,
                this.pluginDownloadURL,
                this.hideDiffs
        );
    }

    /**
     * Takes two @see {@link CustomResourceOptions} values and produces a new @see {@link CustomResourceOptions}
     * with the respective properties of "options2" merged over the same properties in "options1".
     * <p>
     * The original options objects will be unchanged.
     * A new instance will always be returned.
     * <p>
     * Conceptually property merging follows these basic rules:
     * - If the property is a collection, the final value will be a collection containing the
     * values from each options object.
     * - Simple scalar values from "options2" (i.e. "string", "int", "bool") will replace the values of "options1".
     * - "null" values in "options2" will be ignored.
     */
    public static CustomResourceOptions merge(
            @Nullable CustomResourceOptions options1,
            @Nullable CustomResourceOptions options2
    ) {
        return merge(options1, options2, null);
    }

    public static CustomResourceOptions merge(
            @Nullable CustomResourceOptions options1,
            @Nullable CustomResourceOptions options2,
            @Nullable Output<String> id
    ) {
        options1 = options1 != null ? options1.copy() : Empty;
        options2 = options2 != null ? options2.copy() : Empty;

        // first, merge all the normal option values over
        //noinspection ConstantConditions
        options1 = mergeSharedOptions(options1, options2, id);

        options1.deleteBeforeReplace = options1.deleteBeforeReplace || options2.deleteBeforeReplace;
        options1.importId = options2.importId == null ? options1.importId : options2.importId;

        options1.additionalSecretOutputs = mergeNullableList(options1.additionalSecretOutputs, options2.additionalSecretOutputs);
        return options1;
    }
}
