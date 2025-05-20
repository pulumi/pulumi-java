package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Log;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Setter;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.serialization.internal.ConverterTests.ContainerSize;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.pulumi.serialization.internal.ConverterTests.ContainerColor;
import static com.pulumi.serialization.internal.ConverterTests.ContainerColor.Blue;
import static com.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;

class ComplexTypeConverterTest {

    private final static Log log = PulumiTestInternal.mockLog();

    @CustomType
    public static class ComplexType1 {
        private @Nullable String s;
        private boolean b;
        private int i;
        private double d;
        private @Nullable ImmutableList<Boolean> list;
        private @Nullable ImmutableMap<String, Integer> map;
        private @Nullable Object $private;
        private @Nullable ContainerSize size;
        private @Nullable ContainerColor color;

        @Nullable
        public String s() {
            return s;
        }

        public boolean b() {
            return b;
        }

        public int i() {
            return i;
        }

        public double d() {
            return d;
        }

        @Nullable
        public ImmutableList<Boolean> list() {
            return list;
        }

        @Nullable
        public ImmutableMap<String, Integer> map() {
            return map;
        }

        @Nullable
        public Object $private() {
            return $private;
        }

        @Nullable
        public ContainerSize size() {
            return size;
        }

        @Nullable
        public ContainerColor color() {
            return color;
        }

        @CustomType.Builder
        public static final class Builder {
            private final ComplexType1 $;

            public Builder() {
                this.$ = new ComplexType1();
            }

            public Builder(ComplexType1 defaults) {
                this.$ = checkNotNull(defaults);
            }

            @Setter("s")
            public Builder s(@Nullable String s) {
                this.$.s = s;
                return this;
            }

            @Setter("b")
            public Builder b(boolean b) {
                this.$.b = b;
                return this;
            }

            @Setter("i")
            public Builder i(int i) {
                this.$.i = i;
                return this;
            }

            @Setter("d")
            public Builder d(double d) {
                this.$.d = d;
                return this;
            }

            @Setter("list")
            public Builder list(@Nullable ImmutableList<Boolean> list) {
                this.$.list = list;
                return this;
            }

            @Setter("map")
            public Builder map(@Nullable ImmutableMap<String, Integer> map) {
                this.$.map = map;
                return this;
            }

            @Setter("private")
            public Builder $private(Object $private) {
                this.$.$private = $private;
                return this;
            }

            @Setter("size")
            public Builder size(@Nullable ContainerSize size) {
                this.$.size = size;
                return this;
            }

            @Setter("color")
            public Builder color(@Nullable ContainerColor color) {
                this.$.color = color;
                return this;
            }

            public ComplexType1 build() {
                return this.$;
            }
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
                "TestComplexType1", serialized, ComplexType1.class
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

    @Test
    void testTestComplexType1ExtraDataDoesNotThrow() {
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
                .put("extra", "data") // extra data not represented in the ComplexType1 class
                .build()
        ).join();
        var data = converter.convertValue(
                "TestComplexType1", serialized, ComplexType1.class
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
        private @Nullable ComplexType1 c;
        private @Nullable ImmutableList<ComplexType1> c2List;
        private @Nullable ImmutableMap<String, ComplexType1> c2Map;

        public ComplexType1 c() {
            return c;
        }

        public ImmutableList<ComplexType1> c2List() {
            return c2List;
        }

        public ImmutableMap<String, ComplexType1> c2Map() {
            return c2Map;
        }

        @CustomType.Builder
        public static final class Builder {
            private final ComplexType2 $;

            public Builder() {
                this.$ = new ComplexType2();
            }

            public Builder(ComplexType2 defaults) {
                this.$ = checkNotNull(defaults);
            }

            @Setter("c")
            public Builder c(@Nullable ComplexType1 c) {
                this.$.c = c;
                return this;
            }

            @Setter("c2List")
            public Builder c2List(@Nullable ImmutableList<ComplexType1> c2List) {
                this.$.c2List = c2List;
                return this;
            }

            @Setter("c2Map")
            public Builder c2Map(@Nullable ImmutableMap<String, ComplexType1> c2Map) {
                this.$.c2Map = c2Map;
                return this;
            }

            public ComplexType2 build() {
                return this.$;
            }
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
                "TestComplexType2", serialized, ComplexType2.class
        ).getValueNullable();

        assertThat(data).isNotNull();

        var value = data.c;
        assertThat(value).isNotNull();
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
        @Nullable
        private String s;

        @Nullable
        public String s() {
            return s;
        }

        @CustomType.Builder
        public static final class Builder {
            private final UnexpectedNullableComplexType $;

            public Builder() {
                this.$ = new UnexpectedNullableComplexType();
            }

            public Builder(UnexpectedNullableComplexType defaults) {
                this.$ = checkNotNull(defaults);
            }

            @Setter("s")
            public Builder s(String s) {
                this.$.s = s;
                return this;
            }

            public UnexpectedNullableComplexType build() {
                return this.$;
            }
        }
    }

    @Test
    void testUnexpectedNullableComplexType() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "ComplexTypeConverterTest#testUnexpectedNullableComplexType");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var deserializer = new Deserializer(inMemoryLog);
        var converter = new Converter(inMemoryLog, deserializer);

        var map = new HashMap<String, Object>();
        map.put("s", null);
        var serialized = serializeToValueAsync(map).join();

        var data = converter.convertValue(
                "UnexpectedNullableComplexType", serialized, UnexpectedNullableComplexType.class
        ).getValueNullable();

        assertThat(data).isNotNull();

        var value = data.s();
        assertThat(value).isNull();

        var messages = logger.getMessages();
        assertThat(messages).haveAtLeastOne(containsString(
                "parameter named 's' lacks @javax.annotation.Nullable annotation"
        ));
    }

    @CustomType
    public static class EscapedComplexType {
        @Nullable
        private String $private;

        @Nullable
        public String $private() {
            return $private;
        }

        @CustomType.Builder
        public static final class Builder {
            private final EscapedComplexType $;

            public Builder() {
                this.$ = new EscapedComplexType();
            }

            public Builder(EscapedComplexType defaults) {
                this.$ = defaults;
            }

            @Setter("$private")
            public void $private(@Nullable String $private) {
                this.$.$private = $private;
            }

            public EscapedComplexType build() {
                return this.$;
            }
        }
    }

    @Test
    void testComplexTypeTypeMismatches() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "ComplexTypeConverterTest#testComplexTypeTypeMismatches");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var deserializer = new Deserializer(inMemoryLog);
        var converter = new Converter(inMemoryLog, deserializer);
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("s", 24)
                .put("b", "hi")
                .put("i", "string")
                .put("d", true)
                .put("list", ImmutableList.of(false, 99, true, "hello"))
                .put("map", ImmutableMap.<String, Object>builder()
                        .put("k", 10)
                        .put("v", "hello")
                        .build())
                .put("private", "test")
                .put("size", "bigger")
                .put("color", true)
                .build()).join();

        var data = converter.convertValue(
                "ComplexTypeTypeMismatches", serialized, ComplexType1.class
        );

        assertThat(data.getValueNullable()).isNotNull();
        assertThat(data.getValueNullable().s).isEqualTo("");
        assertThat(data.getValueNullable().b).isEqualTo(false);
        assertThat(data.getValueNullable().i).isEqualTo(0);
        assertThat(data.getValueNullable().d).isEqualTo(0.0);
        assertThat(((ImmutableList) data.getValueNullable().list)).hasSameElementsAs(ImmutableList.of(false, false, true, false));
        assertThat(((ImmutableMap) data.getValueNullable().map)).containsAllEntriesOf(ImmutableMap.of("k", 10, "v", 0));
        assertThat(data.getValueNullable().$private).isEqualTo("test");
        assertThat(data.getValueNullable().size).isNull();
        assertThat(data.getValueNullable().color).isNull();

        assertThat(data.isKnown()).isTrue();

        assertThat(logger.getMessages()).hasSize(9);
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(s); Expected 'java.lang.String' but got 'java.lang.Double' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(b); Expected 'boolean' but got 'java.lang.String' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(i); Expected 'java.lang.Double' but got 'java.lang.String' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(d); Expected 'double' but got 'java.lang.Boolean' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(list)[1]; Expected 'java.lang.Boolean' but got 'java.lang.Double' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(list)[3]; Expected 'java.lang.Boolean' but got 'java.lang.String' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(map)[v]; Expected 'java.lang.Double' but got 'java.lang.String' while deserializing."
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(size); Expected value that match any of enum 'ContainerSize' constants: [FourInch, SixInch, EightInch], got: 'bigger'"
        ));
        assertThat(logger.getMessages()).haveExactly(1, containsString(
                "$ComplexType1(color); Expected value that match any of enum 'ContainerColor' constants: [ContainerColor{value=red}, ContainerColor{value=blue}, ContainerColor{value=yellow}], got: 'true'"
        ));
    }
}
