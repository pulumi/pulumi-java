package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ObjectTypeSpec {
    @SerializedName("type")
    private String type;

    @SerializedName("properties")
    private Map<String, PropertySpec> properties;

    @SerializedName("required")
    private Set<String> required;
    
    protected ObjectTypeSpec(
        String type,
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        this.type = type;
        this.properties = properties;
        this.required = new TreeSet<>(required);
    }

    public String getType() {
        return type;
    }

    public Map<String, PropertySpec> getProperties() {
        return properties;
    }

    public Set<String> getRequired() {
        return required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectTypeSpec that = (ObjectTypeSpec) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(properties, that.properties) &&
               Objects.equals(required, that.required);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, properties, required);
    }

    @Override
    public String toString() {
        return "ObjectTypeSpec{" +
               "type='" + type + '\'' +
               ", properties=" + properties +
               ", required=" + required +
               '}';
    }
} 