package io.pulumi.core;

import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.MocksTest;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrnTest {

    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .setOptions(new TestOptions(false))
                .setMockGlobalInstance();
    }

    @Test
    void testCreateUrnInputOutput() {
        mock.run(() -> {
            var urn = Urn.create(
                    Output.of("name"),
                    Output.of("type"),
                    null,
                    null,
                    null,
                    null
            );

            var value = Internal.of(urn).getValueNullable().join();
            assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
        });
    }

    @Test
    void testCreateUrnString() {
        mock.run(() -> {
            var urn = Urn.create(
                    Output.of("name"),
                    Output.of("type"),
                    null,
                    null,
                    null,
                    null
            );

            var value = Internal.of(urn).getValueNullable().join();
            assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
        });
    }
}
