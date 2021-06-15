package io.pulumi.core;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of an {@link Either} containing a left reference.
 */
final class Left<L, R> extends Either<L, R> {

    private static final long serialVersionUID = 0L;

    private final L left;

    Left(L left) {
        Objects.requireNonNull(left, "Expected non-null left");
        this.left = left;
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public L left() {
        return this.left;
    }

    @Override
    public R right() {
        throw new UnsupportedOperationException("Right value is absent");
    }

    @Override
    public Either<L, R> or(Either<? extends L, ? extends R> secondChoice) {
        Objects.requireNonNull(secondChoice, "Expected non-null secondChoice");
        //noinspection unchecked
        return (Either<L, R>) secondChoice;
    }


    @Override
    public <E extends Exception> R orThrow(Function<L, E> leftFunction) throws E {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        E exception = leftFunction.apply(left());
        Objects.requireNonNull(exception);
        throw exception;
    }

    @Override
    public <T, E extends Exception> T mapOrThrow(Function<L, E> leftFunction, Function<R, T> rightFunction) throws E {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        Objects.requireNonNull(rightFunction, "Expected non-null rightFunction");
        E exception = leftFunction.apply(left());
        Objects.requireNonNull(exception);
        throw exception;
    }

    @Override
    public <V> V either(Function<L, V> leftFunction, Function<R, V> rightFunction) {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        return leftFunction.apply(left());
    }

    @Override
    public <A, B> Either<A, B> transform(Function<L, A> leftFunction, Function<R, B> rightFunction) {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        return leftOf(leftFunction.apply(left()));
    }

    @Override
    public Either<R, L> swap() {
        return rightOf(left());
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Left<?, ?> left1 = (Left<?, ?>) other;
        return Objects.equals(left, left1.left);
    }

    @Override
    public int hashCode() {
        return 0x43e36ed9 + left.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Left.of(%s)", left);
    }
}
