package io.pulumi.core;

import io.pulumi.core.internal.InputOutputData;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CloudFutureTests {

    @Test
    public void verifyThenApply() {
        assert get(intFuture(1).thenApply(x -> x + 1)).orElse(-1) == 2;
    }

    @Test
    public void verifyThenCombine() {
        assert get(intFuture(1).thenCombine(intFuture(2), (x, y) -> x + y)).orElse(-1) == 3;
    }

    @Test
    public void verifyThenCompose() {
        assert get(intFuture(1).thenCompose(x -> intFuture(x + 1))).orElse(-1) == 2;
    }

    @Test
    public void verifyAbilityToImplementCloudFutureInCustomClasses() {
        var lst = new List123(CloudFutureContext.ignore());
        assert get(lst).map(x -> x.get(0) + x.get(1) + x.get(2)).orElse(-1) == 1 + 2 + 3;
    }

    private CloudFuture<Integer> intFuture(int i) {
        return CloudFuture.of(CloudFutureContext.ignore(), CompletableFuture.completedFuture(InputOutputData.of(i)));
    }

    private <T> Optional<T> get(CloudFuture<T> future) {
        try {
            return future.toCompletableFuture().get().getValueOptional();
        } catch (InterruptedException e) {
            return Optional.empty();
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    class List123 implements CloudFuture<List<Integer>> {
        private CloudFutureContext context;

        public List123(CloudFutureContext context) {
            this.context = context;
        }

        @Override
        public CompletableFuture<InputOutputData<List<Integer>>> toCompletableFuture() {
            return CompletableFuture.completedFuture(InputOutputData.of(List.of(1, 2, 3)));
        }

        @Override
        public CloudFutureContext getContext() {
            return this.context;
        }
    }
}
