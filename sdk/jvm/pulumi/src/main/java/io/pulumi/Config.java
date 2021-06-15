package io.pulumi;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.grpc.Internal;
import io.pulumi.core.Output;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.exceptions.RunException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Reader;
import java.util.Objects;
import java.util.Optional;

/**
 * Config is a bag of related configuration state. Each bag contains any number
 * of configuration variables, indexed by simple keys, and each has a name that uniquely
 * identifies it; two bags with different names do not share values for variables that
 * otherwise share the same key.  For example, a bag whose name is {@code pulumi:foo}, with keys
 * {@code a}, {@code b}, and {@code c}, is entirely separate from a bag whose name is
 * {@code pulumi:bar} with the same simple key names.  Each key has a fully qualified names,
 * such as {@code pulumi:foo:a}, ..., and {@code pulumi:bar:a}, respectively.
 */
@ParametersAreNonnullByDefault
public class Config {

    private final String name;
    private final Gson gson;

    private Config() {
        this(Deployment.getInstance().getProjectName());
    }

    private Config(String name) {
        Objects.requireNonNull(name);

        if (name.endsWith(":config")) {
            name = name.replaceAll(":config$", "");
        }

        this.name = name;
        this.gson = new Gson();
    }

    /**
     * Creates a new @see {@link Config} instance, with default, the name of the current project.
     */
    public static Config of() {
        return new Config();
    }

    /**
     * Creates a new @see {@link Config} instance.
     *
     * @param name unique logical name
     */
    public static Config of(String name) {
        return new Config(name);
    }

    /**
     * The configuration bag's logical name and uniquely identifies it.
     * The default is the name of the current project.
     *
     * @return unique logical configuration bag name
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Loads an optional configuration value by its key, or returns empty if it doesn't exist.
     */
    public Optional<String> get(String key) {
        var fullKey = fullKey(key);
        return DeploymentInternal.getInstance().getConfig(fullKey);
    }

    /**
     * Loads an optional configuration value by its key, marking it as a secret,
     * or empty if it doesn't exist.
     */
    public Output<Optional<String>> getSecret(String key) {
        return Output.ofSecret(get(key));
    }

    /**
     * Loads an optional configuration value, as a boolean, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal boolean, this function will throw an error.
     */
    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(Boolean::parseBoolean);
    }

    /**
     * Loads an optional configuration value, as a boolean, by its key, making it as a secret or
     * null if it doesn't exist. If the configuration value isn't a legal boolean, this
     * function will throw an error.
     */
    public Output<Optional<Boolean>> getSecretBoolean(String key) {
        return Output.ofSecret(getBoolean(key));
    }

    /**
     * Loads an optional configuration value, as a number, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    public Optional<Integer> getInteger(String key) {
        return get(key).map(string -> {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ex) {
                throw new ConfigTypeException(fullKey(key), string, "Integer", ex);
            }
        });
    }

    /**
     * Loads an optional configuration value, as a number, by its key, marking it as a secret
     * or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    public Output<Optional<Integer>> getSecretInteger(String key) {
        return Output.ofSecret(getInteger(key));
    }

    /**
     * Loads an optional configuration value, as an object, by its key, or null if it doesn't
     * exist. This works by taking the value associated with {@code key} and passing
     * it to @see {@link Gson#fromJson(Reader, Class)}.
     */
    public <T> Optional<T> getObject(String key, Class<T> classOfT) {
        var v = get(key);
        try {
            return v.map(string -> gson.fromJson(string, classOfT));
        } catch (JsonParseException ex) {
            throw new ConfigTypeException(fullKey(key), v, classOfT.getTypeName(), ex);
        }
    }

    /**
     * Loads an optional configuration value, as an object, by its key, marking it as a secret
     * or null if it doesn't exist. This works by taking the value associated with {@code key}
     * and passing it to @see {@link Gson#fromJson(Reader, Class)}.
     */
    public <T> Output<Optional<T>> getSecretObject(String key, Class<T> classOfT) {
        return Output.ofSecret(getObject(key, classOfT));
    }

    /**
     * Loads a configuration value by its given key. If it doesn't exist, an error is thrown.
     */
    public String require(String key) {
        return get(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    /**
     * Loads a configuration value by its given key, marking it as a secret. If it doesn't exist, an error
     * is thrown.
     */
    public Output<String> requireSecret(String key) {
        return Output.ofSecret(require(key));
    }

    /**
     * Loads a configuration value, as a boolean, by its given key. If it doesn't exist, or the
     * configuration value is not a legal boolean, an error is thrown.
     */
    public boolean requireBoolean(String key) {
        return getBoolean(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    /**
     * Loads a configuration value, as a boolean, by its given key, marking it as a secret.
     * If it doesn't exist, or the configuration value is not a legal boolean, an error is thrown.
     */
    public Output<Boolean> requireSecretBoolean(String key) {
        return Output.ofSecret(requireBoolean(key));
    }

    /**
     * Loads a configuration value, as a number, by its given key. If it doesn't exist, or the
     * configuration value is not a legal number, an error is thrown.
     */
    public int requireInteger(String key) {
        return getInteger(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    /**
     * Loads a configuration value, as a number, by its given key, marking it as a secret.
     * If it doesn't exist, or the configuration value is not a legal number, an error is thrown.
     */
    public Output<Integer> requireSecretInteger(String key) {
        return Output.ofSecret(requireInteger(key));
    }

    /**
     * Loads a configuration value as a JSON string and deserializes the JSON into an object.
     * object. If it doesn't exist, or the configuration value cannot be converted
     * using @see {@link Gson#fromJson(Reader, Class)}, an error is thrown.
     */
    public <T> T requireObject(String key, Class<T> classOfT) {
        return getObject(key, classOfT).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    /**
     * Loads a configuration value as a JSON string and deserializes the JSON into a JavaScript
     * object, marking it as a secret. If it doesn't exist, or the configuration value cannot
     * be converted using @see {@link Gson#fromJson(Reader, Class)},
     * an error is thrown.
     */
    public <T> Output<T> requireSecretObject(String key, Class<T> classOfT) {
        return Output.ofSecret(requireObject(key, classOfT));
    }

    /**
     * Turns a simple configuration key into a fully resolved one, by prepending the bag's name.
     */
    private String fullKey(String key) {
        return String.format("%s:%s", this.name, key);
    }

    /**
     * ConfigTypeException is used when a configuration value is of the wrong type.
     */
    private static class ConfigTypeException extends RunException {
        public ConfigTypeException(String key, @Nullable Object v, String expectedType, @Nullable Exception cause) {
            super(String.format("Configuration '%s' value '%s' is not a valid %s", key, v, expectedType), cause);
        }
    }

    /**
     * ConfigMissingException is used when a configuration value is completely missing.
     */
    @ParametersAreNonnullByDefault
    @Internal
    @VisibleForTesting
    public static class ConfigMissingException extends RunException {
        public ConfigMissingException(String key) {
            super(String.format("Missing required configuration variable '%s'\n", key) +
                    String.format("\tplease set a value using the command `pulumi config set %s <value>`", key));
        }
    }
}