package io.pulumi.core.internal;

import io.pulumi.core.Either;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.annotations.InputImport;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.core.internal.annotations.InputMetadata;
import io.pulumi.core.internal.annotations.OutputMetadata;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InputOutputMetadataTest {

    @SuppressWarnings("unused")
    public static class Tester {

        @OutputExport(name = "complex1", type = Either.class, parameters = {Integer.class, String.class})
        public final Output<Either<Integer, String>> complex1 = Output.of(Either.ofLeft(1));

        @OutputExport(name = "complex2", type = Either.class, parameters = {Integer.class, String.class})
        public final Output<Either<Integer, String>> complex2 = Output.of(Either.ofRight("1"));

        @OutputExport(name = "foo", type = String.class)
        final Output<String> explicitFoo = Output.of("");

        @OutputExport(type = String.class)
        private final Output<String> implicitFoo = Output.of("");

        @OutputExport(type = Map.class, parameters = {String.class, Integer.class})
        public final Output<Map<String, Integer>> implicitBaz = Output.of(Map.of());

        @OutputExport(type = Double.class)
        private Output<Double> incomplete;

        @InputImport(name = "bar")
        public final Input<String> explicitBar = Input.of("");

        @InputImport
        final Input<String> implicitBar = Input.of("");

        @SuppressWarnings("DefaultAnnotationParam")
        @InputImport(name = "", required = true, json = true)
        public final Input<Map<String, Integer>> inputMap = Input.ofMap("k1", 1, "k2", 2);

        @SuppressWarnings("DefaultAnnotationParam")
        @InputImport(name = "", required = true, json = true)
        public final Input<Map<String, Integer>> inputMapNullsJson = Input.of(nullfulMap("k1", null, "k2", null));

        @SuppressWarnings("DefaultAnnotationParam")
        @InputImport(name = "", required = true, json = false)
        public final Input<Map<String, Integer>> inputMapNulls = Input.of(nullfulMap("k1", null, "k2", null));

        @InputImport
        public final Input<List<Boolean>> inputList = Input.ofList(true, false);

        @InputImport
        public final String inputless = "test";

        @InputImport
        public final Integer inputlessNull = null;

        @InputImport(json = true)
        public final Integer inputlessNullJson = null;
    }

    @Test
    void testInputInfos() {
        var tester = new Tester();
        var infos = InputMetadata.of(Tester.class);
        assertThat(infos).hasSize(9);

        var barInfo = infos.get("bar");
        assertThat(barInfo).isNotNull();
        assertThat(barInfo.getName()).isEqualTo("bar");
        assertThat(barInfo.getAnnotation().name()).isEqualTo("bar");
        assertThat(barInfo.getFieldName()).isEqualTo("explicitBar");
        assertThat(barInfo.getFieldType()).isAssignableFrom(tester.explicitBar.getClass());
        var bar = barInfo.getFieldValue(tester);
        assertThat(bar).isNotNull().isPresent();
        var barValue = Internal.of((Input<?>) bar.get()).getValueNullable().join();
        assertThat(barValue).isNotNull().isInstanceOf(String.class).isEqualTo("");

        var inputMapInfo = infos.get("inputMap");
        assertThat(inputMapInfo).isNotNull();
        assertThat(inputMapInfo.getName()).isEqualTo("inputMap");
        assertThat(inputMapInfo.getAnnotation().name()).isEqualTo("");
        assertThat(inputMapInfo.getFieldName()).isEqualTo("inputMap");
        assertThat(inputMapInfo.getFieldType()).isAssignableFrom(tester.inputMap.getClass());
        var inputMap = inputMapInfo.getFieldValue(tester);
        assertThat(inputMap).isNotNull().isPresent();
        var inputMapValue = Internal.of((Input<?>) inputMap.get()).getValueNullable().join();
        assertThat(inputMapValue).isNotNull().isInstanceOf(Map.class).isEqualTo(Map.of("k1", 1, "k2", 2));

        //noinspection OptionalGetWithoutIsPresent
        var inputMapNullsJson = infos.get("inputMapNullsJson").getFieldValue(tester).get();
        assertThat(Internal.of((Input<?>) inputMapNullsJson).getValueNullable().join()).isEqualTo(
                nullfulMap("k1", null, "k2", null)
        );

        //noinspection OptionalGetWithoutIsPresent
        var inputMapNulls = infos.get("inputMapNulls").getFieldValue(tester).get();
        assertThat(Internal.of((Input<?>) inputMapNulls).getValueNullable().join()).isEqualTo(
                nullfulMap("k1", null, "k2", null)
        );
    }

    @Test
    void testOutputInfos() {
        var tester = new Tester();
        var infos = OutputMetadata.of(Tester.class);
        assertThat(infos).hasSize(6);

        var fooInfo = infos.get("foo");
        assertThat(fooInfo).isNotNull();
        assertThat(fooInfo.getName()).isEqualTo("foo");
        assertThat(fooInfo.getAnnotation().name()).isEqualTo("foo");
        assertThat(fooInfo.getFieldName()).isEqualTo("explicitFoo");
        assertThat(fooInfo.getFieldType()).isAssignableFrom(tester.explicitFoo.getClass());
        var foo = fooInfo.getFieldValue(tester);
        assertThat(foo).isNotNull().isPresent();
        var barValue = Internal.of(foo.get()).getValueNullable().join();
        assertThat(barValue).isNotNull().isInstanceOf(String.class).isEqualTo("");

        var complex2Info = infos.get("complex2");
        assertThat(complex2Info).isNotNull();
        assertThat(complex2Info.getName()).isEqualTo("complex2");
        assertThat(complex2Info.getFieldType()).isAssignableFrom(tester.explicitFoo.getClass());
        assertThat(complex2Info.getDataShape().isAssignablePrimitiveFrom(TypeShape.either(Integer.class, String.class))).isTrue();
        var complex2 = complex2Info.getFieldValue(tester);
        assertThat(complex2).isNotNull().isPresent();
        var complex2Value = Internal.of(complex2.get()).getValueNullable().join();
        assertThat(complex2Value).isNotNull().isInstanceOf(Either.class).isEqualTo(Either.ofRight("1"));
    }

    @SuppressWarnings("SameParameterValue")
    private static <K, V> HashMap<K, V> nullfulMap(K k1, V v1, K k2, V v2) {
        var map = new HashMap<K, V>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}