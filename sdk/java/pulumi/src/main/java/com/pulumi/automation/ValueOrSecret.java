package com.pulumi.automation;

import static java.util.Objects.requireNonNull;

public class ValueOrSecret {

    private final String value;
    private final boolean isSecret;

    private ValueOrSecret(String value, boolean isSecret) {
        this.value = requireNonNull(value);
        this.isSecret = isSecret;
    }

    public static ValueOrSecret value(String value) {
        return new ValueOrSecret(value, false);
    }

    public static ValueOrSecret secret(String value) {
        return new ValueOrSecret(value, true);
    }

    public String value() {
        return value;
    }

    public boolean isSecret() {
        return isSecret;
    }
}
