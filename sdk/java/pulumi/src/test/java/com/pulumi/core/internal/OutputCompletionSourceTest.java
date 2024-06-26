package com.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import com.pulumi.core.Either;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.annotations.ExportMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.assertj.core.api.Assertions.assertThat;

class OutputCompletionSourceTest {

    @SuppressWarnings("unused")
    private static class Tester {
        @Export(refs = {String.class})
        Output<String> foo;

        @Export(refs = {Either.class, Integer.class, String.class}, tree = "[0,1,2]")
        private Output<Either<Integer, String>> complex1;
    }

    @Test
    void testConvertingSetValue() {
        var tester = new Tester();
        var infos = ExportMetadata.of(Tester.class);

        var sources = infos.entrySet().stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> of(entry.getValue(), tester)
                ));

        var expectedFoo = "expected";
        OutputCompletionSource<?> sourceFoo = sources.get("foo");
        assertThat(sourceFoo.mutableData).isNotCompleted();
        sourceFoo.setStringValue(expectedFoo, true);
        assertThat(sourceFoo.mutableData)
                .isCompletedWithValueMatching(d -> Objects.equals(d.getValueNullable(), expectedFoo));

        var expectedComplex1 = Either.ofLeft(1);
        OutputCompletionSource<?> sourceComplex1 = sources.get("complex1");
        assertThat(sourceComplex1.mutableData).isNotCompleted();
        sourceComplex1.setObjectValue(expectedComplex1, TypeShape.either(Integer.class, String.class), true);
        assertThat(sourceComplex1.mutableData)
                .isCompletedWithValueMatching(d -> Objects.equals(d.getValueNullable(), expectedComplex1));
    }

    private static <T, E> OutputCompletionSource<T> of(
            ExportMetadata<T> metadata,
            E extractionObject
    ) {
        var shape = metadata.getDataShape();
        var output = metadata.getOrSetIncompleteFieldValue(extractionObject);
        return OutputCompletionSource.of(output, ImmutableSet.of(), shape);
    }
}