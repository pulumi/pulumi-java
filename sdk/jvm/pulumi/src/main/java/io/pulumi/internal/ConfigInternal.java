package io.pulumi.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.pulumi.Config;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.exceptions.RunException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class ConfigInternal implements Config {

    /**
     * The environment variable key that the language plugin uses to set configuration values.
     */
    private static final String ConfigEnvKey = "PULUMI_CONFIG";

    /**
     * The environment variable key that the language plugin uses to set the list of secret configuration keys.
     */
    private static final String ConfigSecretKeysEnvKey = "PULUMI_CONFIG_SECRET_KEYS";

    private final String name;
    private final ImmutableMap<String, String> allConfig;
    private final ImmutableSet<String> configSecretKeys;

    @VisibleForTesting
    public ConfigInternal(String name, ImmutableMap<String, String> allConfig, ImmutableSet<String> configSecretKeys) {
        this.allConfig = Objects.requireNonNull(allConfig);
        this.configSecretKeys = Objects.requireNonNull(configSecretKeys);

        requireNonNull(name);
        if (name.endsWith(":config")) {
            name = name.replaceAll(":config$", "");
        }
        this.name = name;
    }

    @Override
    public Config withName(String name) {
        return new ConfigInternal(name, allConfig, configSecretKeys);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<String> get(String key) {
        var fullKey = fullKey(key);
        return this.getConfig(fullKey);
    }

    @Override
    public Output<Optional<String>> getSecret(String key) {
        return Output.ofSecret(get(key));
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(Boolean::parseBoolean);
    }

    @Override
    public Output<Optional<Boolean>> getSecretBoolean(String key) {
        return Output.ofSecret(getBoolean(key));
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        return get(key).map(string -> {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ex) {
                throw new ConfigTypeException(fullKey(key), string, "Integer", ex);
            }
        });
    }

    @Override
    public Output<Optional<Integer>> getSecretInteger(String key) {
        return Output.ofSecret(getInteger(key));
    }

    @Override
    public <T> Optional<T> getObject(String key, Class<T> classOfT) {
        var v = get(key);
        try {
            var gson = new Gson();
            return v.map(string -> gson.fromJson(string, classOfT));
        } catch (JsonParseException ex) {
            throw new ConfigTypeException(fullKey(key), v, classOfT.getTypeName(), ex);
        }
    }

    @Override
    public <T> Output<Optional<T>> getSecretObject(String key, Class<T> classOfT) {
        return Output.ofSecret(getObject(key, classOfT));
    }

    @Override
    public <T> Optional<T> getObject(String key, TypeShape<T> shapeOfT) {
        var v = get(key);
        try {
            var gson = new Gson();
            return v.map(string -> gson.fromJson(string, shapeOfT.toGSON().getType()));
        } catch (JsonParseException ex) {
            throw new ConfigTypeException(fullKey(key), v, shapeOfT.getTypeName(), ex);
        }
    }

    @Override
    public <T> Output<Optional<T>> getSecretObject(String key, TypeShape<T> shapeOfT) {
        return Output.ofSecret(getObject(key, shapeOfT));
    }

    @Override
    public String require(String key) {
        return get(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    @Override
    public Output<String> requireSecret(String key) {
        return Output.ofSecret(require(key));
    }

    @Override
    public boolean requireBoolean(String key) {
        return getBoolean(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    @Override
    public Output<Boolean> requireSecretBoolean(String key) {
        return Output.ofSecret(requireBoolean(key));
    }

    @Override
    public int requireInteger(String key) {
        return getInteger(key).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    @Override
    public Output<Integer> requireSecretInteger(String key) {
        return Output.ofSecret(requireInteger(key));
    }

    @Override
    public <T> T requireObject(String key, Class<T> classOfT) {
        return getObject(key, classOfT).orElseThrow(() -> new ConfigMissingException(fullKey(key)));
    }

    @Override
    public <T> Output<T> requireSecretObject(String key, Class<T> classOfT) {
        return Output.ofSecret(requireObject(key, classOfT));
    }

    /**
     * Turns a simple configuration key into a fully resolved one, by prepending the bag's name.
     */
    private String fullKey(String key) {
        return String.format("%s:%s", this.name, key);
    }

    private Optional<String> getConfig(String fullKey) {
        return Optional.ofNullable(this.allConfig.getOrDefault(fullKey, null));
    }

    private boolean isConfigSecret(String fullKey) {
        return this.configSecretKeys.contains(fullKey);
    }

    // Static section -----

    @InternalUse
    @VisibleForTesting
    static ImmutableMap<String, String> parseConfig(String envConfigJson) {
        var parsedConfig = ImmutableMap.<String, String>builder();

        var gson = new Gson();
        var envObject = gson.fromJson(envConfigJson, JsonElement.class);
        for (var prop : envObject.getAsJsonObject().entrySet()) {
            parsedConfig.put(cleanKey(prop.getKey()), prop.getValue().getAsString());
        }

        return parsedConfig.build();
    }

    @InternalUse
    @VisibleForTesting
    static ImmutableSet<String> parseConfigSecretKeys(String envConfigSecretKeysJson) {
        var parsedConfigSecretKeys = ImmutableSet.<String>builder();

        var gson = new Gson();
        var envObject = gson.fromJson(envConfigSecretKeysJson, JsonElement.class);
        for (var element : envObject.getAsJsonArray()) {
            parsedConfigSecretKeys.add(element.getAsString());
        }

        return parsedConfigSecretKeys.build();
    }

    /**
     * CleanKey takes a configuration key, and if it is of the form "(string):config:(string)"
     * removes the ":config:" portion. Previously, our keys always had the string ":config:" in
     * them, and we'd like to remove it. However, the language host needs to continue to set it
     * so we can be compatible with older versions of our packages. Once we stop supporting
     * older packages, we can change the language host to not add this :config: thing and
     * remove this function.
     */
    private static String cleanKey(String key) {
        final var prefix = "config:";
        var idx = key.indexOf(":");
        if (idx > 0 && key.substring(idx + 1).startsWith(prefix)) {
            return key.substring(0, idx) + ":" + key.substring(idx + 1 + prefix.length());
        }
        return key;
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
    }
}