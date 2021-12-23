package io.pulumi.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a value of one of two possible types (a disjoint union).
 * Instances of {@code Either} are either an instance of {@code Left} or {@code Right}.
 * <p>
 * Either is an algebraic data type similar to the an Option/Optional.
 * <p>
 * A common use of {@code Either} is as an alternative to {@code Optional} for dealing with possible missing values.
 * In this usage, {@code Absent} is replaced with a {@code Left} which can contain useful information.
 * {@code Right} takes the place of {@code Present}.
 * Convention dictates that {@code Left} is used for <b>failure</b> and {@code Right} is used for <b>success</b>.
 * <p>
 * A non-null {@code Either<L,R>} reference can be used as an alternative to the classic error handling (exceptions).
 * <p>
 * <pre>
 *  public static Either<Exception, Integer> divide(int x, int y) {
 *    try {
 *      return Either.valueOf(x / y);
 *    } catch (Exception e) {
 *      return Either.errorOf(e);
 *    }
 *  }
 * </pre>
 *
 * @param <L> the type of left instance that can be contained.
 * @param <R> the type of right instance that can be contained.
 */
@CheckReturnValue
public abstract class Either<L, R> implements Serializable {
    private static final long serialVersionUID = 0;

    Either() {
        /* Empty */
    }

    /**
     * Same as {@link #ofRight}, a convenience method for Value/Error use case.
     */
    public static <L, R> Either<L, R> valueOf(R reference) {
        return ofRight(reference);
    }

    /**
     * Same as {@link #ofLeft}, a convenience method for Value/Error use case.
     */
    public static <L, R> Either<L, R> errorOf(L reference) {
        return ofLeft(reference);
    }

    /**
     * Returns an {@code Either} instance containing the given non-null reference.
     *
     * @throws NullPointerException if {@code reference} is null
     */
    public static <L, R> Either<L, R> ofLeft(L reference) {
        Objects.requireNonNull(reference, "Expected non-null reference");
        return new Left<>(reference);
    }

    /**
     * Returns an {@code Either} instance containing the given non-null reference.
     *
     * @throws NullPointerException if {@code reference} is null
     */
    public static <L, R> Either<L, R> ofRight(R reference) {
        Objects.requireNonNull(reference, "Expected non-null reference");
        return new Right<>(reference);
    }

    /**
     * Same as {@link #isRight()}, a convenience method for Value/Error use case.
     */
    public boolean isValue() {
        return isRight();
    }

    /**
     * Same as {@link #isLeft}, a convenience method for Value/Error use case.
     */
    public boolean isError() {
        return isLeft();
    }

    /**
     * @return true if this is a Left, false otherwise.
     */
    public abstract boolean isLeft();

    /**
     * @return true if this is a Right, false otherwise.
     */
    public abstract boolean isRight();

    /**
     * Same as {@link #right()}, a convenience method for Value/Error use case.
     */
    public R value() {
        return right();
    }

    /**
     * Same as {@link #left()}, a convenience method for Value/Error use case.
     */
    public L error() {
        return left();
    }

    /**
     * Returns the contained instance, which must be present.
     *
     * @throws IllegalStateException if the instance is absent ({@link #isLeft()} returns
     *                               {@code false}); depending on this <i>specific</i> exception type (over the more general
     *                               {@link RuntimeException}) is discouraged
     */
    public abstract L left();

    /**
     * Returns the contained instance, which must be present.
     *
     * @throws IllegalStateException if the instance is absent ({@link #isRight()} returns
     *                               {@code false}); depending on this <i>specific</i> exception type (over the more general
     *                               {@link RuntimeException}) is discouraged
     */
    public abstract R right();


    /**
     * Returns this {@code Either} if it has the right value present; {@code secondChoice} otherwise.
     */
    public abstract Either<L, R> or(Either<? extends L, ? extends R> secondChoice);

    /**
     * Returns this {@code Either} value if it has the right value present; {@code defaultValue} otherwise.
     */
    public abstract R or(R defaultValue);

    /**
     * Returns the right instance if it is present; {@code throw leftFunction.apply(left())} otherwise.
     *
     * @param leftFunction the left mapping function
     * @param <E>          type of thrown exception mapped from right
     * @throws E                    if left is present, and is mapped with leftFunction
     * @throws NullPointerException if right value is absent or the given function is {@code null}
     */
    public abstract <E extends Exception> R orThrow(Function<L, E> leftFunction) throws E;

    /**
     * Returns the right instance, if present, mapped with the given rightFunction; {@code throw leftFunction.apply(left())} otherwise.
     *
     * @param rightFunction the right mapping function
     * @param leftFunction  the left mapping function
     * @param <T>           type of the returned value mapped from right
     * @param <E>           type of thrown exception mapped from right
     * @return T if right is present, and is mapped with mapper
     * @throws E                    if left is present, and is mapped with leftFunction
     * @throws NullPointerException if right value is absent or a given function is {@code null}
     */
    public abstract <T, E extends Exception> T mapOrThrow(Function<L, E> leftFunction, Function<R, T> rightFunction) throws E;

    /**
     * Applies {@code leftFunction} if this is a Left or {@code rightFunction} if this is a Right.
     *
     * @return the result of the {@code leftFunction} or {@code rightFunction}.
     * @throws NullPointerException if any of the functions are {@code null}
     */
    public abstract <V> V either(Function<L, V> leftFunction, Function<R, V> rightFunction);

    /**
     * Applies {@code leftFunction} if this is a Left or {@code rightFunction} if this is a Right.
     *
     * @return an Either after transformation by {@code leftFunction} or {@code rightFunction}.
     * @throws NullPointerException if any of the functions are {@code null}
     */
    public abstract <A, B> Either<A, B> transform(Function<L, A> leftFunction, Function<R, B> rightFunction);

    /**
     * Applies {@code function} if this is a Right or returns Left.
     *
     * @return an Either after conditional transformation by {@code function}.
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public abstract <R1> Either<L, R1> flatMap(Function<? super R, ? extends Either<? extends L, ? extends R1>> mapper);

    /**
     * If this is a Left, then return the left value in Right or vice versa.
     *
     * @return a new {@code Either<R,L>}
     */
    public abstract Either<R, L> swap();

    /**
     * Returns {@code true} if {@code object} is an {@code Either} instance, and either
     * the contained references are {@linkplain Object#equals equal} to each other.
     * Note that {@code Either} instances of differing parameterized types can
     * be equal.
     */
    @Override
    public abstract boolean equals(@Nullable Object object);

    /**
     * Returns a hash code for this instance.
     */
    @Override
    public abstract int hashCode();

    /**
     * Returns a string representation for this instance.
     */
    @Override
    public abstract String toString();
}
