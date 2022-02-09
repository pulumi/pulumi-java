package io.pulumi.resources;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base type for all invoke argument classes.
 */
@ParametersAreNonnullByDefault
public abstract class InvokeArgs extends InputArgs {

    public static final InvokeArgs Empty = new InvokeArgs() {
        // Empty
    };

    @Override
    protected void validateMember(Class<?> memberType, String fullName) {
        // No validation. A member may or may not be Input.
    }
}