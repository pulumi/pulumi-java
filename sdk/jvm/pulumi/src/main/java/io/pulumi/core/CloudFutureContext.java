package io.pulumi.core;

import java.util.concurrent.CompletableFuture;

public interface CloudFutureContext {
    <T> void registerFuture(CompletableFuture<T> future);

    static CloudFutureContext ignore() {
        return new IgnoreCloudFutureContext();
    }

    class IgnoreCloudFutureContext implements CloudFutureContext {
        @Override
        public <T> void registerFuture(CompletableFuture<T> future) {}
    }
}
