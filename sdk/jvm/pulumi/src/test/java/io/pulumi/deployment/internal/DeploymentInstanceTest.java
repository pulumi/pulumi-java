package io.pulumi.deployment.internal;

import io.pulumi.deployment.MocksTest;
import io.pulumi.internal.PulumiMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static io.pulumi.internal.PulumiMock.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DeploymentInstanceTest {

    @AfterEach
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testDeploymentInstancePropertyIsProtected() {
        // confirm we cannot retrieve deployment instance early
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(DeploymentImpl::getInstance)
                .withMessageContaining("Trying to acquire Deployment#instance before");

        final var mock = PulumiMock.builder()
                .setMocks(new MocksTest.MyMocks())
                .buildSpyGlobalInstance();

        var task = mock.runAsync(
                ctx -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(new DeploymentInstanceInternal(mock.deployment));
                    return ctx.exports();
                }
        );

        // should not throw until awaited
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(task::join)
                .withCauseInstanceOf(IllegalStateException.class)
                .withMessageContaining("Deployment#instance should only be set once");
    }
}
