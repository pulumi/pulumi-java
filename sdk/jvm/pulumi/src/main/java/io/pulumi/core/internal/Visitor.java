package io.pulumi.core.internal;

import io.pulumi.core.internal.annotations.InternalUse;

@InternalUse
public interface Visitor<T, V> {
    T visit(V value);
}
