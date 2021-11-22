package io.pulumi.core;

import io.pulumi.core.internal.Copyable;

public interface InputOutput<T, IO extends InputOutput<T, IO> & Copyable<IO>> extends Copyable<IO> {

    /**
     * Creates a shallow copy (the underlying CompletableFuture is copied) of this @see {@link io.pulumi.core.Output<T>}
     * or @see {@link io.pulumi.core.Input<T>}
     *
     * @return a shallow copy of the @see {@link io.pulumi.core.Output<T>} or @see {@link io.pulumi.core.Input<T>}
     */
    IO copy();

    /**
     * Returns a new @see {@link Output<T>} or @see {@link Input<T>} which is a copy of the existing output but marked as
     * a non-secret. The original output or input is not modified in any way.
     */
    IO asPlaintext();

    /**
     * Returns a new @see {@link Output<T>} or @see {@link Input<T>} which is a copy of the existing output but marked as
     * a secret. The original output or input is not modified in any way.
     */
    IO asSecret();
}
