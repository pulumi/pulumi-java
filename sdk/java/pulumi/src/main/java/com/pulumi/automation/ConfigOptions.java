package com.pulumi.automation;

/**
 * ConfigOptions represents options for config operations.
 */
public class ConfigOptions {
    private final boolean path;
    private final String configFile;
    private final boolean showSecrets;

    public ConfigOptions(boolean path, String configFile, boolean showSecrets) {
        this.path = path;
        this.configFile = configFile;
        this.showSecrets = showSecrets;
    }

    public boolean getPath() { return path; }
    public String getConfigFile() { return configFile; }
    public boolean getShowSecrets() { return showSecrets; }
} 