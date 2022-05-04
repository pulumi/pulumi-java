package com.pulumi.core;

import org.junit.jupiter.api.Test;

import static com.pulumi.core.OutputTests.waitFor;
import static com.pulumi.core.OutputTests.waitForValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class OutputsTest {

    @Test
    public void testNullableSecretifyOutput() {
        Output<String> res0_ = Output.ofNullable((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = waitFor(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original Input
        var data0_ = waitFor(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = Output.of("test1").asSecret();
        var data1 = waitFor(res1);
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
        assertThat(waitForValue(result)).isEqualTo("foo-bar-baz-null");
    }

    @Test
    public void testFormatNullArray() {
        assertThatCode(() -> {
            var result = Output.format("", (Object[]) null);
            assertThat(waitForValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatNullObject() {
        assertThatCode(() -> {
            var result = Output.format("", (Object) null);
            assertThat(waitForValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatNulls() {
        assertThatCode(() -> {
            var result = Output.format("%s%s%s", null, null, null);
            assertThat(waitForValue(result)).isEqualTo("nullnullnull");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testFormatEmpty() {
        assertThatCode(() -> {
            var result = Output.format("");
            assertThat(waitForValue(result)).isEqualTo("");
        }).doesNotThrowAnyException();
    }
}
