package com.pulumi.bootstrap.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents information about a parameterization for a Pulumi plugin.
 * <p>
 * Keep in sync with pulumi/sdk/go/common/resource/plugin/plugin.go's PulumiParameterizationJSON.
 */
public final class PulumiPluginParameterization {

    public final String name;
    public final String version;
    public final String value;

    public PulumiPluginParameterization(String name, String version, String value) {
        this.name = name;
        this.version = version;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PulumiPluginParameterization that = (PulumiPluginParameterization) o;
        return Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PulumiPluginParameterization.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("version='" + version + "'")
                .add("value='" + value + "'")
                .toString();
    }

    @Nullable
    @VisibleForTesting
    static PulumiPluginParameterization fromJson(String json) {
        var gson = new Gson();
        return gson.fromJson(json, PulumiPluginParameterization.class);
    }
}