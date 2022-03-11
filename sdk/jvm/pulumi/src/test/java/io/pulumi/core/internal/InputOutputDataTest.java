package io.pulumi.core.internal;

import io.pulumi.core.Output;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputOutputDataTest {

    @Test
    void testHashCodeEqualsContract() {
        assertThat(InputOutputData.empty()).isEqualTo(InputOutputData.empty());
        assertThat(InputOutputData.empty()).isNotEqualTo(InputOutputData.of(1));
    }

    @Test
    void testTuple() {
        var result = InputOutputData.tuple(
                Output.of(1), Output.of(2), Output.of(3), Output.of(4),
                Output.of(5), Output.of(6), Output.of(7), Output.of(8)
        ).join();

        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    void testTupleEmpty() {
        var result = InputOutputData.tuple(
                Output.empty(), Output.empty(), Output.empty(), Output.empty(),
                Output.empty(), Output.empty(), Output.empty(), Output.empty()
        ).join();

        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(null, null, null, null, null, null, null, null));
    }

    @Test
    void testTupleUnknown() {
        var result = InputOutputData.tuple(
                OutputTests.unknown(), OutputTests.unknown(),
                OutputTests.unknown(), OutputTests.unknown(),
                OutputTests.unknown(), OutputTests.unknown(),
                OutputTests.unknown(), OutputTests.unknown()
        ).join();

        assertThat(result.isKnown()).isFalse();
        assertThat(result.getValueNullable()).isNull();
    }

    @Test
    void testAccumulator() {
        var result = InputOutputData.builder(null)
                .accumulate(InputOutputData.of("foo"), (__, o2) -> o2)
                .accumulate(InputOutputData.empty(), (o1, __) -> o1)
                .accumulate(InputOutputData.unknown(), (o1, __) -> o1)
                .accumulate(InputOutputData.emptySecret(), (o1, __) -> o1)
                .accumulate(InputOutputData.unknownSecret(), (o1, __) -> o1)
                .build();

        assertThat(result.getValueNullable()).isNull();
        assertThat(result.isKnown()).isFalse();
        assertThat(result.isSecret()).isTrue();
        assertThat(result.getResources()).isEmpty();
    }
}