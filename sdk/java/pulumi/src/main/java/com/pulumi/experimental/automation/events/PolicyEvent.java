// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events;

import javax.annotation.Nullable;

/**
 * {@link PolicyEvent} is emitted whenever there is Policy violation.
 */
public class PolicyEvent {
    @Nullable
    private final String resourceUrn;
    private final String message;
    private final String color;
    private final String policyName;
    private final String policyPackName;
    private final String policyPackVersion;
    private final String policyPackVersionTag;
    private final String enforcementLevel;

    public PolicyEvent(
            String resourceUrn,
            String message,
            String color,
            String policyName,
            String policyPackName,
            String policyPackVersion,
            String policyPackVersionTag,
            String enforcementLevel) {
        this.resourceUrn = resourceUrn;
        this.message = message;
        this.color = color;
        this.policyName = policyName;
        this.policyPackName = policyPackName;
        this.policyPackVersion = policyPackVersion;
        this.policyPackVersionTag = policyPackVersionTag;
        this.enforcementLevel = enforcementLevel;
    }

    /**
     * Gets the resource URN.
     *
     * @return the resource URN, may be null
     */
    @Nullable
    public String getResourceUrn() {
        return resourceUrn;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the color.
     *
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * Gets the policy name.
     *
     * @return the policy name
     */
    public String getPolicyName() {
        return policyName;
    }

    /**
     * Gets the policy pack name.
     *
     * @return the policy pack name
     */
    public String getPolicyPackName() {
        return policyPackName;
    }

    /**
     * Gets the policy pack version.
     *
     * @return the policy pack version
     */
    public String getPolicyPackVersion() {
        return policyPackVersion;
    }

    /**
     * Gets the policy pack version tag.
     *
     * @return the policy pack version tag
     */
    public String getPolicyPackVersionTag() {
        return policyPackVersionTag;
    }

    /**
     * Gets the enforcement level, one of "warning" or "mandatory".
     *
     * @return the enforcement level
     */
    public String getEnforcementLevel() {
        return enforcementLevel;
    }
}
