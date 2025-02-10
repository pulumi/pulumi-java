// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events;

import javax.annotation.Nullable;

/**
 * {@link ResourcePreEvent} is emitted before a resource is modified.
 */
public class ResourcePreEvent {
    private final StepEventMetadata metadata;
    @Nullable
    private final Boolean planning;

    public ResourcePreEvent(StepEventMetadata metadata, Boolean planning) {
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
    @Nullable
    public Boolean planning() {
        return planning;
    }
}
