package com.pulumi.provider.internal;

/**
 * Represents metadata information for a Pulumi provider package.
 *
 * @see com.pulumi.provider.internal.ComponentProvider
 */
public class Metadata {
    /**
     * The unique name of the provider package.
     */
    private final String name;
    /**
     * The version of the provider package.
     */
    private final String version;
    /**
     * The human-readable display name for the provider package, or {@code null} if not specified.
     */
    private final String displayName;

    /**
     * Constructs a new {@code Metadata} instance with the specified name and default version/display name.
     *
     * @param name the unique name of the provider package
     * @throws IllegalArgumentException if {@code name} is null or empty
     */
    public Metadata(String name) {
        this(name, "0.0.0", null);
    }

    /**
     * Constructs a new {@code Metadata} instance with the specified name and version.
     *
     * @param name the unique name of the provider package
     * @param version the version of the provider package
     * @throws IllegalArgumentException if {@code name} or {@code version} is null or empty
     */
    public Metadata(String name, String version) {
        this(name, version, null);
    }

    /**
     * Constructs a new {@code Metadata} instance with the specified name, version, and display name.
     *
     * @param name the unique name of the provider package
     * @param version the version of the provider package
     * @param displayName the human-readable display name, or {@code null} if not specified
     * @throws IllegalArgumentException if {@code name} or {@code version} is null or empty
     */
    public Metadata(String name, String version, String displayName) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        this.name = name;
        this.version = version;
        this.displayName = displayName;
    }

    /**
     * Gets the unique name of the provider package.
     *
     * @return the provider name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the version of the provider package.
     *
     * @return the provider version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the human-readable display name for the provider package, or {@code null} if not specified.
     *
     * @return the display name, or {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }
} 