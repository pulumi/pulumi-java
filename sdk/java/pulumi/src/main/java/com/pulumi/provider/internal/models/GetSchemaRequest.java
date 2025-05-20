package com.pulumi.provider.internal.models;

public class GetSchemaRequest {
    private final int version;
    private final String subpackageName;
    private final String subpackageVersion;

    public GetSchemaRequest(int version, String subpackageName, String subpackageVersion) {
        this.version = version;
        this.subpackageName = subpackageName;
        this.subpackageVersion = subpackageVersion;
    }

    public int getVersion() {
        return version;
    }

    public String getSubpackageName() {
        return subpackageName;
    }

    public String getSubpackageVersion() {
        return subpackageVersion;
    }
} 