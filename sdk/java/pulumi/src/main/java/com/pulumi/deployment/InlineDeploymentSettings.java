package com.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Settings for creating an inline deployment.
 */
public class InlineDeploymentSettings {
    private final String monitorAddr;
    private final String engineAddr;
    private final String project;
    private final String stack;
    @Nullable
    private final String organization;
    private final boolean isDryRun;
    @Nullable
    private final Logger logger;
    private final ImmutableMap<String, String> config;
    private final ImmutableSet<String> configSecretKeys;

    private InlineDeploymentSettings(Builder builder) {
        this.monitorAddr = builder.monitorAddr;
        this.engineAddr = builder.engineAddr;
        this.project = builder.project;
        this.stack = builder.stack;
        this.organization = builder.organization;
        this.isDryRun = builder.isDryRun;
        this.logger = builder.logger;
        this.config = builder.config;
        this.configSecretKeys = builder.configSecretKeys;
    }

    public String getMonitorAddr() {
        return monitorAddr;
    }

    public String getEngineAddr() {
        return engineAddr;
    }

    public String getProject() {
        return project;
    }

    public String getStack() {
        return stack;
    }

    public Optional<String> getOrganization() {
        return Optional.ofNullable(organization);
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public Optional<Logger> getLogger() {
        return Optional.ofNullable(logger);
    }

    public ImmutableMap<String, String> getConfig() {
        return config;
    }

    public ImmutableSet<String> getConfigSecretKeys() {
        return configSecretKeys;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String monitorAddr;
        private String engineAddr;
        private String project;
        private String stack;
        @Nullable
        private String organization;
        private boolean isDryRun;
        @Nullable
        private Logger logger;
        private ImmutableMap<String, String> config = ImmutableMap.of();
        private ImmutableSet<String> configSecretKeys = ImmutableSet.of();

        public Builder monitorAddr(String monitorAddr) {
            this.monitorAddr = monitorAddr;
            return this;
        }

        public Builder engineAddr(String engineAddr) {
            this.engineAddr = engineAddr;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder stack(String stack) {
            this.stack = stack;
            return this;
        }

        public Builder organization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder isDryRun(boolean isDryRun) {
            this.isDryRun = isDryRun;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder config(ImmutableMap<String, String> config) {
            this.config = config;
            return this;
        }

        public Builder configSecretKeys(ImmutableSet<String> configSecretKeys) {
            this.configSecretKeys = configSecretKeys;
            return this;
        }

        public InlineDeploymentSettings build() {
            if (monitorAddr == null || engineAddr == null || project == null || stack == null) {
                throw new IllegalStateException("Required fields monitorAddr, engineAddr, project, and stack must be set");
            }
            return new InlineDeploymentSettings(this);
        }
    }
} 