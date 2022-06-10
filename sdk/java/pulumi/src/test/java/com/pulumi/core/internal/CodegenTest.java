package com.pulumi.core.internal;

import com.pulumi.core.Output;
import org.junit.jupiter.api.Test;

import static com.pulumi.test.internal.PulumiTestInternal.extractOutputData;
import static org.assertj.core.api.Assertions.assertThat;

class CodegenTest {

    @Test
    void testSecret() {
        var actual = Codegen.integerProp("a").secret().arg(Output.of(1)).require();
        assertThat(extractOutputData(actual).isSecret()).isTrue();
    }
}