package io.pulumi;

import com.google.gson.Gson;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;

import java.io.Reader;
import java.util.Optional;

/**
 * Config is a bag of related configuration state. Each bag contains any number
 * of configuration variables, indexed by simple keys, and each has a name that uniquely
 * identifies it; two bags with different names do not share values for variables that
 * otherwise share the same key.
 * <p/>
 * For example, a bag whose name is {@code pulumi:foo}, with keys
 * {@code a}, {@code b}, and {@code c}, is entirely separate from a bag whose name is
 * {@code pulumi:bar} with the same simple key names.  Each key has a fully qualified names,
 * such as {@code pulumi:foo:a}, ..., and {@code pulumi:bar:a}, respectively.
 */
public interface Config {

    /**
     * @param name unique logical name of the configuration
     * @return new configuration with the unique name
     */
    Config withName(String name);

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
