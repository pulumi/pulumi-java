// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

/**
 * {@link ResourceOperationFailedEvent} is emitted when a resource operation
 * fails. Typically a {@link DiagnosticEvent} is emitted before this event,
 * indicating what the root cause of the error.
 */
public class ResourceOperationFailedEvent {
    private final StepEventMetadata metadata;
    private final int status;
    private final int steps;

    public ResourceOperationFailedEvent(StepEventMetadata metadata, int status, int steps) {
        this.metadata = metadata;
        this.status = status;
        this.steps = steps;
    }

    /**
     * The step event metadata.
     *
     * @return the step event metadata
     */
    public StepEventMetadata metadata() {
        return metadata;
    }

    /**
     * The status code of the failure.
     *
     * @return the status code
     */
    public int status() {
        return status;
    }

    /**
     * The number of steps.
     *
     * @return the number of steps
     */
    public int steps() {
        return steps;
    }
}
