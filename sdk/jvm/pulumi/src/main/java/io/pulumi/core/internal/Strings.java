package io.pulumi.core.internal;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;

public class Strings {

    private Strings() {
        throw new UnsupportedOperationException("static class");
    }

    public static Optional<String> emptyToOptional(@Nullable String string) {
        if (string == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(emptyToNull(string.trim()));
    }

    public static boolean isEmptyOrNull(String string) {
        if (string == null) {
            return true;
        }
        return string.isBlank();
    }
}
