package io.pulumi.core.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
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

    @CanIgnoreReturnValue
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

    @Nullable
    @CanIgnoreReturnValue
    public static <T> T require(
            Predicate<T> predicate,
            @Nullable T obj,
            @Nullable Supplier</* @Nullable */ String> messageSupplier
    ) throws IllegalArgumentException {
        return require(predicate, obj, messageSupplier, null);
    }

    @Nullable
    @CanIgnoreReturnValue
    public static <T> T require(
            Predicate<T> predicate,
            @Nullable T obj
    ) throws IllegalArgumentException {
        return require(predicate, obj, null, null);
    }

    @Nullable
    @CanIgnoreReturnValue
    public static <T> T requireNullState(
            @Nullable T obj,
            @Nullable Supplier<String> messageSupplier
    ) throws IllegalStateException {
        return require(
                java.util.Objects::isNull,
                obj,
                messageSupplier,
                exceptionSupplier(IllegalStateException::new, IllegalStateException::new)
        );
    }

    @CanIgnoreReturnValue
    public static boolean requireFalseState(
            boolean obj,
            @Nullable Supplier<String> messageSupplier
    ) throws IllegalStateException {
        return require(
                o -> !o,
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
