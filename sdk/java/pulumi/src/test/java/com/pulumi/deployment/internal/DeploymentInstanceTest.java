package com.pulumi.deployment.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DeploymentInstanceTest {

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testDeploymentInstancePropertyIsProtected() {
        // confirm we cannot retrieve deployment instance early
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(DeploymentImpl::getInstance)
                .withMessageContaining("Trying to acquire Deployment#instance before");

        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .build();
        mock.standardLogger.setLevel(Level.OFF);

        var task = mock.runTestAsync(
                ctx -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(new DeploymentInstanceInternal(mock.deployment));
                }
        );

        // should not throw until awaited
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(task::join)
                .withCauseInstanceOf(IllegalStateException.class)
                .withMessageContaining("Deployment#instance should only be set once");
    }
}
