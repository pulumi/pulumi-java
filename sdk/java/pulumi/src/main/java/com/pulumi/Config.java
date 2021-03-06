package com.pulumi;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.exceptions.RunException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Config is a bag of related configuration state. Each bag contains any number
 * of configuration variables, indexed by simple keys, and each has a name that uniquely
 * identifies it; two bags with different names do not share values for variables that
 * otherwise share the same key.  For example, a bag whose name is {@code pulumi:foo}, with keys
 * {@code a}, {@code b}, and {@code c}, is entirely separate from a bag whose name is
 * {@code pulumi:bar} with the same simple key names.  Each key has a fully qualified names,
 * such as {@code pulumi:foo:a}, ..., and {@code pulumi:bar:a}, respectively.
 * @see com.pulumi.context.ConfigContext
 */
@ParametersAreNonnullByDefault
public class Config {

    private final String name;
    private final DeploymentImpl.Config internalConfig;

    /**
     * @see com.pulumi.Context#config()
     * @see com.pulumi.Context#config(String)
     */
    @InternalUse
    public Config(DeploymentImpl.Config internalConfig, String name) {
        this.internalConfig = requireNonNull(internalConfig);

        requireNonNull(name);
        if (name.endsWith(":config")) {
            name = name.replaceAll(":config$", "");
        }
        this.name = name;
    }

    /**
     * For internal use by providers.
     * @see com.pulumi.Context#config()
     * @see com.pulumi.Context#config(String)
     * @deprecated will be removed in the future, use {@link com.pulumi.Context#config(String)}
     */
    // TODO: remove after refactoring the deployment
    @InternalUse
    @Deprecated
    public static Config of(String name) {
        return new Config(Deployment.getInstance().getConfig(), name);
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
        // FIXME: due to https://github.com/pulumi/pulumi/issues/7127
        //        there is a check for key being a secret missing here
        return this.internalConfig.getConfig(fullKey);
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
     * Loads an optional configuration value, as a number, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    public Optional<Double> getDouble(String key) {
        return get(key).map(string -> {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ex) {
                throw new ConfigTypeException(fullKey(key), string, "Double", ex);
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
     * Loads an optional configuration value as a JSON string and deserializes it
     * as an object, by its key, or null if it doesn't exist.
     * This works by taking the value associated with {@code key} and passing
     * it to {@link Gson#fromJson(Reader, Class)}.
     */
    public <T> Optional<T> getObject(String key, Class<T> classOfT) {
        var v = get(key);
        try {
            var gson = new Gson();
            return v.map(string -> gson.fromJson(string, classOfT));
        } catch (JsonParseException ex) {
            throw new ConfigTypeException(fullKey(key), v, classOfT.getTypeName(), ex);
        }
    }

    /**
     * Loads an optional configuration value as a JSON string and deserializes it
     * as an object, by its key, marking it as a secret or null (empty) if it doesn't exist.
     * This works by taking the value associated with {@code key}
     * and passing it to {@link Gson#fromJson(Reader, Class)}.
     */
    public <T> Output<Optional<T>> getSecretObject(String key, Class<T> classOfT) {
        return Output.ofSecret(getObject(key, classOfT));
    }

    /**
     * @see #getObject(String, Class)
     */
    public <T> Optional<T> getObject(String key, TypeShape<T> shapeOfT) {
        var v = get(key);
        // TODO should this be using Pulumi deserializers from protobuf Struct instead of GSON?
        // JSON can be converted to protobuf struct.
        try {
            var gson = new Gson();
            return v.map(string -> gson.fromJson(string, shapeOfT.toGSON().getType()));
        } catch (JsonParseException ex) {
            throw new ConfigTypeException(fullKey(key), v, shapeOfT.getTypeName(), ex);
        }
    }

    /**
     * @see #getSecretObject(String, Class)
     */
    public <T> Output<Optional<T>> getSecretObject(String key, TypeShape<T> shapeOfT) {
        return Output.ofSecret(getObject(key, shapeOfT));
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
     * Loads a configuration value as a JSON string and deserializes it into an object.
     * If it doesn't exist, or the configuration value cannot be converted
     * using {@link Gson#fromJson(Reader, Class)}, an error is thrown.
     */
    public <T> T requireObject(String key, Class<T> classOfT) {
        return getObject(key, classOfT).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    /**
     * Loads a configuration value as a JSON string and deserializes it into an object,
     * marking it as a secret.
     * If it doesn't exist, or the configuration value cannot be converted
     * using {@link Gson#fromJson(Reader, Class)}, an error is thrown.
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
    @InternalUse
    @VisibleForTesting
    public static class ConfigMissingException extends RunException {
        public ConfigMissingException(String key) {
            super(String.format("Missing required configuration variable '%s'\n", key) +
                    String.format("\tplease set a value using the command `pulumi config set %s <value>`", key));
        }

        public ConfigMissingException(String key, List<String> envVars) {
            super(String.format("Missing required configuration variable '%s'\n", key) +
                    String.format("\tplease set a value using the command `pulumi config set %s <value>`", key) +
                    String.format("\tor provide it via an environment variable %s",
                            String.join(", ", envVars)));
        }
    }
}