package com.pulumi.deployment;

import com.pulumi.core.internal.Internal;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.mock.MockCallArgs;
import com.pulumi.test.mock.MonitorMocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static com.pulumi.deployment.internal.DeploymentTests.defaultLogger;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentExceptionTest {

    private static Logger logger;
    private static PulumiTestInternal mock;

    @BeforeAll
    public static void mockSetup() {
        logger = defaultLogger();
        mock = PulumiTestInternal.withDefaults()
                .mocks(new MyIncorrectMocks())
                .standardLogger(logger)
                .useRealRunner()
                .build();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testUrnFutureDoesNotHangOnException() {
        logger.setLevel(Level.OFF);
        var result = mock.runTestAsync(ctx -> {
            var instance = new MonitorMocksTest.Instance("i1", null, null);
            var out = instance.getUrn();
            Internal.of(out).getDataAsync().orTimeout(1, TimeUnit.SECONDS).join();
        }).join();

        var exceptions = result.exceptions();
        assertThat(exceptions).hasSize(2);
        assertThat(exceptions.get(0)).isInstanceOf(CompletionException.class);
        assertThat(exceptions.get(1)).isInstanceOf(DeliberateException.class);
    }

    static class DeliberateException extends IllegalStateException {
        // Empty
    }

    static class MyIncorrectMocks implements MonitorMocks {

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
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
