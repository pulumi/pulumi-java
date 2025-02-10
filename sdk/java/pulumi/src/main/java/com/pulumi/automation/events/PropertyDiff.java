// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

/**
 * {@link PropertyDiff} describes the difference between a single property's old
 * and new values.
 */
public class PropertyDiff {
    private final DiffKind kind;
    private final boolean inputDiff;

    public PropertyDiff(DiffKind kind, boolean inputDiff) {
        this.kind = kind;
        this.inputDiff = inputDiff;
    }

    /**
     * Gets the kind of difference.
     *
     * @return the difference kind
     */
    public DiffKind kind() {
        return kind;
    }

    /**
     * True if this is a difference between old and new inputs rather
     * than old state and new inputs.
     *
     * @return true if this is a difference between old and new inputs
     */
    public boolean isInputDiff() {
        return inputDiff;
    }
}
