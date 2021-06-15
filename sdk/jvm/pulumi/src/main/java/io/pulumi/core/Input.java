package io.pulumi.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.InputOutputData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.pulumi.core.internal.InputOutputImpl.TupleZeroIn;

public interface Input<T> extends InputOutput<T, Input<T>> {

    /**
     * Convert @see {@link Input<T>} to @see {@link Output<T>}
     *
     * @return an {@link Output<T>} , converted from {@link Input<T>}
     */
    Output<T> toOutput();

    /**
     * Transforms the data of this @see {@link Input<T>} with the provided {@code func}.
     * The result remains an @see {@link Input<T>} so that dependent resources
     * can be properly tracked.
     * <p/>
     * {@code func} is not allowed to make resources.
     * <p/>
     * {@code func} can return other @see {@link Input<T>}s.  This can be handy if
     * you have an <code>Input&lt;SomeType&gt;</code> and you want to get a transitive dependency of it.  i.e.:
     * <br/>
     * <code>
     * Input&lt;SomeType&gt; d1 = ...;
     * Input&lt;OtherType&gt; d2 = d1.apply(v -> v.otherOutput); // getting an input off of 'v'.
     * </code>
     * <p/>
     * In this example, taking a dependency on d2 means a resource will depend on all the resources
     * of d1. It will <b>not</b> depend on the resources of v.x.y.OtherDep.
     * <p/>
     * Importantly, the Resources that d2 feels like it will depend on are the same resources
     * as d1.
     * <p/>
     * If you need have multiple @see {@link Input<T>}s and a single @see {@link Input<T>}
     * is needed that combines both set of resources, then @see {@link Input#allInputs(Input[])}
     * or {@link Input#tuple(Input, Input, Input)} should be used instead.
     * <p/>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     */
    <U> Input<U> apply(Function<T, Input<U>> func);

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyValue(Function<T, U> func) {
        return apply(t -> Input.of(func.apply(t)));
    }

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyFuture(Function<T, CompletableFuture<U>> func) {
        return apply(t -> Input.of(func.apply(t)));
    }

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyOutput(Function<T, Output<U>> func) {
        return apply(t -> func.apply(t).toInput());
    }

    @CanIgnoreReturnValue
    default Input<Void> applyVoid(Consumer<T> consumer) {
        return apply(t -> {
            consumer.accept(t);
            return Input.empty();
        });
    }

    // Static section -----

    static <T> Input<T> of(T value) {
        return new InputDefault<>(value);
    }

    static <T> Input<T> of(CompletableFuture<T> value) {
        return new InputDefault<>(value, false);
    }

    static <T> Input<T> ofSecret(T value) {
        return new InputDefault<>(value, true);
    }

    static <T> Input<T> empty() {
        return new InputDefault<>(InputOutputData.empty());
    }

    // Tuple Overloads that take different numbers of inputs or outputs.

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2> Input<Tuples.Tuple2<T1, T2>> tuple(Input<T1> item1, Input<T2> item2) {
        return tuple(item1, item2, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3> Input<Tuples.Tuple3<T1, T2, T3>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4> Input<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5> Input<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4, Input<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6> Input<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4, Input<T5> item5, Input<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Input<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6, Input<T7> item7
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the @see {@link Input} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the @see {@link Input}s are not known, the final result will be not known.  Similarly,
     * if any of the @see {@link Input}s are secrets, then the final result will be a secret.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Input<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6, Input<T7> item7, Input<T8> item8
    ) {
        return new InputDefault<>(InputOutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }
}
