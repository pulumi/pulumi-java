package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;

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
        return Output.ofNullable((T)null);
    }

    public static <T> Output<T> ofNullable(@Nullable T value) {
        return value == null ? Codegen.empty() : Output.ofNullable(value);
    }

    public static <T, O extends Output<T>> Output<T> ofNullable(@Nullable O value) {
        return value == null ? Codegen.empty() : value;
    }
}
