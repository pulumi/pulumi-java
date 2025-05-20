// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.pulumi.automation.OperationType;

/**
 * {@link StepEventMetadata} describes a "step" within the Pulumi engine, which
 * is any concrete action to migrate a set of cloud resources from one state to
 * another.
 */
public class StepEventMetadata {
    private final OperationType op;
    private final String urn;
    private final String type;
    @Nullable
    private final StepEventStateMetadata old;
    @Nullable
    @SerializedName("new")
    private final StepEventStateMetadata newState;
    private final List<String> keys;
    private final List<String> diffs;
    private final Map<String, PropertyDiff> detailedDiff;
    @Nullable
    private final Boolean logical;
    private final String provider;

    public StepEventMetadata(
            OperationType op,
            String urn,
            String type,
            StepEventStateMetadata old,
            StepEventStateMetadata newState,
            List<String> keys,
            List<String> diffs,
            Map<String, PropertyDiff> detailedDiff,
            Boolean logical,
            String provider) {
        this.op = op;
        this.urn = urn;
        this.type = type;
        this.old = old;
        this.newState = newState;
        this.keys = keys != null ? Collections.unmodifiableList(new ArrayList<>(keys)) : Collections.emptyList();
        this.diffs = diffs != null ? Collections.unmodifiableList(new ArrayList<>(diffs)) : Collections.emptyList();
        this.detailedDiff = detailedDiff != null
                ? Collections.unmodifiableMap(new HashMap<>(detailedDiff))
                : Collections.emptyMap();
        this.logical = logical;
        this.provider = provider;
    }

    /**
     * Gets the operation being performed.
     *
     * @return the operation type
     */
    public OperationType op() {
        return op;
    }

    public String urn() {
        return urn;
    }

    public String type() {
        return type;
    }

    /**
     * Gets the state of the resource before performing the step.
     *
     * @return the old state metadata, may be null
     */
    @Nullable
    public StepEventStateMetadata old() {
        return old;
    }

    /**
     * Gets the state of the resource after performing the step.
     *
     * @return the new state metadata, may be null
     */
    @Nullable
    public StepEventStateMetadata new_() {
        return newState;
    }

    /**
     * Gets the keys causing a replacement (only applicable for "create" and
     * "replace" Ops).
     *
     * @return an unmodifiable list of keys, may be null
     */
    public List<String> keys() {
        return keys;
    }

    /**
     * Gets the keys that changed with this step.
     *
     * @return an unmodifiable list of changed keys, may be null
     */
    public List<String> diffs() {
        return diffs;
    }

    /**
     * Gets the diff for this step as a list of property paths and difference types.
     *
     * @return an unmodifiable map of detailed differences, may be null
     */
    public Map<String, PropertyDiff> detailedDiff() {
        return detailedDiff;
    }

    /**
     * Gets whether the step is a logical operation in the program.
     *
     * @return true if the step is logical, may be null
     */
    @Nullable
    public Boolean logical() {
        return logical;
    }

    /**
     * Gets the provider actually performing the step.
     *
     * @return the provider
     */
    public String provider() {
        return provider;
    }
}
