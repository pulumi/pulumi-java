package io.pulumi.core;

import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

class UrnTest {

    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMockGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testCreateUrnInputOutput() {
        var urn = Urn.create(
                Input.of("name"),
                Input.of("type"),
                null,
                null,
                null,
                null
        );

        var value = Internal.of(urn).getValueNullable().join();
        assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
    }

    @Test
    void testCreateUrnString() {
        var urn = Urn.create(
                Input.of("name"),
                Input.of("type"),
                null,
                null,
                null,
                null
        );

        var value = Internal.of(urn).getValueNullable().join();
        assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
    }
}