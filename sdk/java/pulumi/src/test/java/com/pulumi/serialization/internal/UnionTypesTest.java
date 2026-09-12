package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.ConstValue;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.UnionType;
import com.pulumi.core.internal.UnionCase;
import com.pulumi.serialization.internal.ConverterTests.ContainerColor;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnionTypesTest {

    private final static Log log = PulumiTestInternal.mockLog();

    // A union of two objects that differ only in a constant, a string, a list of strings, and an
    // enum with numeric literals. Every fully-known wire value fits exactly one member.
    @UnionType
    @UnionType.Case(type = VariantOne.class)
    @UnionType.Case(type = VariantTwo.class)
    @UnionType.Case(type = Shape.OfString.class, refs = {String.class}, tree = "[0]")
    @UnionType.Case(type = Shape.OfList.class, refs = {List.class, String.class}, tree = "[0,1]")
    @UnionType.Case(type = Shape.OfSize.class, refs = {ConverterTests.ContainerSize.class}, tree = "[0]")
    public interface Shape {
        static Shape of(String value) {
            return new OfString(value);
        }

        static Shape of(List<String> value) {
            return new OfList(value);
        }

        static Shape of(ConverterTests.ContainerSize value) {
            return new OfSize(value);
        }

        final class OfString implements Shape, UnionCase {
            private final String value;

            private OfString(String value) {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public String value() {
                return value;
            }
        }

        final class OfList implements Shape, UnionCase {
            private final List<String> value;

            private OfList(List<String> value) {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public List<String> value() {
                return value;
            }
        }

        final class OfSize implements Shape, UnionCase {
            private final ConverterTests.ContainerSize value;

            private OfSize(ConverterTests.ContainerSize value) {
                this.value = Objects.requireNonNull(value);
            }

            @Override
            public ConverterTests.ContainerSize value() {
                return value;
            }
        }
    }

    @CustomType
    public static final class VariantOne implements Shape {
        private String kind;
        private @Nullable String field1;

        private VariantOne() {
        }

        @CustomType.Builder
        public static final class Builder {
            private final VariantOne $ = new VariantOne();

            @CustomType.Setter
            @ConstValue("one")
            public Builder kind(String kind) {
                $.kind = kind;
                return this;
            }

            @CustomType.Setter
            public Builder field1(@Nullable String field1) {
                $.field1 = field1;
                return this;
            }

            public VariantOne build() {
                return $;
            }
        }
    }

    @CustomType
    public static final class VariantTwo implements Shape {
        private String kind;
        private @Nullable String field1;

        private VariantTwo() {
        }

        @CustomType.Builder
        public static final class Builder {
            private final VariantTwo $ = new VariantTwo();

            @CustomType.Setter
            @ConstValue("two")
            public Builder kind(String kind) {
                $.kind = kind;
                return this;
            }

            @CustomType.Setter
            public Builder field1(@Nullable String field1) {
                $.field1 = field1;
                return this;
            }

            public VariantTwo build() {
                return $;
            }
        }
    }

    // Two objects with the same properties and no constant to tell them apart.
    @UnionType
    @UnionType.Case(type = Ambiguous.Left.class)
    @UnionType.Case(type = Ambiguous.Right.class)
    public interface Ambiguous {
        @CustomType
        final class Left implements Ambiguous {
            private @Nullable String s;

            @CustomType.Builder
            public static final class Builder {
                private final Left $ = new Left();

                @CustomType.Setter
                public Builder s(@Nullable String s) {
                    $.s = s;
                    return this;
                }

                public Left build() {
                    return $;
                }
            }
        }

        @CustomType
        final class Right implements Ambiguous {
            private @Nullable String s;

            @CustomType.Builder
            public static final class Builder {
                private final Right $ = new Right();

                @CustomType.Setter
                public Builder s(@Nullable String s) {
                    $.s = s;
                    return this;
                }

                public Right build() {
                    return $;
                }
            }
        }
    }

    private static Object convert(Object wire, Class<?> target) {
        return convert(wire, TypeShape.of(target));
    }

    private static Object convert(Object wire, TypeShape<?> target) {
        var converter = new Converter(log, new Deserializer(log));
        Value serialized = serializeToValueAsync(wire).join();
        return converter.convertValue("UnionTypesTest", serialized, target).getValueNullable();
    }

    @Test
    void matchesByWireShape() {
        assertThat(UnionTypes.matches("s", TypeShape.of(String.class))).isTrue();
        assertThat(UnionTypes.matches(1.0, TypeShape.of(String.class))).isFalse();
        assertThat(UnionTypes.matches(1.0, TypeShape.of(Integer.class))).isTrue();
        assertThat(UnionTypes.matches(true, TypeShape.of(Double.class))).isFalse();
        assertThat(UnionTypes.matches(true, TypeShape.of(Boolean.class))).isTrue();
        assertThat(UnionTypes.matches(List.of("a"), TypeShape.list(String.class))).isTrue();
        assertThat(UnionTypes.matches(List.of(1.0), TypeShape.list(String.class))).isFalse();
        assertThat(UnionTypes.matches(Map.of("k", "v"), TypeShape.map(String.class, String.class))).isTrue();
        assertThat(UnionTypes.matches(Map.of("k", 1.0), TypeShape.map(String.class, String.class))).isFalse();
        assertThat(UnionTypes.matches("blue", TypeShape.of(ContainerColor.class))).isTrue();
        assertThat(UnionTypes.matches("green", TypeShape.of(ContainerColor.class))).isFalse();
        assertThat(UnionTypes.matches(6.0, TypeShape.of(ConverterTests.ContainerSize.class))).isTrue();
        assertThat(UnionTypes.matches(7.0, TypeShape.of(ConverterTests.ContainerSize.class))).isFalse();
        assertThat(UnionTypes.matches(null, TypeShape.of(String.class))).isFalse();
        assertThat(UnionTypes.matches(null, TypeShape.optional(String.class))).isTrue();
        assertThat(UnionTypes.matches(UnionTypes.UNKNOWN, TypeShape.of(String.class))).isTrue();
    }

    @Test
    void matchesObjectsUnderTheClosedReading() {
        var one = TypeShape.of(VariantOne.class);
        assertThat(UnionTypes.matches(Map.of("kind", "one", "field1", "x"), one)).isTrue();
        assertThat(UnionTypes.matches(Map.of("kind", "one"), one)).isTrue();
        // Wrong constant.
        assertThat(UnionTypes.matches(Map.of("kind", "two", "field1", "x"), one)).isFalse();
        // Missing required property.
        assertThat(UnionTypes.matches(Map.of("field1", "x"), one)).isFalse();
        // Undeclared property.
        assertThat(UnionTypes.matches(Map.of("kind", "one", "extra", "x"), one)).isFalse();
        // Wrong property type.
        assertThat(UnionTypes.matches(Map.of("kind", "one", "field1", 1.0), one)).isFalse();
        assertThat(UnionTypes.matches("one", one)).isFalse();
    }

    @Test
    void selectFindsTheOneMemberThatFits() {
        assertThat(UnionTypes.select(Shape.class, Map.of("kind", "two")).right().type()).isEqualTo(VariantTwo.class);
        assertThat(UnionTypes.select(Shape.class, "s").right().type()).isEqualTo(Shape.OfString.class);
        assertThat(UnionTypes.select(Shape.class, List.of()).right().type()).isEqualTo(Shape.OfList.class);
        assertThat(UnionTypes.select(Shape.class, 6.0).right().type()).isEqualTo(Shape.OfSize.class);

        assertThat(UnionTypes.select(Shape.class, true).left()).isEqualTo(
                "Expected a value that fits one member of '" + Shape.class.getTypeName() + "', got a Boolean " +
                        "that fits none of: " + VariantOne.class.getTypeName() + ", " + VariantTwo.class.getTypeName() +
                        ", OfString(java.lang.String), OfList(java.util.List<java.lang.String>), " +
                        "OfSize(com.pulumi.serialization.internal.ConverterTests$ContainerSize)");
        assertThat(UnionTypes.select(Ambiguous.class, Map.of("s", "x")).left()).isEqualTo(
                "Expected a value that fits one member of '" + Ambiguous.class.getTypeName() + "', " +
                        "got an object with keys [s] that fits several of: " +
                        Ambiguous.Left.class.getTypeName() + ", " + Ambiguous.Right.class.getTypeName());
    }

    @Test
    void converterProducesTheMemberOrItsCase() {
        var two = convert(ImmutableMap.of("kind", "two", "field1", "x"), Shape.class);
        assertThat(two).isInstanceOf(VariantTwo.class);
        assertThat(((VariantTwo) two).kind).isEqualTo("two");
        assertThat(((VariantTwo) two).field1).isEqualTo("x");

        var string = convert("hello", Shape.class);
        assertThat(string).isInstanceOf(Shape.OfString.class);
        assertThat(((Shape.OfString) string).value()).isEqualTo("hello");

        var list = convert(ImmutableList.of("a", "b"), Shape.class);
        assertThat(list).isInstanceOf(Shape.OfList.class);
        assertThat(((Shape.OfList) list).value()).isEqualTo(List.of("a", "b"));

        var size = convert(6, Shape.class);
        assertThat(size).isInstanceOf(Shape.OfSize.class);
        assertThat(((Shape.OfSize) size).value()).isEqualTo(ConverterTests.ContainerSize.SixInch);

        var nested = (List<?>) convert(ImmutableList.of("hello", ImmutableMap.of("kind", "one")),
                TypeShape.list(Shape.class));
        assertThat(nested.get(0)).isInstanceOf(Shape.OfString.class);
        assertThat(nested.get(1)).isInstanceOf(VariantOne.class);
    }

    @Test
    void converterRejectsAValueThatFitsNoMember() {
        assertThatThrownBy(() -> convert(ImmutableMap.of("kind", "three"), Shape.class))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("UnionTypesTest; Expected a value that fits one member of")
                .hasMessageContaining("got an object with keys [kind] that fits none of");
    }

    @Test
    void serializerUnwrapsACase() {
        assertThat(serializeToValueAsync(Shape.of("hello")).join())
                .isEqualTo(serializeToValueAsync("hello").join());
        assertThat(serializeToValueAsync(Shape.of(List.of("a"))).join())
                .isEqualTo(serializeToValueAsync(List.of("a")).join());
        assertThat(serializeToValueAsync(Shape.of(ConverterTests.ContainerSize.SixInch)).join())
                .isEqualTo(serializeToValueAsync(6).join());
    }
}
