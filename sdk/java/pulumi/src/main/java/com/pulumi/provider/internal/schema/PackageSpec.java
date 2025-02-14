package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PackageSpec {
    @SerializedName("name")
    private String name;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("version")
    private String version;

    @SerializedName("resources")
    private Map<String, ResourceSpec> resources = new HashMap<>();

    @SerializedName("types")
    private Map<String, ComplexTypeSpec> types = new HashMap<>();

    @SerializedName("language")
    private Map<String, Map<String, Object>> language = new HashMap<>();

    public PackageSpec setName(String name) {
        this.name = name;
        return this;
    }

    public PackageSpec setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public PackageSpec setVersion(String version) {
        this.version = version;
        return this;
    }

    public PackageSpec setResources(Map<String, ResourceSpec> resources) {
        this.resources = resources;
        return this;
    }

    public PackageSpec setTypes(Map<String, ComplexTypeSpec> types) {
        this.types = types;
        return this;
    }

    public PackageSpec setLanguage(Map<String, Map<String, Object>> language) {
        this.language = language;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, ResourceSpec> getResources() {
        return resources;
    }

    public Map<String, ComplexTypeSpec> getTypes() {
        return types;
    }

    public Map<String, Map<String, Object>> getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageSpec that = (PackageSpec) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(displayName, that.displayName) &&
               Objects.equals(version, that.version) &&
               Objects.equals(resources, that.resources) &&
               Objects.equals(types, that.types) &&
               Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, version, resources, types, language);
    }

    @Override
    public String toString() {
        return "PackageSpec{" +
               "name='" + name + '\'' +
               ", displayName='" + displayName + '\'' +
               ", version='" + version + '\'' +
               ", resources=" + resources +
               ", types=" + types +
               ", language=" + language +
               '}';
    }
} 