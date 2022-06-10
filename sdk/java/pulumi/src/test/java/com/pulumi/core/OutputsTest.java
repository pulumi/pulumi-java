package com.pulumi.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.pulumi.test.PulumiTest.extractValue;
import static com.pulumi.test.internal.PulumiTestInternal.extractOutputData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class OutputsTest {

    @Test
    public void testNullableSecretifyOutput() {
        Output<String> res0_ = Output.ofNullable((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = extractOutputData(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original Input
        var data0_ = extractOutputData(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = Output.of("test1").asSecret();
        var data1 = extractOutputData(res1);
        assertThat(data1.getValueNullable()).isEqualTo("test1");
        assertThat(data1.isSecret()).isTrue();
        assertThat(data1.isKnown()).isTrue();
    }

    @Test
    public void testFormat() {
        var foo = Output.of("foo");
        var bar = Output.of("bar");
        var baz = "baz";
        var result = Output.format("%s-%s-%s-%d", foo, bar, baz, null);
        assertThat(extractValue(result)).isEqualTo("foo-bar-baz-null");
    }

    @Test
    public void testFormatNullArray() {
        assertThatCode(() -> {
            var result = Output.format("", (Object[]) null);
            assertThat(extractValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatNullObject() {
        assertThatCode(() -> {
            var result = Output.format("", (Object) null);
            assertThat(extractValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatNulls() {
        assertThatCode(() -> {
            var result = Output.format("%s%s%s", null, null, null);
            assertThat(extractValue(result)).isEqualTo("nullnullnull");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatEmpty() {
        assertThatCode(() -> {
            var result = Output.format("");
            assertThat(extractValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }

    public static Stream<Arguments> testAll() {
        return Stream.of(
                arguments(named("0", List.of()), List.of()),
                arguments(named("1", List.of(Output.of(1))), List.of(1)),
                arguments(named("2", List.of(Output.of(1), Output.of(2))), List.of(1, 2)),
                arguments(named("1 and null",
                        List.of(Output.of(1), Output.ofNullable(null))
                ), newArrayList(1, null))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testAll(List<Output<Integer>> list, List<Integer> expected) {
        assertThat(extractOutputData(Output.all(list)).getValueNullable())
                .isNotNull()
                .hasSize(list.size())
                .containsExactlyElementsOf(expected);
    }
}
