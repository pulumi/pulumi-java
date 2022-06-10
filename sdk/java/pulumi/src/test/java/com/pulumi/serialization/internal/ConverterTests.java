package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Constructor;
import com.pulumi.core.annotations.CustomType.Parameter;
import com.pulumi.core.annotations.EnumType;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.Constants;
import com.pulumi.deployment.MockDeployment;
import com.pulumi.deployment.MocksTest;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ConverterTests {

    private final static Log log = DeploymentTests.mockLog();

    private static Value createSecretValue(Value value) {
        return Value.newBuilder().setStructValue(
                Struct.newBuilder()
                        .putFields(Constants.SpecialSigKey, Value.newBuilder().setStringValue(Constants.SpecialSecretSig).build())
                        .putFields(Constants.SecretValueName, value)
                        .build()
        ).build();
    }

    @CheckReturnValue
    public static CompletableFuture<Value> serializeToValueAsync(@Nullable Object value) {
        return serializeToValueAsync(value, true);
    }

    @CheckReturnValue
    public static CompletableFuture<Value> serializeToValueAsync(@Nullable Object value, boolean keepResources) {
        var serializer = new Serializer(log);
        return serializer.serializeAsync("ConverterTest", value, keepResources)
                .thenApply(Serializer::createValue);
    }

    @SuppressWarnings("unused")
    @CheckReturnValue
    public static <T> T deserializeFromValue(Value value, Class<T> ignored) {
        var deserializer = new Deserializer(log);
        //noinspection unchecked
        return (T) deserializer.deserialize(value).getValueNullable();
    }

    @Nested
    class ArgsConverterTest {
        public class SimpleInvokeArgs1 extends InvokeArgs {
            @Import(name = "s")
            @Nullable
            public final String s;

            public SimpleInvokeArgs1(@Nullable String s) {
                this.s = s;
            }
        }

        public class ComplexInvokeArgs1 extends InvokeArgs {
            @Import(name = "v")
            @Nullable
            public final SimpleInvokeArgs1 v;

            public ComplexInvokeArgs1(@Nullable SimpleInvokeArgs1 v) {
                this.v = v;
            }
        }

        public class SimpleResourceArgs1 extends ResourceArgs {
            @Import(name = "s")
            @Nullable
            public final Output<String> s;

            public SimpleResourceArgs1(@Nullable Output<String> s) {
                this.s = s;
            }
        }

        public class ComplexResourceArgs1 extends ResourceArgs {
            @Import(name = "v")
            @Nullable
            public final Output<SimpleResourceArgs1> v;

            public ComplexResourceArgs1(@Nullable Output<SimpleResourceArgs1> v) {
                this.v = v;
            }
        }

        @Test
        void testInvokeArgs() {
            var deserializer = new Deserializer(log);
            var gson = new Gson();

            var args = new ComplexInvokeArgs1(new SimpleInvokeArgs1("value1"));
            var converter = new Converter(log, deserializer);
            serializeToValueAsync(args)
                    .thenApply(value -> converter.convertValue("ArgsConverterTests", value, JsonElement.class))
                    .thenAccept(data ->
                            assertThat(data.getValueNullable())
                                    .isEqualTo(gson.fromJson("{\"v\"={\"s\"=\"value1\"}}", JsonElement.class))
                    ).join();
        }

        @Test
        void testResourceArgs() {
            var deserializer = new Deserializer(log);
            var gson = new Gson();
            var args = new ComplexResourceArgs1(Output.of(new SimpleResourceArgs1(Output.of("value2"))));

            var converter = new Converter(log, deserializer);
            serializeToValueAsync(args)
                    .thenApply(value -> converter.convertValue("ArgsConverterTests", value, JsonElement.class))
                    .thenAccept(data ->
                            assertThat(data.getValueNullable())
                                    .isEqualTo(gson.fromJson("{\"v\"={\"s\"=\"value2\"}}", JsonElement.class))
                    ).join();
        }
    }

    @Nested
    class BooleanConverterTest {

        @AfterEach
        void cleanup() {
            PulumiTestInternal.cleanup();
        }

        @Test
        void testTrue() {
            var deserializer = new Deserializer(log);

            var trueValue = Value.newBuilder().setBoolValue(true).build();
            var converter = new Converter(log, deserializer);
            var data = converter.convertValue(
                    "BooleanConverterTests", trueValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isTrue();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testFalse() {
            var deserializer = new Deserializer(log);

            var falseValue = Value.newBuilder().setBoolValue(false).build();
            var converter = new Converter(log, deserializer);
            var data = converter.convertValue(
                    "BooleanConverterTests", falseValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isFalse();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testTrueSecret() {
            var deserializer = new Deserializer(log);
            var secretValue = createSecretValue(Value.newBuilder().setBoolValue(true).build());
            var converter = new Converter(log, deserializer);
            var data = converter.convertValue(
                    "BooleanConverterTests", secretValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isTrue();
            assertThat(data.isKnown()).isTrue();
            assertThat(data.isSecret()).isTrue();
        }

        @Test
        void testFalseSecret() {
            var deserializer = new Deserializer(log);
            var secretValue = createSecretValue(Value.newBuilder().setBoolValue(false).build());
            var converter = new Converter(log, deserializer);
            var data = converter.convertValue(
                    "BooleanConverterTests", secretValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isFalse();
            assertThat(data.isKnown()).isTrue();
            assertThat(data.isSecret()).isTrue();
        }

        @Test
        void testNonBooleanThrows() {
            var deserializer = new Deserializer(log);
            var wrongValue = Value.newBuilder().setStringValue("").build();
            var converter = new Converter(log, deserializer);
            assertThatThrownBy(
                    () -> converter.convertValue("BooleanConverterTests", wrongValue, Boolean.class)
            ).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Expected 'java.lang.Boolean' but got 'java.lang.String' while deserializing");
        }

        @Test
        void testNullInPreviewProducesFalseKnown() {
            PulumiTestInternal.builder()
                    .mocks(new MocksTest.MyMocks())
                    .options(TestOptions.builder().preview(true).build())
                    .deploymentFactory(MockDeployment::new)
                    .build();

            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var nullValue = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", nullValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isFalse();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testNullInNormalProducesFalseKnown() {
            PulumiTestInternal.builder()
                    .mocks(new MocksTest.MyMocks())
                    .options(TestOptions.builder().preview(false).build())
                    .deploymentFactory(MockDeployment::new)
                    .build();

            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var nullValue = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", nullValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isFalse();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testUnknownValueDeserializesCorrectly() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var unknownValue = Value.newBuilder().setStringValue(Constants.UnknownValue).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", unknownValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNull();
            assertThat(data.isKnown()).isFalse();
        }

        @Test
        void testEmptyStringThrows() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var wrongValue = Value.newBuilder().setStringValue("").build();
            assertThatThrownBy(
                    () -> converter.convertValue("BooleanConverterTests", wrongValue, Boolean.class)
            ).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Expected 'java.lang.Boolean' but got 'java.lang.String' while deserializing");

        }

        @Test
        void testOptionalTrueWrapped() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var trueValue = Value.newBuilder().setBoolValue(true).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", trueValue, TypeShape.optional(Boolean.class)
            );
            assertThat(data.getValueNullable()).isNotNull().isPresent().contains(true);
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testOptionalTrueUnwrapped() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var trueValue = Value.newBuilder().setBoolValue(true).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", trueValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isTrue();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testOptionalFalseWrapped() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var trueValue = Value.newBuilder().setBoolValue(false).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", trueValue, TypeShape.optional(Boolean.class)
            );
            assertThat(data.getValueNullable()).isNotNull().isPresent().contains(false);
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testOptionalFalseUnwrapped() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var trueValue = Value.newBuilder().setBoolValue(false).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", trueValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isNotNull().isFalse();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testNullToOptionalEmpty() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var nullValue = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", nullValue, TypeShape.optional(Boolean.class)
            );
            assertThat(data.getValueNullable()).isNotNull().isEmpty();
            assertThat(data.isKnown()).isTrue();
        }

        @Test
        void testNullToNullUnwrapped() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var nullValue = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            var data = converter.convertValue(
                    "BooleanConverterTests", nullValue, Boolean.class
            );
            assertThat(data.getValueNullable()).isFalse();
            assertThat(data.isKnown()).isTrue();
        }
    }

    @Nested
    class EnumConverterTest {

        @ParameterizedTest
        @EnumSource(ContainerSize.class)
        void testCustomIntEnum(ContainerSize input) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            serializeToValueAsync(input)
                    .thenApply(value -> converter.convertValue("EnumConverterTests", value, ContainerSize.class))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNotNull();
                        assertThat(input.asDouble()).isEqualTo(data.getValueNullable().asDouble());
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }

        @ParameterizedTest
        @EnumSource(JarSize.class)
        void testStandardIntEnum(JarSize input) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            serializeToValueAsync(input)
                    .thenApply(value -> converter.convertValue("EnumConverterTests", value, JarSize.class))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNotNull();
                        assertThat(input.asDouble()).isEqualTo(data.getValueNullable().asDouble());
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }

        @ParameterizedTest
        @EnumSource(ContainerColor.class)
        void testCustomStringEnum(ContainerColor input) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            serializeToValueAsync(input)
                    .thenApply(value -> converter.convertValue("EnumConverterTests", value, ContainerColor.class))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNotNull();
                        assertThat(input.getValue()).isEqualTo(data.getValueNullable().getValue());
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }

        @ParameterizedTest
        @EnumSource(ContainerBrightness.class)
        void testCustomDoubleEnum(ContainerBrightness input) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            serializeToValueAsync(input)
                    .thenApply(value -> converter.convertValue("EnumConverterTests", value, ContainerBrightness.class))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNotNull();
                        assertThat(input.getValue()).isEqualTo(data.getValueNullable().getValue());
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }


        @ParameterizedTest
        @MethodSource("com.pulumi.serialization.internal.ConverterTests#testSerializingUnserializableEnumsThrows")
        void testSerializingUnserializableEnumsThrows(Enum<?> input) {
            //noinspection ResultOfMethodCallIgnored
            assertThatThrownBy(() -> serializeToValueAsync(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("to have exactly one method annotated with @Converter");
        }

        @ParameterizedTest
        @MethodSource("com.pulumi.serialization.internal.ConverterTests#testConvertingNonconvertibleValuesThrows")
        void testConvertingNonconvertibleValuesThrows(Class<?> targetType, Value value) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            assertThatThrownBy(() -> converter.convertValue("EnumConverterTests", value, targetType))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Expected value that match any of enum");
        }
    }

    @Nested
    class EitherConverterTest {

        @Test
        void testLeft() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var data = converter.convertValue(
                    "EitherConverterTests", Value.newBuilder().setNumberValue(1).build(), TypeShape.either(Integer.class, String.class));
            assertThat(data.isKnown()).isTrue();
            assertThat(data.getValueNullable()).isNotNull();
            assertThat(data.getValueNullable().isLeft()).isTrue();
            assertThat(data.getValueNullable().left()).isEqualTo(1);
        }

        @Test
        void testRight() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var value = Value.newBuilder().setStringValue("foo").build();
            var data = converter.convertValue(
                    "EitherConverterTests", value, TypeShape.either(Integer.class, String.class));
            assertThat(data.isKnown()).isTrue();
            assertThat(data.getValueNullable()).isNotNull();
            assertThat(data.getValueNullable().isRight()).isTrue();
            assertThat(data.getValueNullable().right()).isEqualTo("foo");
        }
    }

    @Nested
    class InternalPropertyTest {

        @Test
        public void ignoreInternalProperty() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var value = Value.newBuilder().setStructValue(
                    Struct.newBuilder()
                            .putFields("a", Value.newBuilder().setStringValue("b").build())
                            .putFields("__defaults", Value.newBuilder().setBoolValue(true).build())
                            .build()
            ).build();
            var shape = TypeShape.builder(ImmutableMap.class)
                    .addParameter(String.class)
                    .addParameter(String.class)
                    .build();
            var data = converter.convertValue("InternalPropertyTests", value, shape);
            assertThat(data.isKnown()).isTrue();
            //noinspection unchecked
            assertThat(data.getValueNullable()).isNotNull();
            //noinspection unchecked
            assertThat(data.getValueNullable()).containsKey("a");
            assertThat(data.getValueNullable().get("a")).isEqualTo("b");
            //noinspection unchecked
            assertThat(data.getValueNullable()).doesNotContainKey("__defaults");
        }
    }

    @Nested
    class JsonConverterTest {

        private final Gson gson = new Gson();

        @ParameterizedTest
        @MethodSource("com.pulumi.serialization.internal.ConverterTests#testJsons")
        void testJsons(String json, String expected) {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var element = gson.fromJson(json, JsonElement.class);
            serializeToValueAsync(element)
                    .thenAccept(serialized -> {
                        var converted = converter.convertValue("JsonConverterTests", serialized, JsonElement.class);
                        assertThat(converted.getValueNullable()).isNotNull();
                        assertThat(converted.getValueNullable().toString()).isEqualTo(expected);
                    })
                    .join();
        }
    }

    @Nested
    class ListConverterTest {

        @Test
        void testEmptyList() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            List<Boolean> emptyList = List.of();
            serializeToValueAsync(emptyList)
                    .thenApply(value -> converter.convertValue("ListConverterTests", value, TypeShape.list(Boolean.class)))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNotNull().isEmpty();
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }

        @Test
        void testListWithElement() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var listWithElement = List.of(true);
            serializeToValueAsync(listWithElement)
                    .thenApply(value -> converter.convertValue("ListConverterTests", value, TypeShape.list(Boolean.class)))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable())
                                .isNotNull()
                                .hasSize(1)
                                .containsOnly(true);
                        assertThat(data.isKnown()).isTrue();
                    }).join();
        }

        @Test
        void testSecretListWithElement() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var secretListWithElement = Output.ofSecret(List.of(true));
            serializeToValueAsync(secretListWithElement)
                    .thenApply(value -> converter.convertValue("ListConverterTests", value, TypeShape.list(Boolean.class)))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable())
                                .isNotNull()
                                .hasSize(1)
                                .containsOnly(true);
                        assertThat(data.isKnown()).isTrue();
                        assertThat(data.isSecret()).isTrue();
                    }).join();
        }


        @Test
        void testListWithSecretElement() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var listWithSecretElement = List.of(Output.ofSecret(true));
            serializeToValueAsync(listWithSecretElement)
                    .thenApply(value -> converter.convertValue("ListConverterTests", value, TypeShape.list(Boolean.class)))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable())
                                .isNotNull()
                                .hasSize(1)
                                .containsOnly(true);
                        assertThat(data.isKnown()).isTrue();
                        assertThat(data.isSecret()).isTrue();
                    }).join();
        }

        @Test
        void testListWithUnknownElement() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var listWithUnknownElement = List.of(OutputTests.unknown());
            serializeToValueAsync(listWithUnknownElement)
                    .thenApply(value -> converter.convertValue("ListConverterTests", value, TypeShape.list(Boolean.class)))
                    .thenAccept(data -> {
                        assertThat(data.getValueNullable()).isNull();
                        assertThat(data.isKnown()).isFalse();
                        assertThat(data.isSecret()).isFalse();
                    }).join();
        }

        @Test
        void testStructWithUnknowns() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var value = Value.newBuilder().setStructValue(Struct.newBuilder()
                    .putFields("myString", Value.newBuilder().setStringValue("foo").build())
                    .putFields("myBoolean", Value.newBuilder().setStringValue(Constants.UnknownValue).build())
                    .build()).build();
            var data = converter.convertValue("test", value, SimpleStruct.class);
            assertThat(data.isKnown()).isFalse();
            assertThat(data.getValueNullable()).isNull();
        }
    }

    @Nested
    class RecursiveTypeTest {

        @Test
        void testSimpleCase() {
            var deserializer = new Deserializer(log);
            var converter = new Converter(log, deserializer);
            var value = Value.newBuilder()
                    .setStructValue(
                            Struct.newBuilder()
                                    .putFields("ref", Value.newBuilder().setStringValue("a").build())
                                    .putFields("additionalItems", Value.newBuilder()
                                            .setListValue(
                                                    ListValue.newBuilder()
                                                            .addValues(
                                                                    Value.newBuilder()
                                                                            .setStructValue(
                                                                                    Struct.newBuilder()
                                                                                            .putFields("ref", Value.newBuilder().setStringValue("b").build())
                                                                                            .putFields("additionalItems", Value.newBuilder().setListValue(ListValue.newBuilder().build()).build())
                                                                                            .build()
                                                                            ).build()
                                                            ).build()
                                            ).build()
                                    ).build()
                    ).build();
            var data = converter.convertValue("", value, RecursiveType.class);

            assertThat(data.isKnown()).isTrue();
            assertThat(data.getValueNullable()).isNotNull();
            assertThat(data.getValueNullable().ref).isEqualTo("a");
            assertThat(data.getValueNullable().additionalItems).hasSize(1);
            assertThat(data.getValueNullable().additionalItems.get(0).ref).isEqualTo("b");
            assertThat(data.getValueNullable().additionalItems.get(0).additionalItems).isEmpty();
        }
    }

    // Test types -----

    @EnumType
    public enum JarSize {
        Small, Medium, Big;

        @EnumType.Converter
        private double asDouble() {
            return ordinal();
        }
    }

    @EnumType
    public enum ContainerSize {
        FourInch(4),
        SixInch(6),
        EightInch(8);

        public final int length;

        @EnumType.Converter
        private double asDouble() {
            return length;
        }

        ContainerSize(int length) {
            this.length = length;
        }
    }

    @EnumType
    public enum ContainerColor {
        Red("red"),
        Blue("blue"),
        Yellow("yellow");

        private final String value;

        ContainerColor(String value) {
            this.value = Objects.requireNonNull(value);
        }

        @EnumType.Converter
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("value", value)
                    .toString();
        }
    }

    @EnumType
    public enum ContainerBrightness {
        One(1.0),
        ZeroPointOne(0.1);

        private final double value;

        ContainerBrightness(double value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("value", value)
                    .toString();
        }

        @EnumType.Converter
        public double getValue() {
            return value;
        }
    }

    @EnumType
    public enum Unserializable1 {
        One(new Object());

        public final Object o;

        Unserializable1(Object o) {
            this.o = o;
        }
    }

    @EnumType
    public enum Unserializable2 {
        OneTwo("1", "2");

        public final String s1;
        public final String s2;

        Unserializable2(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
    }

    @CustomType
    public static class RecursiveType {
        public final String ref;
        public final ImmutableList<RecursiveType> additionalItems;

        @Constructor
        public RecursiveType(
                @Parameter("ref") String ref,
                @Parameter("additionalItems") ImmutableList<RecursiveType> additionalItems
        ) {
            this.ref = ref;
            this.additionalItems = additionalItems;
        }
    }

    @CustomType
    public static class SimpleStruct {
        private final @Nullable
        String myString;

        private final @Nullable
        Boolean myBoolean;

        @CustomType.Constructor
        private SimpleStruct(
                @CustomType.Parameter("myString") @Nullable String myString,
                @CustomType.Parameter("myBoolean") @Nullable Boolean myBoolean) {
            this.myString = myString;
            this.myBoolean = myBoolean;
        }
    }

    // Test data sources -----

    @SuppressWarnings("unused")
    private static Stream<Arguments> testSerializingUnserializableEnumsThrows() {
        return Stream.of(
                arguments(Unserializable1.One),
                arguments(Unserializable2.OneTwo)
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testConvertingNonconvertibleValuesThrows() {
        return Stream.of(
                arguments(ContainerColor.class, Value.newBuilder().setNumberValue(1.0).build()),
                arguments(ContainerBrightness.class, Value.newBuilder().setStringValue("hello").build()),
                arguments(ContainerSize.class, Value.newBuilder().setStringValue("hello").build())
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testJsons() {
        return Stream.of(
                arguments("null", "null"),
                arguments("", "null"),
                arguments("\"x\"", "\"x\""),
                arguments("1.1", "1.1"),
                arguments("true", "true"),
                arguments("null", "null"),
                arguments("[1,true,null]", "[1.0,true,null]"),
                arguments("{\"n\":1}", "{\"n\":1.0}"),
                arguments("{\"n\":[1,true,null],\"m\":null}", "{\"n\":[1.0,true,null]}")
        );
    }
}
