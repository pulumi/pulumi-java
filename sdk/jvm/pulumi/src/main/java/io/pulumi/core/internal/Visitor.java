package io.pulumi.core.internal;

import io.grpc.Internal;

@Internal
public interface Visitor<T, V> {
    T visit(V value);
}
