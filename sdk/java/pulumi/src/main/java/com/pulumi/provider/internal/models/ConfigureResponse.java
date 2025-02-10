package com.pulumi.provider.internal.models;

public class ConfigureResponse {
    private final boolean acceptSecrets;
    private final boolean supportsPreview;
    private final boolean acceptResources;
    private final boolean acceptOutputs;

    public ConfigureResponse(boolean acceptSecrets, boolean supportsPreview,
        boolean acceptResources, boolean acceptOutputs) {
        this.acceptSecrets = acceptSecrets;
        this.supportsPreview = supportsPreview;
        this.acceptResources = acceptResources;
        this.acceptOutputs = acceptOutputs;
    }

    public boolean isAcceptSecrets() {
        return acceptSecrets;
    }

    public boolean isSupportsPreview() {
        return supportsPreview;
    }

    public boolean isAcceptResources() {
        return acceptResources;
    }

    public boolean isAcceptOutputs() {
        return acceptOutputs;
    }
} 