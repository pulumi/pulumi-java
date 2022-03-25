package io.pulumi.core.internal;

import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.Nullable;

/**
 * Helper functions that may be referenced by generated code but should not be used otherwise.
 */
@InternalUse
public final class Codegen {
    public static <T> Output<T> secret(@Nullable T value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T, O extends Output<T>> Output<T> secret(@Nullable O value) {
        return Codegen.ofNullable(value).asSecret();
    }

    public static <T> Output<T> empty() {
        return new OutputInternal<>(OutputData.empty());
    }

    public static <T> Output<T> ofNullable(@Nullable T value) {
        return value == null ? Codegen.empty() : Output.of(value);
    }

    public static <T, O extends Output<T>> Output<T> ofNullable(@Nullable O value) {
        return value == null ? Codegen.empty() : value;
    }
}
