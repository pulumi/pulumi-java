package io.pulumi.core;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of an {@link Either} containing a right reference.
 */
final class Right<L, R> extends Either<L, R> {

    private static final long serialVersionUID = 0L;

    private final R right;

    Right(R right) {
        this.right = Objects.requireNonNull(right, "Expected non-null right");
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public boolean isRight() {
        return true;
    }

    @Override
    public L left() {
        throw new UnsupportedOperationException("Left value is absent");
    }

    @Override
    public R right() {
        return this.right;
    }

    @Override
    public Either<L, R> or(Either<? extends L, ? extends R> secondChoice) {
        Objects.requireNonNull(secondChoice, "Expected non-null secondChoice");
        return this;
    }

    @Override
    public R or(R defaultValue) {
        Objects.requireNonNull(defaultValue, "Expected non-null defaultValue");
        return this.right;
    }

    @Override
    public <E extends Exception> R orThrow(Function<L, E> leftFunction) throws E {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        return right;
    }

    @Override
    public <T, E extends Exception> T mapOrThrow(Function<L, E> leftFunction, Function<R, T> rightFunction) throws E {
        Objects.requireNonNull(leftFunction, "Expected non-null leftFunction");
        return rightFunction.apply(right);
    }

    @Override
    public <V> V either(Function<L, V> leftFunction, Function<R, V> rightFunction) {
        Objects.requireNonNull(rightFunction, "Expected non-null rightFunction");
        return rightFunction.apply(right());
    }

    @Override
    public <A, B> Either<A, B> transform(Function<L, A> leftFunction, Function<R, B> rightFunction) {
        Objects.requireNonNull(rightFunction, "Expected non-null rightFunction");
        return ofRight(rightFunction.apply(right()));
    }

    @Override
    public <R1> Either<L, R1> flatMap(Function<? super R, ? extends Either<? extends L, ? extends R1>> mapper) {
        Objects.requireNonNull(mapper, "Expected non-null mapper");
        //noinspection unchecked
        return (Either<L, R1>) mapper.apply(this.right);
    }

    @Override
    public Either<R, L> swap() {
        return ofLeft(right());
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Right<?, ?> right1 = (Right<?, ?>) other;
        return Objects.equals(right, right1.right);
    }

    @Override
    public int hashCode() {
        return 0xc4dcd0b4 + right.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Right.of(%s)", right);
    }
}
