package io.pulumi.resources;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base type for all resource argument classes.
 */
@ParametersAreNonnullByDefault
public abstract class ResourceArgs extends InputArgs {
    public static final ResourceArgs Empty = new ResourceArgs() {
        // Empty
    };

    @Override
    protected void validateMember(Class<?> memberType, String fullName) {
        // No validation. A member may or may not be Input.
    }
}
