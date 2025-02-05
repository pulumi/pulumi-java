// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

/**
 * Thrown when creating a Workspace detects a conflict between
 * project settings found on disk (such as Pulumi.yaml) and a
 * ProjectSettings object passed to the Create API.
 *
 * There are two resolutions:
 *
 * (A) to use the ProjectSettings, delete the Pulumi.yaml file
 *     from WorkDir or use a different WorkDir
 *
 * (B) to use the exiting Pulumi.yaml from WorkDir, avoid
 *     customizing the ProjectSettings
 */
public class ProjectSettingsConflictException extends AutomationException {
    private final String settingsFileLocation;

    public ProjectSettingsConflictException(String settingsFileLocation) {
        super("Custom ProjectSettings passed in code conflict with settings found on disk: " + settingsFileLocation);
        this.settingsFileLocation = settingsFileLocation;
    }

    /**
     * FullPath of the Pulumi.yaml (or Pulumi.yml, Pulumi.json) settings file found on disk.
     *
     * @return The full path to the settings file
     */
    public String getSettingsFileLocation() {
        return settingsFileLocation;
    }
}
