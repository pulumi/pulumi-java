package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodegenTest {

    @Test
    void testSecret() {
        var actual = Codegen.integerProp("a").secret().arg(Output.of(1)).require();
        assertThat(OutputTests.waitFor(actual).isSecret()).isTrue();
    }
}