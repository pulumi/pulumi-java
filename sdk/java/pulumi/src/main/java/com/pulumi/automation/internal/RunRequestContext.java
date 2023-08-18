package com.pulumi.automation.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class RunRequestContext {
    private final String engineAddress;
    private final String monitorAddress;
    private final ImmutableMap<String, String> configMap;
    private final ImmutableSet<String> configSecretKeys;
    private final String project;
    private final String stack;
    private final boolean dryRun;

    public RunRequestContext(
            String engineAddress,
            String monitorAddress,
            Map<String, String> configMap,
            List<String> configSecretKeys,
            String project,
            String stack,
            boolean dryRun
    ) {
        this.engineAddress = requireNonNull(engineAddress);
        this.monitorAddress = requireNonNull(monitorAddress);
        this.configMap = ImmutableMap.copyOf(configMap);
        this.configSecretKeys = ImmutableSet.copyOf(configSecretKeys);
        this.project = requireNonNull(project);
        this.stack = requireNonNull(stack);
        this.dryRun = dryRun;
    }

    public String engineAddress() {
        return this.engineAddress;
    }

    public String monitorAddress() {
        return this.monitorAddress;
    }

    public ImmutableMap<String, String> configMap() {
        return this.configMap;
    }

    public ImmutableSet<String> configSecretKeys() {
        return this.configSecretKeys;
    }

    public String project() {
        return this.project;
    }

    public String stack() {
        return this.stack;
    }

    public boolean dryRun() {
        return this.dryRun;
    }
}
