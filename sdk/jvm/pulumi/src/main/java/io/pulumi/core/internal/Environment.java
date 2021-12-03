package io.pulumi.core.internal;

import io.grpc.Internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Internal
public class Environment {
    private static final Logger logger = Logger.getLogger(Environment.class.getName());

    private Environment() {
        throw new UnsupportedOperationException("static class");
    }

    public static Optional<String> getEnvironmentVariable(String name) {
        Objects.requireNonNull(name);
        // make sure we return empty string as empty optional
        var value = Optional.ofNullable(System.getenv(name))
                .map(String::trim) // make sure we get rid of white spaces
                .map(v -> v.isEmpty() ? null : v);
        logger.log(Level.FINEST, name + "=" + value);
        return value;
    }

    public static String requireEnvironmentVariable(String name) {
        return getEnvironmentVariable(name).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "expected environment variable '%s', not found or empty", name)
                ));
    }

    private static final String[] trueValues = { "1", "t", "T", "true", "TRUE", "True" };
    private static final String[] falseValues = { "0", "f", "F", "false", "FALSE", "False" };

    public static Optional<Boolean> getBooleanEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(value -> {
                    if (Arrays.stream(trueValues)
                            .map(v -> v.equalsIgnoreCase(value))
                            .reduce(false, (b1, b2) -> b1 || b2)
                    ) {
                        return Optional.of(true);
                    }
                    if (!Arrays.stream(falseValues)
                            .map(v -> v.equalsIgnoreCase(value))
                            .reduce(false, (b1, b2) -> b1 || b2)
                    ) {
                        return Optional.of(false);
                    }
                    return Optional.empty();
                });
    }

    public static boolean requireBooleanEnvironmentVariable(String name) {
        return getBooleanEnvironmentVariable(name).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "expected environment variable '%s', not found or empty, or unparseable as a boolean", name)
                ));
    }

    public static Optional<Integer> getIntegerEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(s -> {
                    try {
                        return Optional.of(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        logger.severe(String.format("can't parse environment variable '%s' as an integer: %s", name, ex.getMessage()));
                        return Optional.empty();
                    }
                });
    }

    public static int requireIntegerEnvironmentVariable(String name) {
        return getIntegerEnvironmentVariable(name).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "expected environment variable '%s', not found or empty, or unparseable as an integer", name)
                ));
    }

    public static Optional<Double> getDoubleEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .flatMap(s -> {
                    try {
                        return Optional.of(Double.parseDouble(s));
                    } catch (NumberFormatException ex) {
                        logger.severe(String.format("can't parse environment variable '%s' as a double: %s", name, ex.getMessage()));
                        return Optional.empty();
                    }
                });
    }

    public static double requireDoubleEnvironmentVariable(String name) {
        return getDoubleEnvironmentVariable(name).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "expected environment variable '%s', not found or empty, or unparseable as a double", name)
                ));
    }


}
