package io.pulumi.core.internal;

import io.grpc.Internal;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Internal
public class Objects {

    private Objects() {
        throw new UnsupportedOperationException("static class");
    }

    @Nullable
    public static <T, E extends RuntimeException> T require(
            Predicate<T> predicate,
            @Nullable T obj,
            @Nullable Supplier</* @Nullable */ String> messageSupplier,
            @Nullable Function</* @Nullable */ String, E> exceptionSupplier
    ) throws E {
        java.util.Objects.requireNonNull(predicate);
        if (!predicate.test(obj)) {
            if (messageSupplier == null) {
                throw exceptionSupplier == null
                        ? new IllegalArgumentException()
                        : java.util.Objects.requireNonNull(exceptionSupplier.apply(null));
            } else {
                throw exceptionSupplier == null
                        ? new IllegalArgumentException(messageSupplier.get())
                        : java.util.Objects.requireNonNull(exceptionSupplier.apply(messageSupplier.get()));
            }
        }
        return obj;
    }

    public static <T> T require(
            Predicate<T> predicate,
            @Nullable T obj,
            @Nullable Supplier</* @Nullable */ String> messageSupplier
    ) throws IllegalArgumentException {
        return require(predicate, obj, messageSupplier, null);
    }

    public static <T> T require(
            Predicate<T> predicate,
            @Nullable T obj
    ) throws IllegalArgumentException {
        return require(predicate, obj, null, null);
    }

    @Nullable
    public static <T> T requireNullState(@Nullable T obj, @Nullable Supplier<String> messageSupplier) {
        return require(
                java.util.Objects::isNull,
                obj,
                messageSupplier,
                exceptionSupplier(IllegalStateException::new, IllegalStateException::new)
        );
    }

    public static <E extends RuntimeException> Function</* Nullable */ String, E> exceptionSupplier(
            Supplier<E> noMessageSupplier,
            Function<String, E> withMessageSupplier
    ) {
        java.util.Objects.requireNonNull(noMessageSupplier);
        java.util.Objects.requireNonNull(withMessageSupplier);

        return (@Nullable String message) ->
                message  == null
                    ? noMessageSupplier.get()
                    : withMessageSupplier.apply(message);
    }
}
