package io.pulumi.exceptions;

import javax.annotation.Nullable;

/**
 * RunException can be used for terminating a program abruptly, but resulting in a clean exit
 * rather than the usual verbose unhandled error logic which emits the source program text
 * and complete stack trace.
 * <br/><b>This type should be rarely used.</b>
 * <p/>
 * Ideally @see {@link ResourceException} should always be used so that
 * as many errors as possible can be associated with a @see {@link io.pulumi.resources.Resource}.
 */
public class RunException extends RuntimeException {
    public RunException(String message) {
        super(message);
    }

    public RunException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
