// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.time.Instant;

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

    public String getName() {
        return name;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public boolean isUpdateInProgress() {
        return isUpdateInProgress;
    }

    public Integer getResourceCount() {
        return resourceCount;
    }

    public String getUrl() {
        return url;
    }
}
