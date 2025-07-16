package com.pulumi.provider.internal.models;

/**
 * Represents the response to a configuration request for a provider.
 */
public class ConfigureResponse {
    /**
     * Indicates whether the provider accepts secret values in its configuration and resource properties.
     */
    private final boolean acceptSecrets;
    /**
     * Indicates whether the provider supports preview operations.
     */
    private final boolean supportsPreview;
    /**
     * Indicates whether the provider accepts resource references in its configuration.
     */
    private final boolean acceptResources;
    /**
     * Indicates whether the provider accepts output values in its configuration.
     */
    private final boolean acceptOutputs;

    /**
     * Constructs a new {@code ConfigureResponse} with the specified feature flags.
     *
     * @param acceptSecrets whether the provider accepts secret values
     * @param supportsPreview whether the provider supports preview operations
     * @param acceptResources whether the provider accepts resource references
     * @param acceptOutputs whether the provider accepts output values
     */
    public ConfigureResponse(boolean acceptSecrets, boolean supportsPreview,
        boolean acceptResources, boolean acceptOutputs) {
        this.acceptSecrets = acceptSecrets;
        this.supportsPreview = supportsPreview;
        this.acceptResources = acceptResources;
        this.acceptOutputs = acceptOutputs;
    }

    /**
     * Indicates whether the provider accepts secret values in its configuration and resource properties.
     *
     * @return {@code true} if secrets are accepted, {@code false} otherwise
     */
    public boolean isAcceptSecrets() {
        return acceptSecrets;
    }

    /**
     * Indicates whether the provider supports preview operations.
     *
     * @return {@code true} if preview is supported, {@code false} otherwise
     */
    public boolean isSupportsPreview() {
        return supportsPreview;
    }

    /**
     * Indicates whether the provider accepts resource references in its configuration.
     *
     * @return {@code true} if resource references are accepted, {@code false} otherwise
     */
    public boolean isAcceptResources() {
        return acceptResources;
    }

    /**
     * Indicates whether the provider accepts output values in its configuration.
     *
     * @return {@code true} if output values are accepted, {@code false} otherwise
     */
    public boolean isAcceptOutputs() {
        return acceptOutputs;
    }
} 