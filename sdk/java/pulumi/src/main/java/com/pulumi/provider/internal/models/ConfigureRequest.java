package com.pulumi.provider.internal.models;

import java.util.Map;

import com.pulumi.provider.internal.properties.PropertyValue;

/**
 * Represents a configuration request for a provider.
 */
public class ConfigureRequest {

    /**
     * The environment variables available to the provider.
     */
    private final Map<String, String> variables;

    /**
     * The configuration arguments provided to the provider.
     */
    private final Map<String, PropertyValue> args;

    /**
     * Indicates whether the provider should accept secret values in configuration.
     */
    private final boolean acceptSecrets;

    /**
     * Indicates whether the provider should accept resource references in configuration.
     */
    private final boolean acceptResources;

    /**
     * Constructs a new {@code ConfigureRequest} with the specified variables, arguments, and feature flags.
     *
     * @param variables the environment variables available to the provider
     * @param args the configuration arguments provided to the provider
     * @param acceptSecrets whether the provider should accept secret values
     * @param acceptResources whether the provider should accept resource references
     */
    public ConfigureRequest(Map<String, String> variables, Map<String, PropertyValue> args,
        boolean acceptSecrets, boolean acceptResources) {
        this.variables = variables;
        this.args = args;
        this.acceptSecrets = acceptSecrets;
        this.acceptResources = acceptResources;
    }

    /**
     * Gets the environment variables available to the provider.
     *
     * @return a map of environment variable names to values
     */
    public Map<String, String> getVariables() {
        return variables;
    }

    /**
     * Gets the configuration arguments provided to the provider.
     *
     * @return a map of argument names to {@link PropertyValue} instances
     */
    public Map<String, PropertyValue> getArgs() {
        return args;
    }

    /**
     * Indicates whether the provider should accept secret values in configuration.
     *
     * @return {@code true} if secrets are accepted, {@code false} otherwise
     */
    public boolean isAcceptSecrets() {
        return acceptSecrets;
    }

    /**
     * Indicates whether the provider should accept resource references in configuration.
     *
     * @return {@code true} if resource references are accepted, {@code false} otherwise
     */
    public boolean isAcceptResources() {
        return acceptResources;
    }
} 