package io.pulumi.core.internal;

import io.grpc.Internal;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Internal
public class Objects {

    private Objects() {
        throw new UnsupportedOperationException("static class");
    }

    @Nullable
    public static <T> T requireNull(@Nullable T obj, @Nullable Supplier<RuntimeException> exceptionSupplier) {
        if (obj != null) {
            throw exceptionSupplier == null ? new IllegalArgumentException() : exceptionSupplier.get();
        }
        return null;
    }

    @Nullable
    public static <T> T requireNullState(@Nullable T obj, @Nullable Supplier<String> messageSupplier) {
        return requireNull(obj,
                () -> messageSupplier == null
                        ? new IllegalStateException()
                        : new IllegalStateException(messageSupplier.get())
        );
    }
}
