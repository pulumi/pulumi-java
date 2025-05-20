package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ResourceSpec extends ObjectTypeSpec {
    @SerializedName("isComponent")
    private boolean isComponent;

    @SerializedName("inputProperties")
    private Map<String, PropertySpec> inputProperties;

    @SerializedName("requiredInputs")
    private Set<String> requiredInputs;

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

    public Map<String, PropertySpec> getInputProperties() {
        return inputProperties;
    }

    public Set<String> getRequiredInputs() {
        return requiredInputs;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(inputProperties, getProperties(), getRequired(), requiredInputs);
    }

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