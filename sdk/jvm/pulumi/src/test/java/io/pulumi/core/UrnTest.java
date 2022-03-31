package io.pulumi.core;

import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.MocksTest;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrnTest {

    private static final Deployment deployment = DeploymentTests.DeploymentMockBuilder.builder()
            .setMocks(new MocksTest.MyMocks())
            .setOptions(new TestOptions(false))
            .buildMockInstance()
            .getDeployment();

    private static final OutputBuilder output = OutputBuilder.forDeployment(deployment);

    @Test
    void testCreateUrnInputOutput() {
        var urn = Urn.create(deployment,
                output.of("name"),
                output.of("type"),
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
        var urn = Urn.create(deployment,
                output.of("name"),
                output.of("type"),
                null,
                null,
                null,
                null
        );

        var value = Internal.of(urn).getValueNullable().join();
        assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
    }
}
