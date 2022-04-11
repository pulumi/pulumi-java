package io.pulumi.deployment;

import io.pulumi.Stack;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.exceptions.RunException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class DeploymentExceptionTest {

    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setMocks(new MyIncorrectMocks())
                .setSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testUrnFutureDoesNotHangOnException() {
        mock.standardLogger.setLevel(Level.OFF);

        assertThatThrownBy(() -> mock.testAsync(MyIncorrectStack::new).join())
                .getRootCause()
                .isInstanceOf(RunException.class)
                .hasMessageContaining(DeliberateException.class.getName());
    }

    public static class MyIncorrectStack extends Stack {
        public MyIncorrectStack() {
            var instance = new MocksTest.Instance("i1", null, null);
            var out = instance.getUrn();
            Internal.of(out).getDataAsync().orTimeout(1, TimeUnit.SECONDS).join();
        }
    }

    static class DeliberateException extends IllegalStateException {
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
