package io.pulumi.test.internal.assertj;

import org.assertj.core.api.Assertions;

import javax.annotation.Nullable;

public class PulumiAssertions extends Assertions {
    public static <ELEMENT> ContainsNullableAssert<ELEMENT> assertThatNullable(@Nullable Iterable<? extends ELEMENT> values) {
        return new ContainsNullableAssert<>(values);
    }
}
