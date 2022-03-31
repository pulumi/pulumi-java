package io.pulumi.core.internal;

import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputDataTest {
    @Test
    void testHashCodeEqualsContract() {
        assertThat(OutputData.empty()).isEqualTo(OutputData.empty());
        assertThat(OutputData.empty()).isNotEqualTo(OutputData.of(1));
    }

    @Test
    void testTuple() {
        var ctx = OutputTests.testContext();
        var result = OutputData.tuple(
                ctx.output.of(1), ctx.output.of(2), ctx.output.of(3), ctx.output.of(4),
                ctx.output.of(5), ctx.output.of(6), ctx.output.of(7), ctx.output.of(8)
        ).join();

        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(1, 2, 3, 4, 5, 6, 7, 8));
    }

    @Test
    void testTupleEmpty() {
        var ctx = OutputTests.testContext();
        var result = OutputData.tuple(
                ctx.output.empty(), ctx.output.empty(), ctx.output.empty(), ctx.output.empty(),
                ctx.output.empty(), ctx.output.empty(), ctx.output.empty(), ctx.output.empty()
        ).join();

        assertThat(result.getValueNullable()).isNotNull()
                .isEqualTo(Tuples.of(null, null, null, null, null, null, null, null));
    }

    @Test
    void testTupleUnknown() {
        var ctx = OutputTests.testContext();

        var result = OutputData.tuple(
                OutputTests.unknown(ctx.deployment), OutputTests.unknown(ctx.deployment),
                OutputTests.unknown(ctx.deployment), OutputTests.unknown(ctx.deployment),
                OutputTests.unknown(ctx.deployment), OutputTests.unknown(ctx.deployment),
                OutputTests.unknown(ctx.deployment), OutputTests.unknown(ctx.deployment)
        ).join();

        assertThat(result.isKnown()).isFalse();
        assertThat(result.getValueNullable()).isNull();
    }

    @Test
    void testAccumulator() {
        var result = OutputData.builder(null)
                .accumulate(OutputData.of("foo"), (__, o2) -> o2)
                .accumulate(OutputData.empty(), (o1, __) -> o1)
                .accumulate(OutputData.unknown(), (o1, __) -> o1)
                .accumulate(OutputData.emptySecret(), (o1, __) -> o1)
                .accumulate(OutputData.unknownSecret(), (o1, __) -> o1)
                .build();

        assertThat(result.getValueNullable()).isNull();
        assertThat(result.isKnown()).isFalse();
        assertThat(result.isSecret()).isTrue();
        assertThat(result.getResources()).isEmpty();
    }
}