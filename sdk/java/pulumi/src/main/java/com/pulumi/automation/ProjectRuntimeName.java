// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * Supported Pulumi program language runtimes.
 */
public enum ProjectRuntimeName {
    NODEJS("nodejs"),
    GO("go"),
    PYTHON("python"),
    DOTNET("dotnet"),
    YAML("yaml"),
    JAVA("java");

    private final String value;

    ProjectRuntimeName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProjectRuntimeName fromString(String value) {
        for (var runtime : ProjectRuntimeName.values()) {
            if (runtime.value.equalsIgnoreCase(value)) {
                return runtime;
            }
        }
        throw new IllegalArgumentException("Unknown runtime: " + value);
    }
}
