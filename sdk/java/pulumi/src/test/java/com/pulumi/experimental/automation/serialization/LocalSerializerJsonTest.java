// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization;

import org.junit.jupiter.api.Test;

import com.google.gson.reflect.TypeToken;
import com.pulumi.experimental.automation.ConfigValue;
import com.pulumi.experimental.automation.OperationType;
import com.pulumi.experimental.automation.PluginInfo;
import com.pulumi.experimental.automation.PluginKind;
import com.pulumi.experimental.automation.UpdateKind;
import com.pulumi.experimental.automation.UpdateState;
import com.pulumi.experimental.automation.UpdateSummary;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LocalSerializerJsonTest {
    private static final LocalSerializer serializer = new LocalSerializer();

    @Test
    void testDynamic() {
        var json = "{\n" +
            "    \"one\": 123,\n" +
            "    \"two\": \"two\",\n" +
            "    \"three\": true,\n" +
            "    \"nested\": {\n" +
            "        \"test\": \"test\",\n" +
            "        \"testtwo\": 123\n" +
            "    }\n" +
            "}";

        var type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = serializer.deserializeJson(json, type);

        assertThat(map)
            .isNotNull()
            .isNotEmpty()
            .hasSize(4);
    }

    @Test
    public void testDeserializeConfigValue() {
        var json = "{\n" +
            "    \"aws:region\": {\n" +
            "        \"value\": \"us-east-1\",\n" +
            "        \"secret\": false\n" +
            "    },\n" +
            "    \"project:name\": {\n" +
            "        \"value\": \"test\",\n" +
            "        \"secret\": true\n" +
            "    }\n" +
            "}";

        var type = new TypeToken<Map<String, ConfigValue>>() {
        }.getType();
        Map<String, ConfigValue> config = serializer.deserializeJson(json, type);
        assertThat(config)
                .isNotNull()
                .hasSize(2);
        assertThat(config.get("aws:region").value()).isEqualTo("us-east-1");
        assertThat(config.get("aws:region").isSecret()).isFalse();
        assertThat(config.get("project:name").value()).isEqualTo("test");
        assertThat(config.get("project:name").isSecret()).isTrue();
    }

    @Test
    public void testDeserializePluginInfo() {
        var json = "{\n" +
            "    \"name\": \"aws\",\n" +
            "    \"kind\": \"resource\",\n" +
            "    \"version\": \"3.19.2\",\n" +
            "    \"size\": 258460028,\n" +
            "    \"installTime\": \"2020-12-09T19:24:23.214Z\",\n" +
            "    \"lastUsedTime\": \"2020-12-09T19:24:26.059Z\"\n" +
            "}";

        var installTime = Instant.parse("2020-12-09T19:24:23.214Z");
        var lastUsedTime = Instant.parse("2020-12-09T19:24:26.059Z");

        var info = serializer.deserializeJson(json, PluginInfo.class);
        assertThat(info).isNotNull();
        assertThat(info.getName()).isEqualTo("aws");
        assertThat(info.getKind()).isEqualTo(PluginKind.RESOURCE);
        assertThat(info.getVersion()).isEqualTo("3.19.2");
        assertThat(info.getSize()).isEqualTo(258460028);
        assertThat(info.getInstallTime()).isEqualTo(installTime);
        assertThat(info.getLastUsedTime()).isEqualTo(lastUsedTime);
    }

    @Test
    public void testDeserializeUpdateSummary() {
        var json = "[\n" +
            "  {\n" +
            "    \"kind\": \"destroy\",\n" +
            "    \"startTime\": \"2021-01-07T17:08:49.000Z\",\n" +
            "    \"message\": \"\",\n" +
            "    \"environment\": {\n" +
            "        \"exec.kind\": \"cli\"\n" +
            "    },\n" +
            "    \"config\": {\n" +
            "        \"aws:region\": {\n" +
            "            \"value\": \"us-east-1\",\n" +
            "            \"secret\": false\n" +
            "        },\n" +
            "        \"quickstart:test\": {\n" +
            "            \"value\": \"okok\",\n" +
            "            \"secret\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"result\": \"in-progress\",\n" +
            "    \"endTime\": \"2021-01-07T17:09:14.000Z\",\n" +
            "    \"resourceChanges\": {\n" +
            "        \"delete\": 3,\n" +
            "        \"discard\": 1\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"kind\": \"update\",\n" +
            "    \"startTime\": \"2021-01-07T17:02:10.000Z\",\n" +
            "    \"message\": \"\",\n" +
            "    \"environment\": {\n" +
            "        \"exec.kind\": \"cli\"\n" +
            "    },\n" +
            "    \"config\": {\n" +
            "        \"aws:region\": {\n" +
            "            \"value\": \"us-east-1\",\n" +
            "            \"secret\": false\n" +
            "        },\n" +
            "        \"quickstart:test\": {\n" +
            "            \"value\": \"okok\",\n" +
            "            \"secret\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"result\": \"succeeded\",\n" +
            "    \"endTime\": \"2021-01-07T17:02:24.000Z\",\n" +
            "    \"resourceChanges\": {\n" +
            "      \"create\": 3\n" +
            "    }\n" +
            "  }\n" +
            "]";

        var type = new TypeToken<List<UpdateSummary>>() {
        }.getType();
        List<UpdateSummary> history = serializer.deserializeJson(json, type);

        assertThat(history)
                .isNotNull()
                .hasSize(2);

        var destroy = history.get(0);
        assertThat(destroy.getKind()).isEqualTo(UpdateKind.DESTROY);
        assertThat(destroy.getResult()).isEqualTo(UpdateState.IN_PROGRESS);
        assertThat(destroy.getResourceChanges())
                .isNotNull()
                .hasSize(2);
        assertThat(destroy.getResourceChanges().get(OperationType.DELETE)).isEqualTo(3);
        assertThat(destroy.getResourceChanges().get(OperationType.READ_DISCARD)).isEqualTo(1);
    }
}
