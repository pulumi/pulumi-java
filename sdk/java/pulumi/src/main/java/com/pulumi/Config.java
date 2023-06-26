package com.pulumi;

import com.google.gson.Gson;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;

import java.io.Reader;
import java.util.Optional;

/**
 * Config is a bag of related configuration state. Each bag contains any number
 * of configuration variables, indexed by simple keys, and each has a name that uniquely
 * identifies it; two bags with different names do not share values for variables that
 * otherwise share the same key.
 * For example, a bag whose name is {@code pulumi:foo}, with keys
 * {@code a}, {@code b}, and {@code c}, is entirely separate from a bag whose name is
 * {@code pulumi:bar} with the same simple key names. Each key has a fully qualified names,
 * such as {@code pulumi:foo:a}, ..., and {@code pulumi:bar:a}, respectively.
 * @see com.pulumi.context.ConfigContext
 */
public interface Config {

    /**
     * Get a copy of {@code this} {@link Config} with a different name (namespace prefix)
     *
     * @param name the config namespace name
     * @return a new Config with the given name
     */
    Config withName(String name);

    /**
     * For internal use by providers.
     *
     * @see com.pulumi.Context#config()
     * @see com.pulumi.Context#config(String)
     * @deprecated will be removed in the future, use {@link com.pulumi.Context#config(String)} or {@link com.pulumi.core.internal.Codegen#config(String)}
     */
    // TODO: remove after refactoring the provider codegen
    @InternalUse
    @Deprecated
    static Config of(String name) {
        return Deployment.getInstance().getConfig().withName(name);
    }

    /**
     * The configuration bag's logical name and uniquely identifies it.
     * The default is the name of the current project.
     *
     * @return unique logical configuration bag name
     */
    String getName();

    /**
     * Loads an optional configuration value by its key, or returns empty if it doesn't exist.
     */
    Optional<String> get(String key);

    /**
     * Loads an optional configuration value by its key, marking it as a secret,
     * or empty if it doesn't exist.
     */
    Output<Optional<String>> getSecret(String key);

    /**
     * Loads an optional configuration value, as a boolean, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal boolean, this function will throw an error.
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * Loads an optional configuration value, as a boolean, by its key, making it as a secret or
     * null if it doesn't exist. If the configuration value isn't a legal boolean, this
     * function will throw an error.
     */
    Output<Optional<Boolean>> getSecretBoolean(String key);

    /**
     * Loads an optional configuration value, as a number, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    Optional<Integer> getInteger(String key);

    /**
     * Loads an optional configuration value, as a number, by its key, marking it as a secret
     * or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    Output<Optional<Integer>> getSecretInteger(String key);

    /**
     * Loads an optional configuration value, as a number, by its key, or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    Optional<Double> getDouble(String key);

    /**
     * Loads an optional configuration value, as a number, by its key, marking it as a secret
     * or null if it doesn't exist.
     * If the configuration value isn't a legal number, this function will throw an error.
     */
    Output<Optional<Double>> getSecretDouble(String key);

    /**
     * Loads an optional configuration value as a JSON string and deserializes it
     * as an object, by its key, or null if it doesn't exist.
     * This works by taking the value associated with {@code key} and passing
     * it to {@link Gson#fromJson(Reader, Class)}.
     */
    <T> Optional<T> getObject(String key, Class<T> classOfT);

    /**
     * Loads an optional configuration value as a JSON string and deserializes it
     * as an object, by its key, marking it as a secret or null (empty) if it doesn't exist.
     * This works by taking the value associated with {@code key}
     * and passing it to {@link Gson#fromJson(Reader, Class)}.
     */
    <T> Output<Optional<T>> getSecretObject(String key, Class<T> classOfT);

    /**
     * @see #getObject(String, Class)
     */
    <T> Optional<T> getObject(String key, TypeShape<T> shapeOfT);

    /**
     * @see #getSecretObject(String, Class)
     */
    <T> Output<Optional<T>> getSecretObject(String key, TypeShape<T> shapeOfT);

    /**
     * Loads a configuration value by its given key. If it doesn't exist, an error is thrown.
     */
    String require(String key);

    /**
     * Loads a configuration value by its given key, marking it as a secret. If it doesn't exist, an error
     * is thrown.
     */
    Output<String> requireSecret(String key);

    /**
     * Loads a configuration value, as a boolean, by its given key. If it doesn't exist, or the
     * configuration value is not a legal boolean, an error is thrown.
     */
    boolean requireBoolean(String key);

    /**
     * Loads a configuration value, as a boolean, by its given key, marking it as a secret.
     * If it doesn't exist, or the configuration value is not a legal boolean, an error is thrown.
     */
    Output<Boolean> requireSecretBoolean(String key);

    /**
     * Loads a configuration value, as a number, by its given key. If it doesn't exist, or the
     * configuration value is not a legal number, an error is thrown.
     */
    int requireInteger(String key);

    /**
     * Loads a configuration value, as a number, by its given key, marking it as a secret.
     * If it doesn't exist, or the configuration value is not a legal number, an error is thrown.
     */
    Output<Integer> requireSecretInteger(String key);

    /**
     * Loads a configuration value, as a number, by its given key. If it doesn't exist, or the
     * configuration value is not a legal number, an error is thrown.
     */
    double requireDouble(String key);

    /**
     * Loads a configuration value, as a number, by its given key, marking it as a secret.
     * If it doesn't exist, or the configuration value is not a legal number, an error is thrown.
     */
    Output<Double> requireSecretDouble(String key);

    /**
     * Loads a configuration value as a JSON string and deserializes it into an object.
     * If it doesn't exist, or the configuration value cannot be converted
     * using {@link Gson#fromJson(Reader, Class)}, an error is thrown.
     */
    <T> T requireObject(String key, Class<T> classOfT);

    /**
     * Loads a configuration value as a JSON string and deserializes it into an object,
     * marking it as a secret.
     * If it doesn't exist, or the configuration value cannot be converted
     * using {@link Gson#fromJson(Reader, Class)}, an error is thrown.
     */
    <T> Output<T> requireSecretObject(String key, Class<T> classOfT);
}
