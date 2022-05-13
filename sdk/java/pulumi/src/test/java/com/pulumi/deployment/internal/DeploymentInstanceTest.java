package com.pulumi.deployment.internal;

import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DeploymentInstanceTest {

    @AfterAll
    static void cleanup() {
        PulumiTest.cleanup();
    }

    @Test
    void testDeploymentInstancePropertyIsProtected() {
        // confirm we cannot retrieve deployment instance early
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(DeploymentImpl::getInstance)
                .withMessageContaining("Trying to acquire Deployment#instance before");

        var deploymentReference = new AtomicReference<DeploymentImpl>();
        var mock = PulumiTestInternal.withDefaults()
                .deploymentFactory(state -> {
                    var deployment = new DeploymentImpl(state);
                    deploymentReference.set(deployment);
                    return deployment;
                })
                .build();

        var result = mock.runTestAsync(
                ctx -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(new DeploymentInstanceInternal(deploymentReference.get()));
                }
        );

        // should not throw until awaited
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> { throw result.join().exceptions().get(0); })
                .withCauseInstanceOf(IllegalStateException.class)
                .withMessageContaining("Deployment#instance should only be set once");
    }
}
