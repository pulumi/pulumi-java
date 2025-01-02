package com.pulumi.bootstrap.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pulumi.core.internal.Optionals;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents additional information about a package's associated Pulumi plugin.
 * For Java, the content is inside com/pulumi/{@literal <provider>}/plugin.json file inside the package.
 * <p>
 * Keep in sync with pulumi/sdk/go/common/resource/plugin/plugin.go's PulumiPluginJSON.
 */
public final class PulumiPlugin {

    public final boolean resource;
    @Nullable
    public final String name;
    @Nullable
    public final String version;
    @Nullable
    public final String server;
    @Nullable
    public final PulumiPluginParameterization parameterization;

    /**
     * Represents additional information about a package's associated Pulumi plugin.
     *
     * @param resource
     *  Indicates whether the package has an associated resource plugin. Set to false indicates no plugin.
     * @param name
     *  Optional plugin name. If not set, the plugin name is derived from the package name.
     * @param version
     *  Optional plugin version. If not set, the version is derived from the package version (if possible).
     * @param server
     *  Optional plugin server. If not set, the default server is used when installing the plugin.
     * @param parameterization
     *  Optional parameterization information for the plugin.
     */
    public PulumiPlugin(
        boolean resource,
        @Nullable String name,
        @Nullable String version,
        @Nullable String server,
        @Nullable PulumiPluginParameterization parameterization
    ) {
        this.resource = resource;
        this.name = name;
        this.version = version;
        this.server = server;
        this.parameterization = parameterization;
    }

    public static PulumiPlugin resolve(@Nullable PulumiPlugins.RawResource plugin, @Nullable PulumiPlugins.RawResource rawVersion) {
        if (plugin == null && rawVersion == null) {
            throw new IllegalStateException("Either plugin or version file must be present in order to trigger this code. This should never happen. It's a bug.");
        }
        var maybePlugin = Optional.ofNullable(plugin).map(
                p -> Map.entry(p.name, Optional.ofNullable(fromJson(p.content)))
        );
        var maybeVersion = Optional.ofNullable(rawVersion).map(
                v -> Map.entry(v.name, Optionals.ofBlank(v.content))
        );
        var resource = maybePlugin
                .flatMap(Map.Entry::getValue)
                .map(pulumiPlugin -> pulumiPlugin.resource)
                .orElse(false);
        var name = maybePlugin
                .flatMap(Map.Entry::getValue)
                .map(p -> p.name)
                .or(() -> maybePlugin.map(Map.Entry::getKey))
                .or(() -> maybeVersion.map(Map.Entry::getKey))
                .orElse(null);
        var version = maybePlugin
                .flatMap(Map.Entry::getValue)
                .map(p -> p.version)
                .or(() -> maybeVersion.flatMap(Map.Entry::getValue))
                .orElse(null);
        var server = maybePlugin
                .flatMap(Map.Entry::getValue)
                .map(pulumiPlugin -> pulumiPlugin.server)
                .orElse(null);
        var parameterization = maybePlugin
                .flatMap(Map.Entry::getValue)
                .map(pulumiPlugin -> pulumiPlugin.parameterization)
                .orElse(null);
        return new PulumiPlugin(resource, name, version, server, parameterization);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PulumiPlugin that = (PulumiPlugin) o;
        return resource == that.resource
                && Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Objects.equals(server, that.server)
                && Objects.equals(parameterization, that.parameterization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, name, version, server, parameterization);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PulumiPlugin.class.getSimpleName() + "[", "]")
                .add("resource=" + resource)
                .add("name='" + name + "'")
                .add("version='" + version + "'")
                .add("server='" + server + "'")
                .add("parameterization=" + parameterization)
                .toString();
    }

    @Nullable
    @VisibleForTesting
    static PulumiPlugin fromJson(String json) {
        var gson = new Gson();

        var plugin = gson.fromJson(json, PulumiPlugin.class);
        if (plugin == null) {
            return plugin;
        }

        var raw = gson.fromJson(json, JsonObject.class);
        var parameterization = raw.has("parameterization")
                ? gson.fromJson(raw.get("parameterization"), PulumiPluginParameterization.class)
                : null;

        plugin = new PulumiPlugin(plugin.resource, plugin.name, plugin.version, plugin.server, parameterization);
        return plugin;
    }
}