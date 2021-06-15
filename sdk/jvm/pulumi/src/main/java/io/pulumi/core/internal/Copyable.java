package io.pulumi.core.internal;

public interface Copyable<C extends Copyable<C>> {
    /**
     * @return a shallow copy of {@code C}
     */
    C copy();
}
