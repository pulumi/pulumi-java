package com.pulumi.provider.internal.models;

import java.util.Map;
import java.util.Set;

import com.pulumi.provider.internal.properties.PropertyValue;

public class ConstructResponse {
    private final String urn;
    private final Map<String, PropertyValue> state;
    private final Map<String, Set<String>> stateDependencies;

    public ConstructResponse(String urn, Map<String, PropertyValue> state, Map<String, Set<String>> stateDependencies) {
        this.urn = urn;
        this.state = state;
        this.stateDependencies = stateDependencies;
    }

    public String getUrn() {
        return urn;
    }

    public Map<String, PropertyValue> getState() {
        return state;
    }

    public Map<String, Set<String>> getStateDependencies() {
        return stateDependencies;
    }
} 