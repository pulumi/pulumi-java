package com.pulumi.provider.internal.models;

/**
 * Represents the response to a schema request for a Pulumi provider or subpackage.
 *
 * @see GetSchemaRequest
 */
public class GetSchemaResponse {
    /**
     * The JSON schema definition returned by the provider.
     */
    private final String schema;

    /**
     * Constructs a new {@code GetSchemaResponse} with the specified schema definition.
     *
     * @param schema the JSON schema definition as a string
     */
    public GetSchemaResponse(String schema) {
        this.schema = schema;
    }

    /**
     * Gets the JSON schema definition returned by the provider.
     *
     * @return the schema definition as a string
     */
    public String getSchema() {
        return schema;
    }
} 