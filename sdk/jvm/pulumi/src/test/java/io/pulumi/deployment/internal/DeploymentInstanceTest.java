package io.pulumi.deployment.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class DeploymentInstanceTest {

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testDeploymentInstancePropertyIsProtected() {
        // confirm we cannot retrieve deployment instance early
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(DeploymentImpl::getInstance)
                .withMessageContaining("Trying to acquire Deployment#instance before");

        var options = new TestOptions();
        var engine = mock(Engine.class);
        var monitor = mock(Monitor.class);

        var config = new DeploymentImpl.Config(ImmutableMap.of(), ImmutableSet.of());
        var state = new DeploymentImpl.DeploymentState(
                config,
                DeploymentTests.defaultLogger(),
                options.getProjectName(),
                options.getStackName(),
                options.isPreview(),
                engine,
                monitor
        );
        var deployment = new DeploymentImpl(state);

        var task = DeploymentInternal.createRunnerAndRunAsync(
                () -> deployment,
                runner -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(new DeploymentInstanceInternal(deployment));
                    return CompletableFuture.completedFuture(1);
                }
        );

        // should not throw until awaited
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(task::join)
                .withCauseInstanceOf(IllegalStateException.class)
                .withMessageContaining("Deployment#instance should only be set once");
    }
}
