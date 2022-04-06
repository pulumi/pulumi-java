package io.pulumi.context;

import io.pulumi.core.Output;

/**
 * Allows for {@link Output} creation in current context.
 */
public interface OutputContext {

    /**
     * Creates a new {@link Output} with a non-null value.
     *
     * @param value the value of the new {@code Output}
     * @param <T>   type of the new {@code Output}
     * @return a new {@code Output} instance
     * @throws NullPointerException if provided {@code value} is {@code null}
     */
    <T> Output<T> output(T value);
}
