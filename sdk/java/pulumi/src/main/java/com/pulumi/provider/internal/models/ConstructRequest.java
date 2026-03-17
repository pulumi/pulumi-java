package com.pulumi.provider.internal.models;

import java.util.Map;

import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.resources.ComponentResourceOptions;

/**
 * Represents a request to construct a new component resource in a Pulumi provider.
 */
public class ConstructRequest {

    /**
     * The fully qualified type token of the resource to construct (e.g., {@code "my:module:ResourceType"}).
     */
    private final String type;

    /**
     * The logical name to assign to the constructed resource.
     */
    private final String name;

    /**
     * The input properties for the resource, as a map from property names to values.
     */
    private final Map<String, PropertyValue> inputs;

    /**
     * The options to use when constructing the component resource (e.g., parent, dependencies).
     */
    private final ComponentResourceOptions options;

    /**
     * Constructs a new {@code ConstructRequest} with the specified type, name, inputs, and options.
     *
     * @param type the fully qualified type token of the resource to construct
     * @param name the logical name to assign to the constructed resource
     * @param inputs the input properties for the resource
     * @param options the options to use when constructing the component resource
     */
    public ConstructRequest(String type, String name, Map<String, PropertyValue> inputs,
        ComponentResourceOptions options) {
        this.type = type;
        this.name = name;
        this.inputs = inputs;
        this.options = options;
    }

    /**
     * Gets the fully qualified type token of the resource to construct.
     *
     * @return the resource type token
     */
    public String getType() {
        return type;
    }

    /**
     * Get the logical name to assign to the constructed resource.
     *
     * @return the resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the input properties for the resource.
     *
     * @return a map of property names to {@link PropertyValue} instances
     */
    public Map<String, PropertyValue> getInputs() {
        return inputs;
    }

    /**
     * Get the options to use when constructing the component resource.
     *
     * @return the {@link ComponentResourceOptions} instance
     */
    public ComponentResourceOptions getOptions() {
        return options;
    }
} 