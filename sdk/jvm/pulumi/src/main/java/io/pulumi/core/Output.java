package io.pulumi.core;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.Copyable;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.Deployment;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Output<T> extends Copyable<Output<T>> {

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
     * is needed that combines both set of resources, then @see {@link Output#all(Output[])}
     * or {@link Output#tuple(Output, Output, Output)} should be used instead.
     * <p/>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     */
    <U> Output<U> apply(Function<T, Output<U>> func);

    /**
     * The deployment object this Output is associated with.
     */
    Deployment getDeployment();

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyValue(Function<T, U> func) {
        return apply(t -> OutputBuilder.forDeployment(getDeployment()).of(func.apply(t)));
    }

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyOptional(Function<T, Optional<U>> func) {
        return apply(t -> OutputBuilder.forDeployment(getDeployment()).ofOptional(func.apply(t))); // TODO: a candidate to move to Output.ofOptional
    }

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyFuture(Function<T, CompletableFuture<U>> func) {
        return apply(t -> OutputBuilder.forDeployment(getDeployment()).of(func.apply(t)));
    }

    @CanIgnoreReturnValue
    default Output<Void> applyVoid(Consumer<T> consumer) {
        return apply(t -> {
            consumer.accept(t);
            return OutputBuilder.forDeployment(getDeployment()).empty();
        });
    }

    /**
     * Creates a shallow copy (the underlying CompletableFuture is copied) of this @see {@link Output<T>}
     *
     * @return a shallow copy of the @see {@link Output<T>}
     */
    Output<T> copy();

    /**
     * Returns a new @see {@link Output<T>} which is a copy of the existing output but marked as
     * a non-secret. The original output or input is not modified in any way.
     */
    Output<T> asPlaintext();

    /**
     * Returns a new @see {@link Output<T>} which is a copy of the existing output but marked as
     * a secret. The original output or input is not modified in any way.
     */
    Output<T> asSecret();
}
