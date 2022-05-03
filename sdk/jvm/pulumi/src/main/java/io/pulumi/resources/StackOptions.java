package io.pulumi.resources;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @see StackOptions is a bag of optional settings that control a stack's behavior.
 */
@ParametersAreNonnullByDefault
public class StackOptions {

    public static final StackOptions Empty = new StackOptions();

    private final List<ResourceTransformation> resourceTransformations;

    public StackOptions() {
        this.resourceTransformations = null;
    }

    public StackOptions(@Nullable List<ResourceTransformation> resourceTransformations) {
        this.resourceTransformations = resourceTransformations;
    }

    /**
     * Optional list of transformations to apply to this stack's resources during construction.
     * The transformations are applied in order, and are applied after all the transformations of custom
     * and component resources in the stack.
     */
    public List<ResourceTransformation> getResourceTransformations() {
        return this.resourceTransformations == null ? List.of() : this.resourceTransformations;
    }
}
