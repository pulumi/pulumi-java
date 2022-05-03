package io.pulumi.test.internal.assertj;

import org.assertj.core.api.AbstractAssert;

import javax.annotation.Nullable;

import static io.pulumi.test.internal.assertj.ContainsNullableCondition.containsNullable;

public class ContainsNullableAssert<ELEMENT> extends AbstractAssert<ContainsNullableAssert<ELEMENT>, Iterable<? extends ELEMENT>> {
    protected ContainsNullableAssert(@Nullable Iterable<? extends ELEMENT> elements) {
        super(elements, ContainsNullableAssert.class);
    }

    public static <ELEMENT> ContainsNullableAssert<ELEMENT> assertThat(@Nullable Iterable<? extends ELEMENT> actual) {
        return new ContainsNullableAssert<>(actual);
    }

    public ContainsNullableAssert<ELEMENT> containsAll(@Nullable Iterable<? extends ELEMENT> values) {
        return has(containsNullable(values));
    }
}
