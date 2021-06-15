package io.pulumi.resources;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @see StackOptions is a bag of optional settings that control a stack's behavior.
 */
public class StackOptions {
    @Nullable
    private final List<ResourceTransformation> resourceTransformations;

    public StackOptions(@Nullable List<ResourceTransformation> resourceTransformations) {
        this.resourceTransformations = resourceTransformations;
    }

    /**
     * Optional list of transformations to apply to this stack's resources during construction.
     * The transformations are applied in order, and are applied after all the transformations of custom
     * and component resources in the stack.
     */
    public List<ResourceTransformation> getResourceTransformations() {
        return resourceTransformations == null ? List.of() : resourceTransformations;
    }
}
