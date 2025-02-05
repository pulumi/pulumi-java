// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.pulumi.experimental.automation.OperationType;

/**
 * {@link SummaryEvent} is emitted at the end of an update, with a summary of
 * the changes made.
 */
public class SummaryEvent {
    private final boolean maybeCorrupt;
    private final int durationSeconds;
    private final Map<OperationType, Integer> resourceChanges;
    private final Map<String, String> policyPacks;

    public SummaryEvent(
            boolean maybeCorrupt,
            int durationSeconds,
            Map<OperationType, Integer> resourceChanges,
            Map<String, String> policyPacks) {
        this.maybeCorrupt = maybeCorrupt;
        this.durationSeconds = durationSeconds;
        this.resourceChanges = resourceChanges != null
                ? Collections.unmodifiableMap(new HashMap<>(resourceChanges))
                : Collections.emptyMap();
        this.policyPacks = policyPacks != null
                ? Collections.unmodifiableMap(new HashMap<>(policyPacks))
                : Collections.emptyMap();
    }

    /**
     * Gets whether one or more of the resources is in an invalid state.
     *
     * @return true if one or more resources is in an invalid state, false otherwise
     */
    public boolean isMaybeCorrupt() {
        return maybeCorrupt;
    }

    /**
     * Gets the number of seconds the update was executing.
     *
     * @return duration in seconds
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Gets the count for resource change by type.
     *
     * @return an unmodifiable map of resource changes by operation type
     */
    public Map<OperationType, Integer> getResourceChanges() {
        return resourceChanges;
    }

    /**
     * Gets the policy packs run during update. Maps PolicyPackName to version.
     *
     * @return an unmodifiable map of policy pack names to versions
     */
    public Map<String, String> getPolicyPacks() {
        return policyPacks;
    }
}
