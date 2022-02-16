package io.pulumi.core;

import io.pulumi.core.internal.InputOutputData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.BiFunction;

public interface CloudFuture<T>  {
    CompletableFuture<InputOutputData<T>> toCompletableFuture();
    CloudFutureContext getContext();

    default <U> CloudFuture<U> thenApply(Function<? super T,? extends U> fn) {
        return CloudFuture.of(getContext(), toCompletableFuture().thenApply(x -> x.map(fn)));
    }

    default <U,V> CloudFuture<V> thenCombine(CloudFuture<? extends U> other,
                                             BiFunction<? super T,? super U,? extends V> fn) {
        return CloudFuture.of(getContext(), toCompletableFuture()
                .thenCombine(other.toCompletableFuture(), (x, y) -> x.combine(y, fn)));
    }

    default <U> CloudFuture<U> thenCompose(Function<? super T,? extends CloudFuture<U>> fn) {
        return CloudFuture.of(getContext(), toCompletableFuture().thenCompose(rT ->
                rT.traverseFuture(t -> fn.apply(t).toCompletableFuture()).thenApply(InputOutputData::join)
        ));
    }

    static <T> CloudFuture<T> of(CloudFutureContext context, CompletableFuture<InputOutputData<T>> innerFuture) {
        var future = new ResultFuture<T>(context, innerFuture);
        context.registerFuture(innerFuture);
        return future;
    }

    class ResultFuture<T> implements CloudFuture<T> {
        private CompletableFuture<InputOutputData<T>> innerFuture;
        private CloudFutureContext context;

        ResultFuture(CloudFutureContext context, CompletableFuture<InputOutputData<T>> innerFuture) {
            this.innerFuture = innerFuture;
            this.context = context;
        }

        @Override
        public CompletableFuture<InputOutputData<T>> toCompletableFuture() {
            return innerFuture;
        }

        @Override
        public CloudFutureContext getContext() {
            return context;
        }
    }
}

