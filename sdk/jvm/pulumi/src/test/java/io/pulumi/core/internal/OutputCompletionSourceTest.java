package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.annotations.Export;
import io.pulumi.core.internal.annotations.OutputMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.assertj.core.api.Assertions.assertThat;

class OutputCompletionSourceTest {

    @SuppressWarnings("unused")
    private static class Tester {
        @Export(type = String.class)
        Output<String> foo;

        @Export(type = Either.class, parameters = {Integer.class, String.class})
        private Output<Either<Integer, String>> complex1;
    }

    @Test
    void testConvertingSetValue() {
        var tester = new Tester();
        var infos = OutputMetadata.of(Tester.class);

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
            OutputMetadata<T> metadata,
            E extractionObject
    ) {
        var shape = metadata.getDataShape();
        var output = metadata.getOrSetIncompleteFieldValue(extractionObject);
        return OutputCompletionSource.of(output, ImmutableSet.of(), shape);
    }
}