package io.pulumi.test.internal.assertj;

import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.CoreMatchers;

public class PulumiConditions {

    public static HamcrestCondition<String> containsString(String s) {
        return new HamcrestCondition<>(CoreMatchers.containsString(s));
    }

    public static HamcrestCondition<String> startsWith(String s) {
        return new HamcrestCondition<>(CoreMatchers.startsWith(s));
    }
}
