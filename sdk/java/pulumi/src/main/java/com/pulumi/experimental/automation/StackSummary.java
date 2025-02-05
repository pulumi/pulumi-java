// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.time.Instant;

/**
 * Information about a stack.
 */
public class StackSummary {
    private final String name;
    private final boolean isCurrent;
    private final Instant lastUpdate;
    private final boolean isUpdateInProgress;
    private final Integer resourceCount;
    private final String url;

    StackSummary(
            String name,
            boolean isCurrent,
            Instant lastUpdate,
            boolean isUpdateInProgress,
            Integer resourceCount,
            String url) {
        this.name = name;
        this.isCurrent = isCurrent;
        this.lastUpdate = lastUpdate;
        this.isUpdateInProgress = isUpdateInProgress;
        this.resourceCount = resourceCount;
        this.url = url;
    }

    /**
     * Returns the name of the stack.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the stack is the currently selected stack.
     *
     * @return true if the stack is the currently selected stack
     */
    public boolean isCurrent() {
        return isCurrent;
    }

    /**
     * Returns the time of the last update to the stack.
     *
     * @return the time of the last update
     */
    public Instant getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Returns whether an update is currently in progress for the stack.
     *
     * @return true if an update is in progress
     */
    public boolean isUpdateInProgress() {
        return isUpdateInProgress;
    }

    /**
     * Returns the number of resources in the stack.
     *
     * @return the number of resources
     */
    public Integer getResourceCount() {
        return resourceCount;
    }

    /**
     * Returns the URL of the stack.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }
}
