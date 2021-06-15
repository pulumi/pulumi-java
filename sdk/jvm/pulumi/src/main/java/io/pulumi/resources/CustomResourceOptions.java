package io.pulumi.resources;

import io.pulumi.core.Alias;
import io.pulumi.core.Input;
import io.pulumi.core.InputList;
import io.pulumi.core.internal.Copyable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static io.pulumi.resources.Resources.*;

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
            @Nullable Input<String> id,
            @Nullable Resource parent,
            @Nullable InputList<Resource> dependsOn,
            boolean protect,
            @Nullable List<String> ignoreChanges,
            @Nullable String version,
            @Nullable ProviderResource provider,
            @Nullable CustomTimeouts customTimeouts,
            @Nullable List<ResourceTransformation> resourceTransformations,
            @Nullable List<Input<Alias>> aliases,
            @Nullable String urn,
            boolean deleteBeforeReplace,
            @Nullable List<String> additionalSecretOutputs,
            @Nullable String importId
    ) {
        super(id, parent, dependsOn, protect, ignoreChanges, version, provider, customTimeouts,
                resourceTransformations, aliases, urn);
        this.deleteBeforeReplace = deleteBeforeReplace;
        this.additionalSecretOutputs = additionalSecretOutputs;
        this.importId = importId;
    }

    public static Builder builder() {
        return new Builder(new CustomResourceOptions());
    }

    public static final class Builder extends ResourceOptions.Builder<CustomResourceOptions, Builder> {

        private final CustomResourceOptions options;

        private Builder(CustomResourceOptions options) {
            super(options);
            this.options = options;
        }

        public Builder setDeleteBeforeReplace(boolean deleteBeforeReplace) {
            options.deleteBeforeReplace = deleteBeforeReplace;
            return this;
        }

        public Builder setAdditionalSecretOutputs(@Nullable List<String> additionalSecretOutputs) {
            options.additionalSecretOutputs = additionalSecretOutputs;
            return this;
        }

        public Builder setImportId(@Nullable String importId) {
            options.importId = importId;
            return this;
        }

        public CustomResourceOptions build() {
            return this.options;
        }
    }

    /**
     * When set to "true", indicates that this resource should be deleted before its
     * replacement is created when replacement is necessary.
     */
    public boolean getDeleteBeforeReplace() {
        return this.deleteBeforeReplace;
    }

    /**
     * The names of outputs for this resource that should be treated as secrets. This augments
     * the list that the resource provider and pulumi engine already determine based on inputs
     * to your resource. It can be used to mark certain outputs as a secrets on a per resource
     * basis.
     */
    public List<String> getAdditionalSecretOutputs() {
        return this.additionalSecretOutputs == null ? List.of() : List.copyOf(this.additionalSecretOutputs);
    }

    /**
     * When provided with a resource ID, import indicates that this resource's provider should
     * import its state from the cloud resource with the given ID.The inputs to the resource's
     * constructor must align with the resource's current state.Once a resource has been
     * imported, the import property must be removed from the resource's options.
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
                this.importId
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
        options1 = options1 != null ? options1.copy() : Empty;
        options2 = options2 != null ? options2.copy() : Empty;

        // first, merge all the normal option values over
        options1 = mergeSharedOptions(options1, options2);

        options1.deleteBeforeReplace = options1.deleteBeforeReplace || options2.deleteBeforeReplace;
        options1.importId = options2.importId == null ? options1.importId : options2.importId;

        options1.additionalSecretOutputs = mergeNullableList(options1.additionalSecretOutputs, options2.additionalSecretOutputs);
        return options1;
    }
}
