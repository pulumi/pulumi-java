package io.pulumi.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OutputsTest {

    @Test
    public void testNullableSecretifyOutput() {
        Output<String> res0_ = Output.ofNullable((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = InputOutputTests.waitFor(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isPresent()).isFalse();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original Input
        var data0_ = InputOutputTests.waitFor(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = Output.ofNullable("test1").asSecret();
        var data1 = InputOutputTests.waitFor(res1);
        assertThat(data1.getValueNullable()).isEqualTo("test1");
        assertThat(data1.isSecret()).isTrue();
        assertThat(data1.isPresent()).isTrue();
        assertThat(data1.isKnown()).isTrue();

        Output<String> res2 = Output.ofNullable(Output.of("test2")).asSecret();
        var data2 = InputOutputTests.waitFor(res2);
        assertThat(data2.getValueNullable()).isEqualTo("test2");
        assertThat(data2.isSecret()).isTrue();
        assertThat(data2.isPresent()).isTrue();
        assertThat(data2.isKnown()).isTrue();
    }
}
