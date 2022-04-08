package io.pulumi.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OutputsTest {

    @Test
    public void testNullableSecretifyOutput() {
        Output<String> res0_ = Output.of((String) null);
        Output<String> res0 = res0_.asSecret();
        var data0 = OutputTests.waitFor(res0);
        assertThat(data0.getValueNullable()).isEqualTo(null);
        assertThat(data0.isSecret()).isTrue();
        assertThat(data0.isKnown()).isTrue();

        // stringify should not modify the original Input
        var data0_ = OutputTests.waitFor(res0_);
        assertThat(data0_.isSecret()).isFalse();

        Output<String> res1 = Output.of("test1").asSecret();
        var data1 = OutputTests.waitFor(res1);
        assertThat(data1.getValueNullable()).isEqualTo("test1");
        assertThat(data1.isSecret()).isTrue();
        assertThat(data1.isKnown()).isTrue();

        Output<String> res2 = Output.ofNullable(Output.of("test2")).asSecret();
        var data2 = OutputTests.waitFor(res2);
        assertThat(data2.getValueNullable()).isEqualTo("test2");
        assertThat(data2.isSecret()).isTrue();
        assertThat(data2.isPresent()).isTrue();
        assertThat(data2.isKnown()).isTrue();
    }
}
