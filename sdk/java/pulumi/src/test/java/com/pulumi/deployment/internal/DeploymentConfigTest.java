package com.pulumi.deployment.internal;

import com.pulumi.deployment.internal.DeploymentImpl.Config;
import org.junit.jupiter.api.Test;

import static com.pulumi.test.PulumiTest.extractValue;
import static com.pulumi.test.internal.PulumiTestInternal.parseConfig;
import static com.pulumi.test.internal.PulumiTestInternal.parseConfigSecretKeys;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentConfigTest {

    @Test
    void testParseConfig() {
        var json = "{\n" +
                "  \"name\": \"test\"," +
                "  \"aws:region\": \"us-east-1\",\n" +
                "  \"aws-native:region\": \"us-east-1\"\n" +
                "}";

        var config = Config.parseConfig(json);
        assertThat(config).hasSize(3);
        assertThat(config).containsEntry("name", "test");
        assertThat(config).containsEntry("aws:region", "us-east-1");
        assertThat(config).containsEntry("aws-native:region", "us-east-1");
    }

    @Test
    void testParseConfigSecretKeys() {
        var config = Config.parseConfigSecretKeys("[\"test\"]");

        assertThat(config).hasSize(1);
        assertThat(config).contains("test");
    }

    @Test
    void testConfigUsage() {
        var json = "{\n" +
                "  \"name\": \"test\"," +
                "  \"aws:region\": \"us-east-1\",\n" +
                "  \"project:name\": \"minimal\",\n" +
                "  \"project:secret\": \"a secret\"\n" +
                "}";
        var secrets = "[\"project:secret\"]";
        var configMap = parseConfig(json);
        var secretSet = parseConfigSecretKeys(secrets);
        var config = new Config(configMap, secretSet);
        assertThat(config.getConfig("not there")).isEmpty();
        assertThat(config.getConfig("name")).hasValue("test");
        assertThat(config.isConfigSecret("project:secret")).isTrue();
        assertThat(config.getConfig("aws:region")).hasValue("us-east-1");

        var projectConfig = new com.pulumi.Config(config, "project");
        var awsConfig = new com.pulumi.Config(config, "aws");

        assertThat(projectConfig.get("name")).hasValue("minimal");
        assertThat(extractValue(projectConfig.getSecret("secret"))).hasValue("a secret");
        assertThat(awsConfig.get("region")).hasValue("us-east-1");
    }
}
