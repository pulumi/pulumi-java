package io.pulumi.core;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.pulumi.core.internal.InputOutputData.internalAllHelperAsync;
import static io.pulumi.core.internal.InputOutputImpl.TupleZeroIn;
import static io.pulumi.core.internal.InputOutputImpl.TupleZeroOut;

public interface Output<T> extends InputOutput<T, Output<T>> {

    /**
     * Convert @see {@link Output<T>} to @see {@link Input<T>}
     *
     * @return an {@link Input<T>} , converted from {@link Output<T>}
     */
    Input<T> toInput();

    /**
     * Transforms the data of this @see {@link Output<T>} with the provided {@code func}.
     * The result remains an @see {@link Output<T>} so that dependent resources
     * can be properly tracked.
     * <p/>
     * {@code func} is not allowed to make resources.
     * <p/>
     * {@code func} can return other @see {@link Output<T>}s.  This can be handy if
     * you have an <code>Output&lt;SomeType&gt;</code> and you want to get a transitive dependency of it.  i.e.:
     * <br/>
     * <code>
     * Output&lt;SomeType&gt; d1 = ...;
     * Output&lt;OtherType&gt; d2 = d1.apply(v -> v.otherOutput); // getting an output off of 'v'.
     * </code>
     * <p/>
     * In this example, taking a dependency on d2 means a resource will depend on all the resources
     * of d1. It will <b>not</b> depend on the resources of v.x.y.OtherDep.
     * <p/>
     * Importantly, the Resources that d2 feels like it will depend on are the same resources
     * as d1.
     * <p/>
     * If you need have multiple @see {@link Output<T>}s and a single @see {@link Output<T>}
     * is needed that combines both set of resources, then @see {@link Output#allInputs(Input[])}
     * or {@link Output#tuple(Input, Input, Input)} should be used instead.
     * <p/>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     */
    <U> Output<U> apply(Function<T, Output<U>> func);

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyValue(Function<T, U> func) {
        return apply(t -> Output.of(func.apply(t)));
    }

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyFuture(Function<T, CompletableFuture<U>> func) {
        return apply(t -> Output.of(func.apply(t)));
    }

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyInput(Function<T, Input<U>> func) {
        return apply(t -> func.apply(t).toOutput());
    }

    @CanIgnoreReturnValue
    default Output<Void> applyVoid(Consumer<T> consumer) {
        return apply(t -> {
            consumer.accept(t);
            return Output.empty();
        });
    }

    // Static section -----

    static <T> Output<T> of(T value) {
        return new OutputDefault<>(value);
    }

    static <T> Output<T> of(CompletableFuture<T> value) {
        return new OutputDefault<>(value, false);
    }

    static <T> Output<T> ofSecret(T value) {
        return new OutputDefault<>(value, true);
    }

    static <T> Output<T> empty() {
        return new OutputDefault<>(InputOutputData.empty());
    }

    /**
     * Combines all the @see {@link io.pulumi.core.Input<T>} values in {@code inputs} into a single @see {@link Output}
     * with an @see {@link java.util.List<T>} containing all their underlying values.
     * <p>
     * If any of the {@link io.pulumi.core.Input<T>}s are not known, the final result will be not known.
     * Similarly, if any of the {@link io.pulumi.core.Input<T>}s are secrets, then the final result will be a secret.
     */
    @SafeVarargs // safe because we only call List.of, that is also @SafeVarargs
    static <T> Output<List<T>> allInputs(Input<T>... inputs) {
        return allInputs(List.of(inputs));
    }

    /**
     * @see Output#allInputs(Input[]) for more details.
     */
    static <T> Output<List<T>> allInputs(Iterable<Input<T>> inputs) {
        return allInputs(Lists.newArrayList(inputs));
    }

    /**
     * Combines all the @see {@link Output<T>} values in {@code outputs}
     * into a single @see {@link Output<T>} with an @see {@link java.util.List<T>}
     * containing all their underlying values.
     * <p/>
     * If any of the @see {@link Output<T>}s are not known, the final result will be not known.
     * Similarly, if any of the @see {@link Output<T>}s are secrets, then the final result will be a secret.
     */
    @SafeVarargs // safe because we only call List.of, that is also @SafeVarargs
    static <T> Output<List<T>> allOutputs(Output<T>... outputs) {
        return allOutputs(List.of(outputs));
    }

    /**
     * @see Output#allOutputs(Output[])  for more details.
     */
    static <T> Output<List<T>> allOutputs(Iterable<Output<T>> outputs) {
        return allOutputs(Lists.newArrayList(outputs));
    }

    private static <T> Output<List<T>> allInputs(List<Input<T>> inputs) {
        return new OutputDefault<>(
                internalAllHelperAsync(inputs
                        .stream()
                        .map(input -> TypedInputOutput.cast(input).internalGetDataAsync())
                        .collect(Collectors.toList()))
        );
    }

    private static <T> Output<List<T>> allOutputs(List<Output<T>> outputs) {
        return new OutputDefault<>(
                internalAllHelperAsync(outputs
                        .stream()
                        .map(output -> TypedInputOutput.cast(output).internalGetDataAsync())
                        .collect(Collectors.toList()))
        );
    }
    // Tuple Overloads that take different numbers of inputs or outputs.

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2> Output<Tuples.Tuple2<T1, T2>> tuple(Input<T1> item1, Input<T2> item2) {
        return tuple(item1, item2, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3> Output<Tuples.Tuple3<T1, T2, T3>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4> Output<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5> Output<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4, Input<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6> Output<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Output#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Output<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
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
    static <T1, T2, T3, T4, T5, T6, T7, T8> Output<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6, Input<T7> item7, Input<T8> item8
    ) {
        return new OutputDefault<>(InputOutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2> Output<Tuples.Tuple2<T1, T2>> tuple(Output<T1> item1, Output<T2> item2) {
        return tuple(item1, item2, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3> Output<Tuples.Tuple3<T1, T2, T3>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4> Output<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5> Output<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6> Output<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Output<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the @see {@link Output} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the @see {@link Output}s are not known, the final result will be not known.  Similarly,
     * if any of the @see {@link Output}s are secrets, then the final result will be a secret.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Output<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7, Output<T8> item8
    ) {
        return new OutputDefault<>(InputOutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }
}