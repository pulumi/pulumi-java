package com.pulumi.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a value of one of two possible types (a disjoint union).
 * Instances of {@code Either} are either an instance of {@link Left} or {@link Right}.
 * <p>
 * Either is an algebraic data type similar to the an Option/Optional.
 * </p>
 * A common use of {@code Either} is as an alternative to {@code Optional} for dealing with possible missing values.
 * In this usage, {@code Absent} is replaced with a {@code Left} which can contain useful information.
 * {@code Right} takes the place of {@code Present}.
 * Convention dictates that {@code Left} is used for <b>failure</b> and {@code Right} is used for <b>success</b>.
 * <p>
 * A non-null {@code Either<L,R>} reference can be used as an alternative to the classic error handling (exceptions).
 * </p>
 * <pre>
 *  {@literal public static Either<Exception, Integer> divide(int x, int y)} {
 *    try {
 *      return Either.valueOf(x / y);
 *    } catch (Exception e) {
 *      return Either.errorOf(e);
 *    }
 *  }
 * </pre>
 *
 * @param <L> the type contained by the {@link Left} instance
 * @param <R> the type contained by the {@link Right} instance
 */
@CheckReturnValue
public abstract class Either<L, R> implements Serializable {
    private static final long serialVersionUID = 0;

    Either() {
        /* Empty */
    }

    /**
     * Same as {@link #ofRight}, a convenience method for Value/Error use case.
     *
     * @param <L>       the type contained by the {@link Left} instance
     * @param <R>       the type contained by the {@link Right} instance
     * @param reference the value to create the {@link Right} instance with
     * @return new {@link Right} instance with the given value
     */
    public static <L, R> Either<L, R> valueOf(R reference) {
        return ofRight(reference);
    }

    /**
     * Same as {@link #ofLeft}, a convenience method for Value/Error use case.
     *
     * @param <L>       the type contained by the {@link Left} instance
     * @param <R>       the type contained by the {@link Right} instance
     * @param reference the value to create the {@link Left} instance with
     * @return new {@link Left} instance with the given value
     */
    public static <L, R> Either<L, R> errorOf(L reference) {
        return ofLeft(reference);
    }

    /**
     * Returns an {@code Either} instance containing the given non-null reference.
     *
     * @param <L>       the type contained by the {@link Left} instance
     * @param <R>       the type contained by the {@link Right} instance
     * @param reference the value to create the {@link Left} instance with
     * @return new {@link Left} instance with the given value
     * @throws NullPointerException if {@code reference} is null
     */
    public static <L, R> Either<L, R> ofLeft(L reference) {
        Objects.requireNonNull(reference, "Expected non-null reference");
        return new Left<>(reference);
    }

    /**
     * Returns an {@code Either} instance containing the given non-null reference.
     *
     * @param <L>       the type contained by the {@link Left} instance
     * @param <R>       the type contained by the {@link Right} instance
     * @param reference the value to create the {@link Right} instance with
     * @return new {@link Right} instance with the given value
     * @throws NullPointerException if {@code reference} is null
     */
    public static <L, R> Either<L, R> ofRight(R reference) {
        Objects.requireNonNull(reference, "Expected non-null reference");
        return new Right<>(reference);
    }

    /**
     * Same as {@link #isRight()}, a convenience method for Value/Error use case.
     *
     * @return true if this is a {@link Right}, false otherwise.
     */
    public boolean isValue() {
        return isRight();
    }

    /**
     * Same as {@link #isLeft}, a convenience method for Value/Error use case.
     *
     * @return true if this is a {@link Left}, false otherwise.
     */
    public boolean isError() {
        return isLeft();
    }

    /**
     * @return true if this is a {@link Left}, false otherwise.
     */
    public abstract boolean isLeft();

    /**
     * @return true if this is a {@link Right}, false otherwise.
     */
    public abstract boolean isRight();

    /**
     * Same as {@link #right()}, a convenience method for Value/Error use case.
     *
     * @return the {@link Right} instance if present or throw
     */
    public R value() {
        return right();
    }

    /**
     * Same as {@link #left()}, a convenience method for Value/Error use case.
     *
     * @return the {@link Left} instance if present or throw
     */
    public L error() {
        return left();
    }

    /**
     * Returns the contained instance, which must be present. Otherwise, throw.
     *
     * @return the {@link Left} instance if present or throw
     * @throws IllegalStateException if the instance is absent ({@link #isLeft()} returns
     *                               {@code false}); depending on this <i>specific</i> exception type
     *                               (over the more general {@link RuntimeException}) is discouraged
     */
    public abstract L left();

    /**
     * Returns the contained instance, which must be present. Otherwise, throw.
     *
     * @return the {@link Right} instance if present or throw
     * @throws IllegalStateException if the instance is absent ({@link #isRight()} returns
     *                               {@code false}); depending on this <i>specific</i> exception type
     *                               (over the more general {@link RuntimeException}) is discouraged
     */
    public abstract R right();

    /**
     * Returns {@code this} {@link Either} if it has the {@link Right} value present; {@code secondChoice} otherwise.
     *
     * @param secondChoice the alternative {@link Either}
     * @return {@code this} instance if {@link Right} is present or {@code secondChoice} otherwise.
     */
    public abstract Either<L, R> or(Either<? extends L, ? extends R> secondChoice);

    /**
     * Returns this {@code Either} value if it has the {@link Right} value present; {@code defaultValue} otherwise.
     *
     * @param defaultValue the alternative value
     * @return {@code this} value if {@link Right} is present or {@code defaultValue} otherwise.
     */
    public abstract R or(R defaultValue);

    /**
     * Returns the {@link Right} value if it is present; {@code throw leftFunction.apply(left())} otherwise.
     *
     * @param leftFunction the {@link Left} mapping function from left to an exception
     * @param <E>          type of thrown exception mapped from left
     * @return the {@link Right} value or throw the result of {@code leftFunction}
     * @throws E                    type of thrown exception mapped from {@link Left}
     * @throws NullPointerException if right value is absent or the given function is {@code null}
     */
    public abstract <E extends Exception> R orThrow(Function<L, E> leftFunction) throws E;

    /**
     * Returns the {@link Right} instance, if present, mapped with the given {@code rightFunction};
     * {@code throw leftFunction.apply(left())} otherwise.
     *
     * @param leftFunction  the {@link Left} mapping function
     * @param rightFunction the {@link Right} mapping function
     * @param <T>           type of the returned value mapped from {@link Right}
     * @param <E>           type of thrown exception mapped from {@link Left}
     * @return T mapped with {@code rightFunction} if {@link Right} is present, or throw
     * @throws E                    mapped with leftFunction if {@link Left} is present
     * @throws NullPointerException if {@link Right} value is absent or a given function is {@code null}
     */
    public abstract <T, E extends Exception> T mapOrThrow(Function<L, E> leftFunction, Function<R, T> rightFunction) throws E;

    /**
     * Applies {@code leftFunction} if this is a {@link Left} or {@code rightFunction} if this is a {@link Right}.
     *
     * @param <V>           type of the value returned by either {@code leftFunction} or {@code rightFunction}
     * @param leftFunction  {@link Left} value mapper
     * @param rightFunction {@link Right} value mapper
     * @return the result of the {@code leftFunction} or {@code rightFunction}
     * @throws NullPointerException if any of the functions are {@code null}
     */
    public abstract <V> V either(Function<L, V> leftFunction, Function<R, V> rightFunction);

    /**
     * Applies {@code leftFunction} if this is a {@link Left} or {@code rightFunction} if this is a {@link Right}.
     *
     * @param <A>           the {@link Left} type after transformation
     * @param <B>           the {@link Right} type after transformation
     * @param leftFunction  {@link Left} value mapper
     * @param rightFunction {@link Right} value mapper
     * @return an Either after transformation by {@code leftFunction} or {@code rightFunction}.
     * @throws NullPointerException if any of the functions are {@code null}
     */
    public abstract <A, B> Either<A, B> transform(Function<L, A> leftFunction, Function<R, B> rightFunction);

    /**
     * Applies {@code function} if this is a {@link Right} or returns {@link Left}.
     *
     * @param <R1> the type contained by the mapped {@link Right} instance
     * @param mapper {@link Right} value mapper, with {@link Either} as result
     * @return an Either after conditional transformation by {@code function}.
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public abstract <R1> Either<L, R1> flatMap(Function<? super R, ? extends Either<? extends L, ? extends R1>> mapper);

    /**
     * Applies {@code function} if this is a {@link Right} or returns {@link Left}.
     *
     * @param <R1> the type contained by the mapped {@link Right} instance
     * @param mapper {@link Right} value mapper, with {@code R1} as result
     * @return an Either after conditional transformation by {@code function}.
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public <R1> Either<L, R1> map(Function<? super R, ? extends R1> mapper) {
        return flatMap(r -> Either.ofRight(mapper.apply(r)));
    }

    /**
     * If this is a {@link Left}, then return the left value in {@link Right} or vice versa.
     *
     * @return a new {@code Either<R,L>}
     */
    public abstract Either<R, L> swap();

    /**
     * Returns {@code true} if {@code object} is an {@code Either} instance, and either
     * the contained references are {@linkplain Object#equals equal} to each other.
     * Note that {@code Either} instances of differing parameterized types can be equal.
     *
     * @param object the object to compare with
     * @return {@code true} if given {@code object} is equal to {@code this}
     */
    @Override
    public abstract boolean equals(@Nullable Object object);

    /**
     * @return a hash code for {@code this} instance.
     */
    @Override
    public abstract int hashCode();

    /**
     * @return a string representation for {@code this} instance.
     */
    @Override
    public abstract String toString();
}
