package com.pulumi.core;

import com.google.common.collect.ImmutableSet;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;

public class OutputTests {

    public static <T> Output<T> unknown() {
        return new OutputInternal<>(OutputData.ofNullable(ImmutableSet.of(), null, false, false));
    }

    public static <T> Output<T> unknownSecret() {
        return new OutputInternal<>(OutputData.ofNullable(ImmutableSet.of(), null, false, true));
    }
}
