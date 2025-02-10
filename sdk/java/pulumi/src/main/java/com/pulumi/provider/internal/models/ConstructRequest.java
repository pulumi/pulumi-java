package com.pulumi.provider.internal.models;

import java.util.Map;

import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.resources.ComponentResourceOptions;

public class ConstructRequest {
    private final String type;
    private final String name;
    private final Map<String, PropertyValue> inputs;
    private final ComponentResourceOptions options;

    public ConstructRequest(String type, String name, Map<String, PropertyValue> inputs,
        ComponentResourceOptions options) {
        this.type = type;
        this.name = name;
        this.inputs = inputs;
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Map<String, PropertyValue> getInputs() {
        return inputs;
    }

    public ComponentResourceOptions getOptions() {
        return options;
    }
} 