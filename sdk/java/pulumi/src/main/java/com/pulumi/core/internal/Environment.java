package com.pulumi.core.internal;

import com.pulumi.core.Either;
import com.pulumi.core.internal.annotations.InternalUse;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pulumi.core.internal.Arrays.concat;
import static com.pulumi.core.internal.Arrays.contains;

@InternalUse
public class Environment {

    private static final Logger logger = Logger.getLogger(Environment.class.getName());

    private Environment() {
        throw new UnsupportedOperationException("static class");
    }

    private static Optional<String> getEnvironmentVariableOptional(String name){
        Objects.requireNonNull(name, "Expected an environment variable name, got 'null'");
        // make sure we return blank string as empty optional
        return Optional.ofNullable(System.getenv(name))
                .map(String::trim) // make sure we get rid of white spaces
                .map(v -> v.isEmpty() ? null : v);
    }

    public static boolean hasEnvironmentVariable(String name) {
        return getEnvironmentVariableOptional(name).isPresent();
    }

    public static Either<RuntimeException, String> getEnvironmentVariable(String name) {
        var value = getEnvironmentVariableOptional(name);
        logger.log(Level.FINE, name + "=" + value.orElse(""));
        return value
                .map(Either::<RuntimeException, String>valueOf)
                .orElse(Either.errorOf(
                        new IllegalArgumentException(String.format(
                                "expected environment variable: '%s', got: %s", name, System.getenv().keySet()
                        ))
                ));
    }

    private static final String[] trueValues = {"1", "t", "T", "true", "TRUE", "True"};
    private static final String[] falseValues = {"0", "f", "F", "false", "FALSE", "False"};

    public static Either<RuntimeException, Boolean> getBooleanEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(value -> {
                    if (contains(trueValues, value, String::equalsIgnoreCase)) {
                        return Either.valueOf(true);
                    }
                    if (contains(falseValues, value, String::equalsIgnoreCase)) {
                        return Either.valueOf(false);
                    }
                    return Either.errorOf(new IllegalArgumentException(String.format(
                            "expected environment variable '%s' value to be one of: %s; got: '%s'",
                            name,
                            Arrays.toString(concat(trueValues, falseValues)),
                            value
                    )));
                });
    }

    public static Either<RuntimeException, Integer> getIntegerEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(s -> {
                    try {
                        return Either.valueOf(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        return Either.errorOf(new IllegalArgumentException(String.format(
                                "can't parse environment variable '%s' as an integer: %s", name, ex.getMessage()
                        ), ex));
                    }
                });
    }

    public static Either<RuntimeException, Double> getDoubleEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(s -> {
                    try {
                        return Either.valueOf(Double.parseDouble(s));
                    } catch (NumberFormatException ex) {
                        return Either.errorOf(new IllegalArgumentException(String.format(
                                "can't parse environment variable '%s' as a double: %s", name, ex.getMessage()
                        ), ex));
                    }
                });
    }

}
