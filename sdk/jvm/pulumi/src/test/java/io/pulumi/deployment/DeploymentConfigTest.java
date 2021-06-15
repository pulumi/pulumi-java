package io.pulumi.deployment;

import org.junit.jupiter.api.Test;

import static io.pulumi.deployment.internal.DeploymentTests.parseConfig;
import static io.pulumi.deployment.internal.DeploymentTests.parseConfigSecretKeys;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentConfigTest {

    @Test
    void testParseConfig() {
        var config = parseConfig("{\"test\":\"test\"}");
        assertThat(config).hasSize(1);
        assertThat(config).containsEntry("test", "test");
    }

    @Test
    void testParseConfigSecretKeys() {
        var config = parseConfigSecretKeys("[\"test\"]");

        assertThat(config).hasSize(1);
        assertThat(config).contains("test");
    }
}
