package com.pulumi.provider.internal.schema;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the specification for a provider package schema.
 *
 * @see ResourceSpec
 * @see ComplexTypeSpec
 */
public class PackageSpec {
    @SerializedName("name")
    private String name;

    @SerializedName("namespace")
    private String namespace;

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

    /**
     * Sets the name of the provider package.
     *
     * @param name the package name
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the namespace for the provider package.
     *
     * @param namespace the namespace for code generation and organization
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Sets the human-readable display name for the provider package.
     *
     * @param displayName the display name
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Sets the version of the provider package.
     *
     * @param version the version string
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the resource specifications for this package.
     *
     * @param resources a map of resource names to their specifications
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setResources(Map<String, ResourceSpec> resources) {
        this.resources = resources;
        return this;
    }

    /**
     * Sets the complex type specifications for this package.
     *
     * @param types a map of complex type names to their specifications
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setTypes(Map<String, ComplexTypeSpec> types) {
        this.types = types;
        return this;
    }

    /**
     * Sets the language-specific configuration for this package.
     *
     * @param language a map keyed by language name, each containing language-specific options
     * @return this {@code PackageSpec} instance for method chaining
     */
    public PackageSpec setLanguage(Map<String, Map<String, Object>> language) {
        this.language = language;
        return this;
    }

    /**
     * Gets the name of the provider package.
     *
     * @return the package name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the namespace for the provider package.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the human-readable display name for the provider package.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the version of the provider package.
     *
     * @return the version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the resource specifications for this package.
     *
     * @return a map of resource names to their specifications
     */
    public Map<String, ResourceSpec> getResources() {
        return resources;
    }

    /**
     * Gets the complex type specifications for this package.
     *
     * @return a map of complex type names to their specifications
     */
    public Map<String, ComplexTypeSpec> getTypes() {
        return types;
    }

    /**
     * Gets the language-specific configuration for this package.
     *
     * @return a map keyed by language name, each containing language-specific options
     */
    public Map<String, Map<String, Object>> getLanguage() {
        return language;
    }

    /**
     * Compares this package specification to another for equality.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
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

    /**
     * Computes the hash code for this package specification.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, version, resources, types, language);
    }

    /**
     * Returns a string representation of this package specification.
     *
     * @return a string describing the package specification
     */
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
