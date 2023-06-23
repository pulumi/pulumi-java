package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.pulumi.core.internal.Environment;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;

@ParametersAreNonnullByDefault
@InternalUse
public class Config {

    /**
     * The environment variable key that the language plugin uses to set configuration values.
     */
    private static final String ConfigEnvKey = "PULUMI_CONFIG";

    /**
     * The environment variable key that the language plugin uses to set the list of secret configuration keys.
     */
    private static final String ConfigSecretKeysEnvKey = "PULUMI_CONFIG_SECRET_KEYS";

    private final ImmutableMap<String, String> allConfig;

    private final ImmutableSet<String> configSecretKeys;

    @VisibleForTesting
    public Config(ImmutableMap<String, String> allConfig, ImmutableSet<String> configSecretKeys) {
        this.allConfig = Objects.requireNonNull(allConfig);
        this.configSecretKeys = Objects.requireNonNull(configSecretKeys);
    }

    @InternalUse
    static Config parse() {
        return new Config(parseConfig(), parseConfigSecretKeys());
    }

    /**
     * Returns a copy of the full config map.
     */
    @InternalUse
    private ImmutableMap<String, String> getAllConfig() {
        return allConfig;
    }

    /**
     * Returns a copy of the config secret keys.
     */
    @InternalUse
    private ImmutableSet<String> configSecretKeys() {
        return configSecretKeys;
    }

    public Optional<String> getConfig(String fullKey) {
        return Optional.ofNullable(this.allConfig.getOrDefault(fullKey, null));
    }

    public boolean isConfigSecret(String fullKey) {
        return this.configSecretKeys.contains(fullKey);
    }

    private static ImmutableMap<String, String> parseConfig() {
        var envConfig = Environment.getEnvironmentVariable(ConfigEnvKey);
        if (envConfig.isValue()) {
            return parseConfig(envConfig.value());
        }
        return ImmutableMap.of();
    }

    @InternalUse
    @VisibleForTesting
    public static ImmutableMap<String, String> parseConfig(String envConfigJson) {
        var parsedConfig = ImmutableMap.<String, String>builder();

        var gson = new Gson();
        var envObject = gson.fromJson(envConfigJson, JsonElement.class);
        for (var prop : envObject.getAsJsonObject().entrySet()) {
            parsedConfig.put(cleanKey(prop.getKey()), prop.getValue().getAsString());
        }

        return parsedConfig.build();
    }

    private static ImmutableSet<String> parseConfigSecretKeys() {
        var envConfigSecretKeys = Environment.getEnvironmentVariable(ConfigSecretKeysEnvKey);
        if (envConfigSecretKeys.isValue()) {
            return parseConfigSecretKeys(envConfigSecretKeys.value());
        }

        return ImmutableSet.of();
    }

    @InternalUse
    @VisibleForTesting
    public static ImmutableSet<String> parseConfigSecretKeys(String envConfigSecretKeysJson) {
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
}
