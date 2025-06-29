package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents the specification for a resource in a provider schema.
 *
 * @see ObjectTypeSpec
 * @see PropertySpec
 */
public class ResourceSpec extends ObjectTypeSpec {
    @SerializedName("isComponent")
    private boolean isComponent;

    @SerializedName("inputProperties")
    private Map<String, PropertySpec> inputProperties;

    @SerializedName("requiredInputs")
    private Set<String> requiredInputs;

    /**
     * Constructs a new {@code ResourceSpec} with the specified input and output properties and required fields.
     *
     * @param inputProperties a map of input property names to their specifications
     * @param requiredInputs a set of required input property names
     * @param properties a map of output property names to their specifications
     * @param required a set of required output property names
     */
    public ResourceSpec(
            Map<String, PropertySpec> inputProperties,
            Set<String> requiredInputs,
            Map<String, PropertySpec> properties,
            Set<String> required) {
        super("object", properties, required);
        this.isComponent = true;
        this.inputProperties = inputProperties;
        this.requiredInputs = new TreeSet<>(requiredInputs);
    }

    /**
     * Gets the map of input property names to their specifications for this resource.
     *
     * @return a map of input property names to {@link PropertySpec} instances
     */
    public Map<String, PropertySpec> getInputProperties() {
        return inputProperties;
    }

    /**
     * Gets the set of required input property names for this resource.
     *
     * @return a set of required input property names
     */
    public Set<String> getRequiredInputs() {
        return requiredInputs;
    }

    /**
     * Compares this resource specification to another for equality.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceSpec that = (ResourceSpec) o;
        return Objects.equals(inputProperties, that.inputProperties) &&
               Objects.equals(getProperties(), that.getProperties()) &&
               Objects.equals(getRequired(), that.getRequired()) &&
               Objects.equals(requiredInputs, that.requiredInputs);
    }

    /**
     * Computes the hash code for this resource specification.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(inputProperties, getProperties(), getRequired(), requiredInputs);
    }

    /**
     * Returns a string representation of this resource specification.
     *
     * @return a string describing the resource specification
     */
    @Override
    public String toString() {
        return "ResourceSpec{" +
               "inputProperties=" + inputProperties +
               ", properties=" + getProperties() +
               ", required=" + getRequired() +
               ", requiredInputs=" + requiredInputs +
               '}';
    }
} 