package com.pulumi.core;

import java.util.Optional;

/**
 * Helper class used to track values in policy resource objects.
 * It's used to prevent accessible proprties of unknown value.
 *
 * @param <T>
 */
public class UndeferrableValue<T> {
    private final T value;
    private final boolean isPresent;

    public UndeferrableValue(T value) {
        isPresent = true;
        this.value = value;
    }

    public UndeferrableValue() {
        isPresent = false;
        this.value = null;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public T getValue(String context) {
        if (isPresent) {
            return value;
        }

        throw new UndeferrableValueException(String.format("Value '%s' is not present", context));
    }
}
