package io.pulumi.core.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.Internal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Internal
public class CompletableFutures {

    private CompletableFutures() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * @param futures tasks to await completion of
     * @return a future with all given nested futures completed
     */
    public static <T> CompletableFuture<Collection<CompletableFuture<T>>> allOf(Collection<CompletableFuture<T>> futures) {
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(unused -> futures);
    }

    /**
     * @param futuresMap tasks to await completion of
     * @return a future with all given nested futures completed
     */
    public static <K, V> CompletableFuture<Map<K, CompletableFuture<V>>> allOf(Map<K, CompletableFuture<V>> futuresMap) {
        return CompletableFuture
                .allOf(futuresMap.values().toArray(new CompletableFuture[futuresMap.size()]))
                .thenApply(unused -> futuresMap);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> CompletableFuture<Optional<T>> flipOptional(Optional<CompletableFuture<T>> optionalFuture) {
        return optionalFuture
                .orElseGet(() -> CompletableFuture.completedFuture(null))
                .thenApply(Optional::ofNullable);
    }

    public static <T> CompletableFuture<Optional<T>> flipOptional(Supplier<Optional<CompletableFuture<T>>> supplier) {
        return CompletableFuture.supplyAsync(supplier).thenCompose(CompletableFutures::flipOptional);
    }

    public static <T> Builder<T> builder(CompletableFuture<T> future) {
        return new Builder<>(future);
    }

    public static class Builder<T> {
        protected CompletableFuture<T> future;

        public Builder(CompletableFuture<T> future) {
            this.future = Objects.requireNonNull(future).copy();
        }

        @CanIgnoreReturnValue
        public <U> Builder<T> accumulate(CompletableFuture<U> more, BiFunction<T, U, T> reduce) {
            this.future = this.future.thenCompose(t -> more.thenApply(u -> reduce.apply(t, u)));
            return this;
        }

        public <U, R> Builder<R> transform(CompletableFuture<U> more, BiFunction<T, U, R> reduce) {
            return new Builder<>(
                    this.future.thenCompose(t -> more.thenApply(u -> reduce.apply(t, u)))
            );
        }

        public <U> CompletableFuture<U> build(Function<T, U> build) {
            return future.thenApply(build);
        }

        public CompletableFuture<T> build() {
            return future;
        }
    }
}
