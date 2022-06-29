package com.pulumi.deployment;

import com.pulumi.core.Tuples;
import com.pulumi.core.internal.Internal;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentExceptionTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testUrnFutureDoesNotHangOnException() {
        var test = PulumiTestInternal.builder()
                .mocks(new MyIncorrectMocks())
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTestAsync(ctx -> {
            var instance = new MocksTest.Instance("i1", null, null);
            var out = instance.getUrn();
            Internal.of(out).getDataAsync().orTimeout(1, TimeUnit.SECONDS).join();
        }).join();

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(DeliberateException.class);
        assertThat(result.exceptions().get(1)).hasMessageContaining("deliberate exception");
    }

    static class DeliberateException extends IllegalStateException {
        public DeliberateException() {
            super("deliberate exception");
        }
    }

    static class MyIncorrectMocks implements Mocks {

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "aws:ec2/instance:Instance":
                    return CompletableFuture.failedFuture(new DeliberateException());
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
