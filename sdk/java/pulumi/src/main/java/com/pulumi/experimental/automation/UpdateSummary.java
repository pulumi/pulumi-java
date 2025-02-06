// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Information about an update.
 */
public final class UpdateSummary {
    private final UpdateKind kind;
    private final Instant startTime;
    private final String message;
    private final Map<String, String> environment;
    private final Map<String, ConfigValue> config;
    private final UpdateState result;
    private final Instant endTime;
    private final Integer version;
    private final String deployment;
    private final Map<OperationType, Integer> resourceChanges;

    public UpdateSummary(
            UpdateKind kind,
            Instant startTime,
            String message,
            Map<String, String> environment,
            Map<String, ConfigValue> config,
            UpdateState result,
            Instant endTime,
            Integer version,
            String deployment,
            Map<OperationType, Integer> resourceChanges) {
        this.kind = kind;
        this.startTime = startTime;
        this.message = message;
        this.environment = environment != null ? Collections.unmodifiableMap(environment) : Collections.emptyMap();
        this.config = config != null ? Collections.unmodifiableMap(config) : Collections.emptyMap();
        this.result = result;
        this.endTime = endTime;
        this.version = version;
        this.deployment = deployment;
        this.resourceChanges = resourceChanges != null ? Collections.unmodifiableMap(resourceChanges) : null;
    }

    /**
     * Returns the kind of the update.
     *
     * @return the kind
     */
    public UpdateKind getKind() {
        return kind;
    }

    /**
     * Returns the start time of the update.
     *
     * @return the start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns the message associated with the update.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the environment that was used for the update.
     *
     * @return the environment
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * Returns the configuration that was used for the update.
     *
     * @return the configuration
     */
    public Map<String, ConfigValue> getConfig() {
        return config;
    }

    /**
     * Returns the result of the update.
     *
     * @return the result
     */
    public UpdateState getResult() {
        return result;
    }

    /**
     * Returns the end time of the update.
     *
     * @return the end time
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Returns the version of the stack after the update.
     *
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Returns a raw JSON blob detailing the deployment.
     *
     * @return the deployment
     */
    public String getDeployment() {
        return deployment;
    }

    /**
     * Returns the changes that were applied by the update.
     *
     * @return the changes
     */
    public Map<OperationType, Integer> getResourceChanges() {
        return resourceChanges;
    }
}
