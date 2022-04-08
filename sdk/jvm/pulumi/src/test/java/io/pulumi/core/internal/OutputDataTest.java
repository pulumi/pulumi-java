package io.pulumi.core.internal;

import io.pulumi.core.Output;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputDataTest {

    @Test
    void testHashCodeEqualsContract() {
        assertThat(OutputData.of((Object)null)).isEqualTo(OutputData.of((Object)null));
        assertThat(OutputData.of((Object)null)).isNotEqualTo(OutputData.of(1));
    }

    @Test
    void testTuple() {
        var result = OutputData.tuple(
                Output.of(1), Output.of(2), Output.of(3), Output.of(4),
                Output.of(5), Output.of(6), Output.of(7), Output.of(8)
        ).join();

        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    void testTupleEmpty() {
        var e = Output.ofNullable(null);
        var result = OutputData.tuple(e, e, e, e, e, e, e, e).join();
        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(null, null, null, null, null, null, null, null));
    }

    @Test
    void testTupleUnknown() {
        var result = OutputData.tuple(
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
        var result = OutputData.builder(null)
                .accumulate(OutputData.of("foo"), (__, o2) -> o2)
                .accumulate(OutputData.of(null), (o1, __) -> o1)
                .accumulate(OutputData.unknown(), (o1, __) -> o1)
                .accumulate(OutputData.of(null).withIsSecret(true), (o1, __) -> o1)
                .accumulate(OutputData.unknownSecret(), (o1, __) -> o1)
                .build();

        assertThat(result.getValueNullable()).isNull();
        assertThat(result.isKnown()).isFalse();
        assertThat(result.isSecret()).isTrue();
        assertThat(result.getResources()).isEmpty();
    }
}
