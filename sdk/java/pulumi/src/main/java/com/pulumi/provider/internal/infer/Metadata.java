package com.pulumi.provider.internal.infer;

public class Metadata {
    private final String name;
    private final String version;
    private final String displayName;

    public Metadata(String name) {
        this(name, null, null);
    }

    public Metadata(String name, String version) {
        this(name, version, null);
    }

    public Metadata(String name, String version, String displayName) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
        this.version = version;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }
} 