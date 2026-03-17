package com.pulumi.resources;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.pulumi.resources.Resources.mergeNullableList;
import static java.util.Objects.requireNonNull;

/**
 * {@link StackOptions} is a bag of optional settings that control a stack's behavior.
 */
@ParametersAreNonnullByDefault
public class StackOptions {

    public static final StackOptions Empty = builder().build();

    private final List<ResourceTransformation> resourceTransformations;

    /**
     * Creates a {@link StackOptions} instance.
     *
     * @param resourceTransformations the resource transformations to use
     * @see #builder()
     */
    public StackOptions(List<ResourceTransformation> resourceTransformations) {
        this.resourceTransformations = requireNonNull(resourceTransformations);
    }

    /**
     * Optional list of transformations to apply to this stack's resources during construction.
     * The transformations are applied in order, and are applied after all the transformations of custom
     * and component resources in the stack.
     *
     * @return list of transformations to apply to children resources
     */
    public List<ResourceTransformation> resourceTransformations() {
        return this.resourceTransformations;
    }

    /**
     * @return a {@link StackOptions} builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The {@link StackOptions} builder.
     */
    public static final class Builder {

        private List<ResourceTransformation> resourceTransformations = List.of();

        /**
         * @param resourceTransformations transformations to apply to children resources
         * @return this {@link Builder}
         * @see StackOptions#resourceTransformations()
         */
        public Builder resourceTransformations(ResourceTransformation... resourceTransformations) {
            this.resourceTransformations = List.of(resourceTransformations);
            return this;
        }

        /**
         * @param resourceTransformations list of transformations to apply to children resources
         * @return this {@link Builder}
         * @see StackOptions#resourceTransformations()
         */
        public Builder resourceTransformations(List<ResourceTransformation> resourceTransformations) {
            this.resourceTransformations = requireNonNull(resourceTransformations);
            return this;
        }

        /**
         * @return a new {@link StackOptions} from this {@link Builder}
         */
        public StackOptions build() {
            return new StackOptions(resourceTransformations);
        }
    }

    /**
     * Takes two {@link StackOptions} values and produces a new {@link StackOptions}
     * with the respective properties of "options2" merged over the same properties in "options1".
     * <p>
     * The original options objects will be unchanged. A new instance will always be returned.
     * <p>
     *
     * @param options1 first options to merge
     * @param options2 second options to merge
     * @return a new {@link StackOptions} with merged values
     */
    public static StackOptions merge(@Nullable StackOptions options1, @Nullable StackOptions options2) {
        var opt1 = options1 != null ? options1 : Empty;
        var opt2 = options2 != null ? options2 : Empty;

        var resourceTransformations = requireNonNull(mergeNullableList(
                opt1.resourceTransformations, opt2.resourceTransformations
        ));
        return new StackOptions(resourceTransformations);
    }
}
