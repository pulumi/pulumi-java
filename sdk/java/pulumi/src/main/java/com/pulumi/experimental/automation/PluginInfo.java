// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.time.Instant;

import javax.annotation.Nullable;

/**
 * Information about a plugin.
 */
public class PluginInfo {
    private final String name;
    private final PluginKind kind;
    @Nullable
    private final String version;
    private final long size;
    private final Instant installTime;
    private final Instant lastUsedTime;

    PluginInfo(
            String name,
            String path,
            PluginKind kind,
            String version,
            long size,
            Instant installTime,
            Instant lastUsedTime) {
        this.name = name;
        this.kind = kind;
        this.version = version;
        this.size = size;
        this.installTime = installTime;
        this.lastUsedTime = lastUsedTime;
    }

    /**
     * The simple name of the plugin.
     *
     * @return the plugin name
     */
    public String getName() {
        return name;
    }

    /**
     * The kind of plugin.
     *
     * @return the plugin kind
     */
    public PluginKind getKind() {
        return kind;
    }

    /**
     * The plugin's semantic version, if present.
     *
     * @return the plugin version or {@code null}
     */
    @Nullable
    public String getVersion() {
        return version;
    }

    /**
     * The size of the plugin in bytes.
     *
     * @return the plugin size
     */
    public long getSize() {
        return size;
    }

    /**
     * The time the plugin was installed.
     *
     * @return the install time
     */
    public Instant getInstallTime() {
        return installTime;
    }

    /**
     * The last time the plugin was used.
     *
     * @return the last used time
     */
    public Instant getLastUsedTime() {
        return lastUsedTime;
    }
}
