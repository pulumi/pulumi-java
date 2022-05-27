package com.pulumi.deployment.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;
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

        // confirm we've initialized the global singleton holder
        assertThat(DeploymentImpl.getInstance()).isNotNull();

        var result = mock.runTestAsync(
                ctx -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(new DeploymentInstanceInternal(mock.deployment));
                }
        ).join();

        assertThat(result.exceptions).hasSize(1);
        assertThat(result.exceptions.get(0)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(result.exceptions.get(0)).hasMessageContaining("Deployment#instance should only be set once");
    }
}
