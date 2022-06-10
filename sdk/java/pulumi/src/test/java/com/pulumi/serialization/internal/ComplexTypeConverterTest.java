package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Log;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Constructor;
import com.pulumi.core.annotations.CustomType.Parameter;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.serialization.internal.ConverterTests.ContainerSize;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.pulumi.serialization.internal.ConverterTests.ContainerColor;
import static com.pulumi.serialization.internal.ConverterTests.ContainerColor.Blue;
import static com.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplexTypeConverterTest {

    private final static Log log = PulumiTestInternal.mockLog();

    @CustomType
    public static class ComplexType1 {
        public final String s;
        public final boolean b;
        public final int i;
        public final double d;
        public final ImmutableList<Boolean> list;
        public final ImmutableMap<String, Integer> map;
        public final Object $private;
        public final ContainerSize size;
        public final ContainerColor color;

        @Constructor
        public ComplexType1(
                @Parameter("s") String s,
                @Parameter("b") boolean b,
                @Parameter("i") int i,
                @Parameter("d") double d,
                @Parameter("list") ImmutableList<Boolean> list,
                @Parameter("map") ImmutableMap<String, Integer> map,
                @Parameter("private") Object $private,
                @Parameter("size") ContainerSize size,
                @Parameter("color") ContainerColor color
        ) {
            this.s = s;
            this.b = b;
            this.i = i;
            this.d = d;
            this.list = list;
            this.map = map;
            this.$private = $private;
            this.size = size;
            this.color = color;
        }
    }

    @Test
    void testTestComplexType1() {
        var deserializer = new Deserializer(log);
        var converter = new Converter(log, deserializer);
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("s", "str")
                .put("b", true)
                .put("i", 42)
                .put("d", 1.5)
                .put("list", ImmutableList.of(true, false))
                .put("map", ImmutableMap.of("k", 10))
                .put("private", "test")
                .put("size", 6)
                .put("color", "blue")
                .build()
        ).join();
        var data = converter.convertValue(
                "ComplexTypeConverterTest", serialized, ComplexType1.class
        );

        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable().s).isEqualTo("str");
        assertThat(data.getValueNullable().b).isTrue();
        assertThat(data.getValueNullable().i).isEqualTo(42);
        assertThat(data.getValueNullable().d).isEqualTo(1.5);
        assertThat(data.getValueNullable().list).hasSameElementsAs(ImmutableList.of(true, false));
        assertThat(data.getValueNullable().map).containsAllEntriesOf(ImmutableMap.of("k", 10));
        assertThat(data.getValueNullable().$private).isEqualTo("test");
        assertThat(data.getValueNullable().size).isEqualTo(ContainerSize.SixInch);
        assertThat(data.getValueNullable().color).isEqualTo(Blue);

        assertThat(data.isKnown()).isTrue();
    }

    @CustomType
    public static class ComplexType2 {
        public final ComplexType1 c;
        public final ImmutableList<ComplexType1> c2List;
        public final ImmutableMap<String, ComplexType1> c2Map;

        @Constructor
        public ComplexType2(
                @Parameter("c") ComplexType1 c,
                @Parameter("c2List") ImmutableList<ComplexType1> c2List,
                @Parameter("c2Map") ImmutableMap<String, ComplexType1> c2Map
        ) {
            this.c = c;
            this.c2List = c2List;
            this.c2Map = c2Map;
        }
    }

    @Test
    void testTestComplexType2() {
        var deserializer = new Deserializer(log);
        var converter = new Converter(log, deserializer);
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("c", ImmutableMap.<String, Object>builder()
                        .put("s", "str1")
                        .put("b", false)
                        .put("i", 1)
                        .put("d", 1.1)
                        .put("list", List.of(false, false))
                        .put("map", Map.of("k", 1))
                        .put("private", 50.0)
                        .put("size", 8)
                        .put("color", "red")
                        .build()
                )
                .put("c2List", ImmutableList.of(
                        ImmutableMap.<String, Object>builder()
                                .put("s", "str2")
                                .put("b", true)
                                .put("i", 2)
                                .put("d", 2.2)
                                .put("list", List.of(false, true))
                                .put("map", Map.of("k", 2))
                                .put("private", true)
                                .put("size", 4)
                                .put("color", "yellow")
                                .build()
                ))
                .put("c2Map", ImmutableMap.of(
                        "someKey", ImmutableMap.<String, Object>builder()
                                .put("s", "str3")
                                .put("b", false)
                                .put("i", 3)
                                .put("d", 3.3)
                                .put("list", List.of(true, false))
                                .put("map", Map.of("k", 3))
                                .put("private", Map.of("o", 5.5))
                                .put("size", 6)
                                .put("color", "blue")
                                .build()
                ))
                .build()
        ).join();
        var data = converter.convertValue(
                "ComplexTypeConverterTest", serialized, ComplexType2.class
        ).getValueNullable();

        assertThat(data).isNotNull();

        var value = data.c;
        assertThat(value.s).isEqualTo("str1");
        assertThat(value.b).isEqualTo(false);
        assertThat(value.i).isEqualTo(1);
        assertThat(value.d).isEqualTo(1.1);
        assertThat(value.list).hasSameElementsAs(List.of(false, false));
        assertThat(value.map).containsAllEntriesOf(Map.of("k", 1));
        assertThat(value.$private).isEqualTo(50.0);
        assertThat(value.size).isEqualTo(ContainerSize.EightInch);
        assertThat(value.color).isEqualTo(ContainerColor.Red);

        assertThat(data.c2List).hasSize(1);
        value = data.c2List.get(0);

        assertThat(value.s).isEqualTo("str2");
        assertThat(value.b).isEqualTo(true);
        assertThat(value.i).isEqualTo(2);
        assertThat(value.d).isEqualTo(2.2);
        assertThat(value.list).hasSameElementsAs(List.of(false, true));
        assertThat(value.map).containsAllEntriesOf(Map.of("k", 2));
        assertThat(value.$private).isEqualTo(true);
        assertThat(value.size).isEqualTo(ContainerSize.FourInch);
        assertThat(value.color).isEqualTo(ContainerColor.Yellow);

        assertThat(data.c2Map).hasSize(1);
        assertThat(data.c2Map).containsKey("someKey");
        value = data.c2Map.get("someKey");

        assertThat(value.s).isEqualTo("str3");
        assertThat(value.b).isEqualTo(false);
        assertThat(value.i).isEqualTo(3);
        assertThat(value.d).isEqualTo(3.3);
        assertThat(value.list).hasSameElementsAs(List.of(true, false));
        assertThat(value.map).containsAllEntriesOf(Map.of("k", 3));
        assertThat(value.$private).isInstanceOf(Map.class);
        //noinspection unchecked
        assertThat((Map<String, Object>) value.$private).containsAllEntriesOf(Map.of("o", 5.5));
        assertThat(value.size).isEqualTo(ContainerSize.SixInch);
        assertThat(value.color).isEqualTo(ContainerColor.Blue);
    }

    @CustomType
    public static class UnexpectedNullableComplexType {
        public final String s;

        @Constructor
        public UnexpectedNullableComplexType(@Parameter("s") String s) {
            this.s = s;
        }
    }

    @Test
    void testUnexpectedNullableComplexType() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "ComplexTypeConverterTest#testUnexpectedNullableComplexType");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var deserializer = new Deserializer(log);
        var converter = new Converter(inMemoryLog, deserializer);

        var map = new HashMap<String, Object>();
        map.put("s", null);
        var serialized = serializeToValueAsync(map).join();

        var data = converter.convertValue(
                "ComplexTypeConverterTest", serialized, UnexpectedNullableComplexType.class
        ).getValueNullable();

        assertThat(data).isNotNull();

        var value = data.s;
        assertThat(value).isNull();

        var messages = logger.getMessages();
        assertThat(messages).haveAtLeastOne(containsString(
                "parameter named 's' (nr 0 starting from 0) lacks @javax.annotation.Nullable annotation, so the value is required, but there is no value to deserialize."
        ));
    }

    @CustomType
    public static class EscapedComplexType {
        public final String $private;

        @Constructor
        public EscapedComplexType(@Parameter("$private") String $private) {
            this.$private = $private;
        }
    }

    @Test
    void testEscapedComplexType() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "ComplexTypeConverterTest#testEscapedComplexType");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var deserializer = new Deserializer(log);
        var converter = new Converter(inMemoryLog, deserializer);

        var map = new HashMap<String, Object>();
        map.put("private", "test");
        var serialized = serializeToValueAsync(map).join();

        assertThatThrownBy(() -> converter.convertValue(
                "ComplexTypeConverterTest", serialized, EscapedComplexType.class
        )).isInstanceOf(UnsupportedOperationException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "expects parameter names of: '$private', but does not expect: 'private'. Unable to deserialize."
                );
    }
}
