package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.internal.annotations.OutputCustomType;
import io.pulumi.serialization.internal.ConverterTests.ContainerSize;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.pulumi.serialization.internal.ConverterTests.ContainerColor;
import static io.pulumi.serialization.internal.ConverterTests.ContainerColor.Blue;
import static io.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static org.assertj.core.api.Assertions.assertThat;

class ComplexTypeConverterTest {

    @OutputCustomType
    public static class ComplexType1 {
        public final String s;
        public final boolean b;
        public final int i;
        public final double d;
        public final ImmutableList<Boolean> list;
        public final ImmutableMap<String, Integer> map;
        public final Object obj;
        public final ContainerSize size;
        public final ContainerColor color;

        @OutputCustomType.Constructor({"s", "b", "i", "d", "list", "map", "obj", "size", "color"})
        public ComplexType1(
                String s, boolean b, int i, double d,
                ImmutableList<Boolean> list, ImmutableMap<String, Integer> map, Object obj,
                ContainerSize size, ContainerColor color
        ) {
            this.s = s;
            this.b = b;
            this.i = i;
            this.d = d;
            this.list = list;
            this.map = map;
            this.obj = obj;
            this.size = size;
            this.color = color;
        }
    }

    @Test
    void testTestComplexType1() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("s", "str")
                .put("b", true)
                .put("i", 42)
                .put("d", 1.5)
                .put("list", ImmutableList.of(true, false))
                .put("map", ImmutableMap.of("k", 10))
                .put("obj", "test")
                .put("size", 6)
                .put("color", "blue")
                .build()
        ).join();
        var data = Converter.convertValue(
                "ComplexTypeConverterTest", serialized, ComplexType1.class
        );

        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable().s).isEqualTo("str");
        assertThat(data.getValueNullable().b).isTrue();
        assertThat(data.getValueNullable().i).isEqualTo(42);
        assertThat(data.getValueNullable().d).isEqualTo(1.5);
        assertThat(data.getValueNullable().list).hasSameElementsAs(ImmutableList.of(true, false));
        assertThat(data.getValueNullable().map).containsAllEntriesOf(ImmutableMap.of("k", 10));
        assertThat(data.getValueNullable().obj).isEqualTo("test");
        assertThat(data.getValueNullable().size).isEqualTo(ContainerSize.SixInch);
        assertThat(data.getValueNullable().color).isEqualTo(Blue);

        assertThat(data.isKnown()).isTrue();
    }

    @OutputCustomType
    public static class ComplexType2 {
        public final ComplexType1 c;
        public final ImmutableList<ComplexType1> c2List;
        public final ImmutableMap<String, ComplexType1> c2Map;

        @OutputCustomType.Constructor({"c", "c2List", "c2Map"})
        public ComplexType2(
                ComplexType1 c,
                ImmutableList<ComplexType1> c2List,
                ImmutableMap<String, ComplexType1> c2Map)
        {
            this.c = c;
            this.c2List = c2List;
            this.c2Map = c2Map;
        }
    }

    @Test
    void testTestComplexType2() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("c", ImmutableMap.<String, Object>builder()
                        .put("s", "str1")
                        .put("b", false)
                        .put("i", 1)
                        .put("d", 1.1)
                        .put("list", List.of(false, false))
                        .put("map", Map.of("k", 1))
                        .put("obj", 50.0)
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
                                .put("obj", true)
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
                                .put("obj", Map.of( "o", 5.5))
                                .put("size", 6)
                                .put("color", "blue")
                                .build()
                ))
                .build()
        ).join();
        var data = Converter.convertValue(
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
        assertThat(value.obj).isEqualTo(50.0);
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
        assertThat(value.obj).isEqualTo(true);
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
        assertThat(value.obj).isInstanceOf(Map.class);
        //noinspection unchecked
        assertThat((Map<String, Object>) value.obj).containsAllEntriesOf(Map.of("o", Optional.of(5.5))); // C# didn't have Optional, it has Nullable type.
        assertThat(value.size).isEqualTo(ContainerSize.SixInch);
        assertThat(value.color).isEqualTo(ContainerColor.Blue);
    }
}
