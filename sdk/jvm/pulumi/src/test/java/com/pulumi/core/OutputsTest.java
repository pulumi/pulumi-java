package com.pulumi.core;

import org.junit.jupiter.api.Test;

import static io.pulumi.core.OutputTests.waitFor;
import static io.pulumi.core.OutputTests.waitForValue;
import static org.assertj.core.api.Assertions.assertThat;

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
        var result = Output.format("%s-%s", foo, bar);
        assertThat(waitForValue(result)).isEqualTo("foo-bar");
    }
}
