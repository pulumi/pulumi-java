// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * {@link StepEventStateMetadata} is the more detailed state information for a
 * resource as it relates to a step(s) being performed.
 */
public class StepEventStateMetadata {
    private final String urn;
    private final String type;
    @Nullable
    private final Boolean custom;
    @Nullable
    private final Boolean delete;
    private final String id;
    private final String parent;
    @Nullable
    private final Boolean protect;
    private final Map<String, Object> inputs;
    private final Map<String, Object> outputs;
    private final String provider;
    private final List<String> initErrors;

    public StepEventStateMetadata(
            String urn,
            String type,
            Boolean custom,
            Boolean delete,
            String id,
            String parent,
            Boolean protect,
            Map<String, Object> inputs,
            Map<String, Object> outputs,
            String provider,
            List<String> initErrors) {
        this.urn = urn;
        this.type = type;
        this.custom = custom;
        this.delete = delete;
        this.id = id;
        this.parent = parent;
        this.protect = protect;
        this.inputs = inputs != null ? Collections.unmodifiableMap(new HashMap<>(inputs)) : Collections.emptyMap();
        this.outputs = outputs != null ? Collections.unmodifiableMap(new HashMap<>(outputs)) : Collections.emptyMap();
        this.provider = provider;
        this.initErrors = initErrors != null
                ? Collections.unmodifiableList(new ArrayList<>(initErrors))
                : Collections.emptyList();
    }

    public String urn() {
        return urn;
    }

    public String type() {
        return type;
    }

    /**
     * Gets whether the resource is managed by a plugin.
     *
     * @return true if managed by a plugin, may be null
     */
    @Nullable
    public Boolean custom() {
        return custom;
    }

    /**
     * Gets whether the resource is pending deletion due to a replacement.
     *
     * @return true if pending deletion, may be null
     */
    @Nullable
    public Boolean delete() {
        return delete;
    }

    /**
     * Gets the resource's unique ID, assigned by the resource provider (or blank if
     * none/uncreated).
     *
     * @return the resource ID
     */
    public String id() {
        return id;
    }

    /**
     * Gets the optional parent URN that this resource belongs to.
     *
     * @return the parent URN
     */
    public String parent() {
        return parent;
    }

    /**
     * Gets whether this resource is protected (protected resources cannot be
     * deleted).
     *
     * @return true if protected, may be null
     */
    @Nullable
    public Boolean protect() {
        return protect;
    }

    /**
     * Gets the resource's input properties (as specified by the program). Secrets
     * have filtered out, and large assets have been replaced by hashes as
     * applicable.
     *
     * @return an unmodifiable map of input properties
     */
    public Map<String, Object> inputs() {
        return inputs;
    }

    /**
     * Gets the resource's complete output state (as returned by the resource
     * provider).
     *
     * @return an unmodifiable map of output properties
     */
    public Map<String, Object> outputs() {
        return outputs;
    }

    /**
     * Gets the resource's provider reference.
     *
     * @return the provider reference
     */
    public String provider() {
        return provider;
    }

    /**
     * Gets the set of errors encountered in the process of initializing resource.
     *
     * @return an unmodifiable list of initialization errors, may be null
     */
    public List<String> initErrors() {
        return initErrors;
    }
}
