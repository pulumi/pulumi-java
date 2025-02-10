// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.pulumi.experimental.automation.*;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;

public class StackSettingsYamlTest {
    private static final LocalSerializer serializer = new LocalSerializer();

    @Test
    void testDeserializingEmptyYaml() {
        var yaml = "";
        var settings = serializer.deserializeYaml(yaml, StackSettings.class);
        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(StackSettings.class);
        assertThat(settings.secretsProvider()).isNull();
        assertThat(settings.encryptedKey()).isNull();
        assertThat(settings.encryptionSalt()).isNull();
        assertThat(settings.config()).isEmpty();
    }

    @Test
    void testDeserializingSecretsProviderYaml() {
        var yaml = "secretsprovider: foo\n";
        var settings = serializer.deserializeYaml(yaml, StackSettings.class);
        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(StackSettings.class);
        assertThat(settings.secretsProvider()).isEqualTo("foo");
        assertThat(settings.encryptedKey()).isNull();
        assertThat(settings.encryptionSalt()).isNull();
        assertThat(settings.config()).isEmpty();

        var serialized = serializer.serializeYaml(settings);
        assertThat(serialized).isEqualTo(yaml);
    }

    @Test
    void testDeserializingFullYaml() {
        var yaml = "config:\n" +
                "  aws:region: us-west-2\n" +
                "  baz: qux\n" +
                "  foo:\n" +
                "    secure: bar\n" +
                "  nestedlist:\n" +
                "  - nestedvalue1\n" +
                "  nestedobj:\n" +
                "    nestedkey: nestedvalue\n" +
                "  num: 42\n" +
                "encryptedkey: key\n" +
                "encryptionsalt: salt\n" +
                "secretsprovider: foo\n";
        var settings = serializer.deserializeYaml(yaml, StackSettings.class);
        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(StackSettings.class);
        var config = settings.config();
        assertThat(config).isNotNull();
        assertThat(config).containsOnlyKeys("aws:region", "baz", "foo", "nestedlist", "nestedobj", "num");
        var awsRegion = config.get("aws:region");
        assertThat(awsRegion.value()).isEqualTo("us-west-2");
        assertThat(awsRegion.isSecure()).isFalse();
        var baz = config.get("baz");
        assertThat(baz.value()).isEqualTo("qux");
        assertThat(baz.isSecure()).isFalse();
        var foo = config.get("foo");
        assertThat(foo.value()).isEqualTo("bar");
        assertThat(foo.isSecure()).isTrue();
        var nestedObj = config.get("nestedobj");
        assertThat(nestedObj.value()).isEqualTo(Map.of("nestedkey", "nestedvalue"));
        assertThat(nestedObj.isSecure()).isFalse();
        var nestedList = config.get("nestedlist");
        assertThat(nestedList.value()).isEqualTo(List.of("nestedvalue1"));
        assertThat(nestedList.isSecure()).isFalse();
        var num = config.get("num");
        assertThat(num.value()).isEqualTo(42);
        assertThat(num.isSecure()).isFalse();
        assertThat(settings.encryptedKey()).isEqualTo("key");
        assertThat(settings.encryptionSalt()).isEqualTo("salt");
        assertThat(settings.secretsProvider()).isEqualTo("foo");

        var serialized = serializer.serializeYaml(settings);
        assertThat(serialized).isEqualTo(yaml);
    }
}
