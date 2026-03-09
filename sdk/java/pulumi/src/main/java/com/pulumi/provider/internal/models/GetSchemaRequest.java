package com.pulumi.provider.internal.models;

/**
 * Represents a request to retrieve the schema for a provider or one of its subpackages.
 *
 * @see GetSchemaResponse
 */
public class GetSchemaRequest {

    /**
     * The version of the schema being requested.
     */
    private final int version;

    /**
     * The name of the subpackage for which the schema is requested, or {@code null} for the main package.
     */
    private final String subpackageName;

    /**
     * The version of the subpackage being requested, or {@code null} if not applicable.
     */
    private final String subpackageVersion;

    /**
     * Constructs a new {@code GetSchemaRequest} with the specified version and subpackage details.
     *
     * @param version the version of the schema being requested
     * @param subpackageName the name of the subpackage, or {@code null} for the main package
     * @param subpackageVersion the version of the subpackage, or {@code null} if not applicable
     */
    public GetSchemaRequest(int version, String subpackageName, String subpackageVersion) {
        this.version = version;
        this.subpackageName = subpackageName;
        this.subpackageVersion = subpackageVersion;
    }

    /**
     * Gets the version of the schema being requested.
     *
     * @return the schema version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Gets the name of the subpackage for which the schema is requested.
     *
     * @return the subpackage name, or {@code null} for the main package
     */
    public String getSubpackageName() {
        return subpackageName;
    }

    /**
     * Gets the version of the subpackage being requested.
     *
     * @return the subpackage version, or {@code null} if not applicable
     */
    public String getSubpackageVersion() {
        return subpackageVersion;
    }
} 