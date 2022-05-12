package com.pulumi.serialization.internal;

import com.google.gson.JsonParser;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputData;
import com.pulumi.deployment.MonitorMocksTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMock;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class ReSerializerTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
                .setMocks(new MonitorMocksTest.MyMocks())
                .setMockGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }


    @Nullable
    private Object reSerialize(@Nullable Object o) {
        var deserializer = new Deserializer();
        var serialized = new Serializer(mock.log)
                .serializeAsync("ReSerializerTest", o, true);

        return serialized
                .thenApply(Serializer::createValue)
                .thenApply(deserializer::deserialize)
                .thenApply(OutputData::getValueNullable)
                .join();
    }

    @TestFactory
    Stream<DynamicTest> testSerializeDeserializeCommonTypes() {
        var nr = Stream.iterate(1, i -> i + 1).map(String::valueOf).iterator();
        return Stream.of(
                dynamicTest(nr.next(), () -> assertThat(reSerialize(null)).isNull()),
                dynamicTest(nr.next(), () -> assertThat(reSerialize("test")).isEqualTo("test")),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(true)).isEqualTo(true)),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(false)).isEqualTo(false)),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(1)).isEqualTo(1.0)),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(1.0)).isEqualTo(1.0)),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(List.of())).isEqualTo(List.of())),
                dynamicTest(nr.next(), () -> assertThat(reSerialize(Map.of())).isEqualTo(Map.of())),
                dynamicTest(nr.next(), () ->
                        assertThat(reSerialize(List.of(1))).isEqualTo(List.of(1.0))),
                dynamicTest(nr.next(), () ->
                        assertThat(reSerialize(Map.of("1", 1))).isEqualTo(Map.of("1", 1.0))),
                dynamicTest(nr.next(), () ->
                        assertThat(reSerialize(Map.of("v", List.of("a", "b")))).isEqualTo(Map.of("v", List.of("a", "b")))),
                dynamicTest(nr.next(), () ->
                        assertThat(reSerialize(newArrayList(1, null))).isEqualTo(newArrayList(1.0, null))),
                // we remove null entries explicitly in serialization in serializeMapAsync
                // we remove null entries explicitly in serialization in deserializeStruct
                dynamicTest(nr.next(), () -> {
                    var map = newHashMap();
                    map.put("1", null);
                    assertThat(reSerialize(map)).isEqualTo(Map.of());
                })
        );
    }

    @TestFactory
    Stream<DynamicNode> testSerializeDeserializeJson() {
        return Stream.of(
                dynamicTest("empty", () ->
                        assertThat(reSerialize(JsonParser.parseString(""))).isNull()),
                dynamicTest("empty object", () ->
                        assertThat(reSerialize(JsonParser.parseString("{}"))).isEqualTo(Map.of())),
                dynamicTest("empty array", () ->
                        assertThat(reSerialize(JsonParser.parseString("[]"))).isEqualTo(List.of())),
                dynamicContainer("complex json",
                        Stream.of(JsonParser.parseString("{\"test\": [\"test1\", \"1\"]}"))
                                .map(this::reSerialize)
                                .flatMap(o -> Stream.of(
                                        dynamicTest("not null", () -> assertThat(o).isNotNull()),
                                        dynamicTest("is Map", () -> assertThat(o).isInstanceOf(Map.class)),
                                        dynamicTest("contains", () -> assertThat((Map<Object, Object>) o).containsAllEntriesOf(
                                                Map.of("test", List.of("test1", "1")))
                                        )
                                ))
                )
        );
    }

    @TestFactory
    Stream<DynamicTest> testSerializeDeserializeOutput() {
        return Stream.of(
                dynamicTest("simple output", () ->
                        assertThat(reSerialize(Output.of("test"))).isEqualTo("test")
                ),
                dynamicTest("secret output", () ->
                        assertThat(reSerialize(Output.ofSecret("password"))).isEqualTo("password")
                )
        );
    }
}
