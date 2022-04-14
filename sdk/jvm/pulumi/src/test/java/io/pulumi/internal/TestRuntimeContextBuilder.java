package io.pulumi.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.logging.Level;

public class TestRuntimeContextBuilder {

    private String projectName = "project";
    private String stackName = "stack";
    private boolean preview = true;
    private boolean dryRun = true;
    private ImmutableMap<String, String> config = ImmutableMap.of();
    private ImmutableSet<String> configSecretKeys = ImmutableSet.of();
    private Level globalLoggingLevel = Level.SEVERE;
    private boolean disableResourceReferences = false;
    private boolean excessiveDebugOutput = false;
    private int taskTimeoutInMillis = -1;

    public TestRuntimeContextBuilder setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public TestRuntimeContextBuilder setStackName(String stackName) {
        this.stackName = stackName;
        return this;
    }

    public TestRuntimeContextBuilder setPreview(boolean preview) {
        return setDryRun(preview);
    }

    public TestRuntimeContextBuilder setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public TestRuntimeContextBuilder setConfig(ImmutableMap<String, String> config) {
        this.config = config;
        return this;
    }

    public TestRuntimeContextBuilder setConfigSecretKeys(ImmutableSet<String> configSecretKeys) {
        this.configSecretKeys = configSecretKeys;
        return this;
    }

    public TestRuntimeContextBuilder setGlobalLoggingLevel(Level globalLoggingLevel) {
        this.globalLoggingLevel = globalLoggingLevel;
        return this;
    }

    public TestRuntimeContextBuilder setDisableResourceReferences(boolean disableResourceReferences) {
        this.disableResourceReferences = disableResourceReferences;
        return this;
    }

    public TestRuntimeContextBuilder setExcessiveDebugOutput(boolean excessiveDebugOutput) {
        this.excessiveDebugOutput = excessiveDebugOutput;
        return this;
    }

    public TestRuntimeContextBuilder setTaskTimeoutInMillis(int taskTimeoutInMillis) {
        this.taskTimeoutInMillis = taskTimeoutInMillis;
        return this;
    }

    public TestRuntimeContext build() {
        return new TestRuntimeContext(
                projectName, stackName, dryRun, config, configSecretKeys,
                globalLoggingLevel, disableResourceReferences, excessiveDebugOutput, taskTimeoutInMillis
        );
    }
}