package io.pulumi.resources;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base type for all call argument classes.
 */
@ParametersAreNonnullByDefault
public class CallArgs extends InputArgs {

    public static final CallArgs Empty = new CallArgs() {
        // Empty
    };

    @Override
    protected void validateMember(Class<?> memberType, String fullName) {
        // No validation. A member may or may not be Input.
    }
}
