package com.pulumi.deployment.internal;

import com.pulumi.deployment.Deployment;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DeploymentInstanceTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testDeploymentInstancePropertyIsProtected() {
        // confirm we cannot retrieve deployment instance early
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(DeploymentImpl::getInstance)
                .withMessageContaining("Trying to acquire Deployment#instance before");

        var test = PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();

        // confirm we've initialized the global singleton holder
        assertThat(DeploymentImpl.getInstance()).isNotNull();

        var result = test.runTest(ctx -> {
                    // try to double-set the Deployment#instance
                    DeploymentImpl.setInstance(Deployment.getInstance());
        });

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(result.exceptions().get(1)).hasMessageContaining("Deployment#instance should only be set once");
    }
}
