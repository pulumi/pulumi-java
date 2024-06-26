package com.pulumi.core.internal;

import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.annotations.ExportMetadata;
import com.pulumi.core.internal.annotations.ImportMetadata;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportExportMetadataTest {

    @SuppressWarnings("unused")
    public static class Tester {

        @Export(name = "complex1", refs = {Either.class, Integer.class, String.class}, tree = "[0,1,2]")
        public final Output<Either<Integer, String>> complex1 = Output.of(Either.ofLeft(1));

        @Export(name = "complex2", refs = {Either.class, Integer.class, String.class}, tree = "[0,1,2]")
        public final Output<Either<Integer, String>> complex2 = Output.of(Either.ofRight("1"));

        @Export(name = "foo", refs = String.class)
        final Output<String> explicitFoo = Output.of("");

        @Export(refs = String.class)
        private final Output<String> implicitFoo = Output.of("");

        @Export(refs = {Map.class, String.class, Integer.class}, tree = "[0,1,2]")
        public final Output<Map<String, Integer>> implicitBaz = Output.of(Map.of());

        @Export(refs = Double.class)
        private Output<Double> incomplete;

        @Import(name = "bar")
        public final Output<String> explicitBar = Output.of("");

        @Import
        final Output<String> implicitBar = Output.of("");

        @SuppressWarnings("DefaultAnnotationParam")
        @Import(name = "", required = true, json = true)
        public final Output<Map<String, Integer>> inputMap = Output.ofMap("k1", 1, "k2", 2);

        @SuppressWarnings("DefaultAnnotationParam")
        @Import(name = "", required = true, json = true)
        public final Output<Map<String, Integer>> inputMapNullsJson = Output.of(nullfulMap("k1", null, "k2", null));

        @SuppressWarnings("DefaultAnnotationParam")
        @Import(name = "", required = true, json = false)
        public final Output<Map<String, Integer>> inputMapNulls = Output.of(nullfulMap("k1", null, "k2", null));

        @Import
        public final Output<List<Boolean>> inputList = Output.ofList(true, false);

        @Import
        public final String inputless = "test";

        @Import
        public final Integer inputlessNull = null;

        @Import(json = true)
        public final Integer inputlessNullJson = null;
    }

    @Test
    void testImportInfos() {
        var tester = new Tester();
        var infos = ImportMetadata.of(Tester.class);
        assertThat(infos).hasSize(9);

        var barInfo = infos.get("bar");
        assertThat(barInfo).isNotNull();
        assertThat(barInfo.getName()).isEqualTo("bar");
        assertThat(barInfo.getAnnotation().name()).isEqualTo("bar");
        assertThat(barInfo.getFieldName()).isEqualTo("explicitBar");
        assertThat(barInfo.getFieldType()).isAssignableFrom(tester.explicitBar.getClass());
        var bar = barInfo.getFieldValue(tester);
        assertThat(bar).isNotNull().isPresent();
        var barValue = Internal.of((Output<?>) bar.get()).getValueNullable().join();
        assertThat(barValue).isNotNull().isInstanceOf(String.class).isEqualTo("");

        var inputMapInfo = infos.get("inputMap");
        assertThat(inputMapInfo).isNotNull();
        assertThat(inputMapInfo.getName()).isEqualTo("inputMap");
        assertThat(inputMapInfo.getAnnotation().name()).isEqualTo("");
        assertThat(inputMapInfo.getFieldName()).isEqualTo("inputMap");
        assertThat(inputMapInfo.getFieldType()).isAssignableFrom(tester.inputMap.getClass());
        var inputMap = inputMapInfo.getFieldValue(tester);
        assertThat(inputMap).isNotNull().isPresent();
        var inputMapValue = Internal.of((Output<?>) inputMap.get()).getValueNullable().join();
        assertThat(inputMapValue).isNotNull().isInstanceOf(Map.class).isEqualTo(Map.of("k1", 1, "k2", 2));

        //noinspection OptionalGetWithoutIsPresent
        var inputMapNullsJson = infos.get("inputMapNullsJson").getFieldValue(tester).get();
        assertThat(Internal.of((Output<?>) inputMapNullsJson).getValueNullable().join()).isEqualTo(
                nullfulMap("k1", null, "k2", null)
        );

        //noinspection OptionalGetWithoutIsPresent
        var inputMapNulls = infos.get("inputMapNulls").getFieldValue(tester).get();
        assertThat(Internal.of((Output<?>) inputMapNulls).getValueNullable().join()).isEqualTo(
                nullfulMap("k1", null, "k2", null)
        );
    }

    @Test
    void testExportInfos() {
        var tester = new Tester();
        var infos = ExportMetadata.of(Tester.class);
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
        assertThat(complex2Info.getDataShape().isAssignableFrom(TypeShape.either(Integer.class, String.class))).isTrue();
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