package com.pulumi.automation;

/**
 * GetAllConfigOptions represents options for retrieving all config values.
 */
public class GetAllConfigOptions {
    private final boolean path;
    private final String configFile;
    private final boolean showSecrets;

    public GetAllConfigOptions(boolean path, String configFile, boolean showSecrets) {
        this.path = path;
        this.configFile = configFile;
        this.showSecrets = showSecrets;
    }

    public boolean getPath() { return path; }
    public String getConfigFile() { return configFile; }
    public boolean getShowSecrets() { return showSecrets; }
} 