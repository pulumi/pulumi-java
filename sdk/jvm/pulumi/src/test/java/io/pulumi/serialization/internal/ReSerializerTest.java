package io.pulumi.serialization.internal;

import com.google.gson.JsonParser;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputData;
import io.pulumi.deployment.MocksTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMock;
import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class ReSerializerTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
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
        return Stream.of(
                dynamicTest("1", () -> assertThat(reSerialize(null)).isNull()),
                dynamicTest("2", () -> assertThat(reSerialize("test")).isEqualTo("test")),
                dynamicTest("3", () -> assertThat(reSerialize(true)).isEqualTo(true)),
                dynamicTest("4", () -> assertThat(reSerialize(false)).isEqualTo(false)),
                dynamicTest("5", () -> assertThat(reSerialize(1)).isEqualTo(1.0)),
                dynamicTest("6", () -> assertThat(reSerialize(1.0)).isEqualTo(1.0)),
                dynamicTest("7", () -> assertThat(reSerialize(List.of())).isEqualTo(List.of())),
                dynamicTest("8", () -> assertThat(reSerialize(Map.of())).isEqualTo(Map.of())),
                dynamicTest("9", () -> assertThat(reSerialize(List.of(1))).isEqualTo(List.of(Optional.of(1.0)))),
                dynamicTest("0", () ->
                        assertThat(reSerialize(Map.of("1", 1))).isEqualTo(Map.of("1", Optional.of(1.0)))),
                // we remove null entries explicitly in serialization in serializeListAsync
                dynamicTest("1", () ->
                        assertThat(reSerialize(newArrayList(1, null)))
                                .isEqualTo(List.of(Optional.of(1.0), Optional.empty()))),
                // we remove null entries explicitly in serialization in serializeMapAsync
                dynamicTest("12", () -> {
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
                                                Map.of("test", Optional.of(
                                                        List.of(Optional.of("test1"), Optional.of("1"))
                                                ))
                                        ))
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
