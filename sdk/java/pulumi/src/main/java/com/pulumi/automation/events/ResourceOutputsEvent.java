// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

import javax.annotation.Nullable;

/**
 * {@link ResourceOutputsEvent} is emitted when a resource is finished being
 * provisioned.
 */
public class ResourceOutputsEvent {
    private final StepEventMetadata metadata;
    @Nullable
    private final Boolean planning;

    public ResourceOutputsEvent(StepEventMetadata metadata, Boolean planning) {
        this.metadata = metadata;
        this.planning = planning;
    }

    /**
     * Gets the step event metadata.
     *
     * @return The step event metadata
     */
    public StepEventMetadata metadata() {
        return metadata;
    }

    /**
     * Gets the planning flag.
     *
     * @return The planning flag
     */
    public Boolean planning() {
        return planning;
    }
}
