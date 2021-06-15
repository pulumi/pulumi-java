package io.pulumi.core.internal;

import io.grpc.Internal;

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

    public static Optional<Boolean> getBooleanEnvironmentVariable(String name) {
        return getEnvironmentVariable(name)
                .map(value -> Objects.equals(value, "1") || Boolean.parseBoolean(value));
    }

    public static boolean requireBooleanEnvironmentVariable(String name) {
        return getBooleanEnvironmentVariable(name).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "expected environment variable '%s', not found or empty", name)
                ));
    }
}
