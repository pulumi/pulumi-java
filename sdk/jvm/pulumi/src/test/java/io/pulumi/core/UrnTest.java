package io.pulumi.core;

import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.MocksTest;
import io.pulumi.internal.PulumiMock;
import io.pulumi.internal.TestRuntimeContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.pulumi.internal.PulumiMock.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

class UrnTest {

    private static PulumiMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = PulumiMock.builder()
            .setMocks(new MocksTest.MyMocks())
            .setRuntimeContext(
                    TestRuntimeContext.builder()
                    .setDryRun(false)
                    .build()
            )
            .buildMockGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testCreateUrnInputOutput() {
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
    }

    @Test
    void testCreateUrnString() {
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
    }
}
