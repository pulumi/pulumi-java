package com.pulumi.provider.internal.models;

import java.util.Map;

import com.pulumi.provider.internal.properties.PropertyValue;

public class ConfigureRequest {
    private final Map<String, String> variables;
    private final Map<String, PropertyValue> args;
    private final boolean acceptSecrets;
    private final boolean acceptResources;

    public ConfigureRequest(Map<String, String> variables, Map<String, PropertyValue> args,
        boolean acceptSecrets, boolean acceptResources) {
        this.variables = variables;
        this.args = args;
        this.acceptSecrets = acceptSecrets;
        this.acceptResources = acceptResources;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, PropertyValue> getArgs() {
        return args;
    }

    public boolean isAcceptSecrets() {
        return acceptSecrets;
    }

    public boolean isAcceptResources() {
        return acceptResources;
    }
} 