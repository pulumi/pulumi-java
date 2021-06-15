package io.pulumi.resources;

import io.pulumi.core.Input;

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
        if (Input.class.isAssignableFrom(memberType)) {
            throw new UnsupportedOperationException(
                    String.format("'%s' must not be an Input<T>", fullName));
        }
    }
}