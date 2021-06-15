package io.pulumi.exceptions;

import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * ResourceException can be used for terminating a program abruptly, specifically associating the
 * problem with a Resource. Depending on the nature of the problem, clients can choose whether
 * or not a call stack should be returned as well. This should be very rare, and would only
 * indicate no usefulness of presenting that stack to the user.
 */
@SuppressWarnings("RedundantCast")
public class ResourceException extends RuntimeException {
    @Nullable
    private final Resource resource;
    private final boolean hideStack;

    public ResourceException() {
        this((Throwable) null, (Resource) null, false);
    }

    public ResourceException(boolean hideStack) {
        this((Throwable) null, (Resource) null, hideStack);
    }

    public ResourceException(@Nullable Resource resource) {
        this((Throwable) null, resource, false);
    }

    public ResourceException(@Nullable Resource resource, boolean hideStack) {
        this((Throwable) null, resource, hideStack);
    }

    public ResourceException(@Nullable Throwable cause) {
        this(cause, (Resource) null, false);
    }

    public ResourceException(@Nullable Throwable cause, @Nullable Resource resource) {
        this(cause, resource, false);
    }

    public ResourceException(@Nullable Throwable cause, @Nullable Resource resource, boolean hideStack) {
        super(cause);
        this.resource = resource;
        this.hideStack = hideStack;
    }

    public ResourceException(String message) {
        this(message, (Throwable) null, (Resource) null, false);
    }

    public ResourceException(String message, @Nullable Resource resource) {
        this(message, (Throwable) null, resource, false);
    }

    public ResourceException(String message, @Nullable Resource resource, boolean hideStack) {
        this(message, (Throwable) null, resource, hideStack);
    }

    public ResourceException(String message, @Nullable Throwable cause) {
        this(message, cause, (Resource) null, false);
    }

    public ResourceException(String message, @Nullable Throwable cause, @Nullable Resource resource) {
        this(message, cause, resource, false);
    }

    public ResourceException(String message, @Nullable Throwable cause, @Nullable Resource resource, boolean hideStack) {
        super(message, cause);
        this.resource = resource;
        this.hideStack = hideStack;
    }

    public Optional<Resource> getResource() {
        return Optional.ofNullable(this.resource);
    }

    public boolean isHideStack() {
        return hideStack;
    }
}