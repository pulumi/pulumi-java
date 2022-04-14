package io.pulumi.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentConfigTest {

    @Test
    void testParseConfig() {
        var config = ConfigInternal.parseConfig("{\"test\":\"test\"}");
        assertThat(config).hasSize(1);
        assertThat(config).containsEntry("test", "test");
    }

    @Test
    void testParseConfigSecretKeys() {
        var config = ConfigInternal.parseConfigSecretKeys("[\"test\"]");

        assertThat(config).hasSize(1);
        assertThat(config).contains("test");
    }
}
