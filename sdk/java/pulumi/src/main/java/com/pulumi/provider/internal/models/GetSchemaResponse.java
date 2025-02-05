package com.pulumi.provider.internal.models;

public class GetSchemaResponse {
    private final String schema;

    public GetSchemaResponse(String schema) {
        this.schema = schema;
    }

    public String getSchema() {
        return schema;
    }
} 