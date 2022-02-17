package io.pulumi.core.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Objects;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@InternalUse
public class CompletableFutures {

    private CompletableFutures() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * @param futures tasks to await completion of
     * @return a future with all given nested futures completed and joined
     */
    public static <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> futures) {
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(unused -> futures.stream()
                        .filter(ignoreNullValues())
                        .map(CompletableFuture::join) // join() is not blocking here, by the time this function is called, the future is guaranteed to be complete
                        .collect(Collectors.toList())
                );
    }

    /**
     * @param futures tasks to await completion of
     * @return a future with all given nested futures completed (but not joined)
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed exceptionally or a completion computation threw an exception
     */
    public static <T> CompletableFuture<Collection<CompletableFuture<T>>> flatAllOf(Collection<CompletableFuture<T>> futures) {
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(unused -> futures);
    }

    /**
     * @param futuresMap tasks to await completion of
     * @return a future with all given nested futures completed and joined, {@code null} values will be ignored
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed exceptionally or a completion computation threw an exception
     */
    public static <K, V> CompletableFuture<Map<K, V>> allOf(Map<K, CompletableFuture<V>> futuresMap) {
        return CompletableFuture
                .allOf(futuresMap.values().toArray(new CompletableFuture[futuresMap.size()]))
                .thenApply(unused -> futuresMap.entrySet().stream()
                        .filter(ignoreNullMapValues()) // join() is not blocking here, by the time this function is called, the future is guaranteed to be complete
                        .map(joinMapValues()) // join() is not blocking here, by the time this function is called, the future is guaranteed to be complete
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
    }

    /**
     * @param futuresMap tasks to await completion of
     * @return a future with all given nested futures completed
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException   if this future completed exceptionally or a completion computation threw an exception
     */
    public static <K, V> CompletableFuture<Map<K, CompletableFuture<V>>> flatAllOf(
            Map<K, CompletableFuture<V>> futuresMap
    ) {
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


    public static <T> Predicate<? super CompletableFuture<T>> ignoreNullValues() {
        return e -> e.join() != null;
    }

    public static <K, V> Predicate<? super Map.Entry<K, CompletableFuture<V>>> ignoreNullMapValues() {
        return e -> e.getValue().join() != null;
    }

    public static <K, V> Function<Map.Entry<K, CompletableFuture<V>>, Map.Entry<K, V>> joinMapValues() {
        return e -> Map.entry(e.getKey(), e.getValue().join());
    }
}
