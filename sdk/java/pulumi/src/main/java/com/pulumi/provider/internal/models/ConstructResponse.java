package com.pulumi.provider.internal.models;

import java.util.Map;
import java.util.Set;

import com.pulumi.provider.internal.properties.PropertyValue;

/**
 * Represents the response to a construct request for a component resource.
 */
public class ConstructResponse {

    /**
     * The URN of the constructed resource.
     */
    private final String urn;

    /**
     * The output state of the constructed resource, as a map from property names to values.
     */
    private final Map<String, PropertyValue> state;

    /**
     * The dependencies for each output property, as a map from property names to sets of dependent URNs.
     */
    private final Map<String, Set<String>> stateDependencies;

    /**
     * Constructs a new {@code ConstructResponse} with the specified URN, state, and state dependencies.
     *
     * @param urn the URN of the constructed resource
     * @param state the output state of the resource
     * @param stateDependencies the dependencies for each output property
     */
    public ConstructResponse(String urn, Map<String, PropertyValue> state, Map<String, Set<String>> stateDependencies) {
        this.urn = urn;
        this.state = state;
        this.stateDependencies = stateDependencies;
    }

    /**
     * Get the URN of the constructed resource.
     *
     * @return the resource URN
     */
    public String getUrn() {
        return urn;
    }

    /**
     * Get the output state of the constructed resource.
     *
     * @return a map of property names to {@link PropertyValue} instances
     */
    public Map<String, PropertyValue> getState() {
        return state;
    }

    /**
     * Get the dependencies for each output property.
     *
     * @return a map from property names to sets of dependent URNs
     */
    public Map<String, Set<String>> getStateDependencies() {
        return stateDependencies;
    }
} 