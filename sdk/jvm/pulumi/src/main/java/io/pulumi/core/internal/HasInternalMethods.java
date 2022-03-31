package io.pulumi.core.internal;

import java.util.Optional;

public interface HasInternalMethods {
    <I> Optional<I> tryGetInternalHandle(Class<I> classHandle);
}
