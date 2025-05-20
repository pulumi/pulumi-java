// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

/**
 * DiffKind describes the kind of a particular property diff.
 */
public enum DiffKind {
    /**
     * Add indicates that the property was added.
     */
    ADD,

    /**
     * AddReplace indicates that the property was added and requires that the
     * resource be replaced.
     */
    ADD_REPLACE,

    /**
     * Delete indicates that the property was deleted.
     */
    DELETE,

    /**
     * DeleteReplace indicates that the property was deleted and requires that the
     * resource be replaced.
     */
    DELETE_REPLACE,

    /**
     * Update indicates that the property was updated.
     */
    UPDATE,

    /**
     * UpdateReplace indicates that the property was updated and requires that the
     * resource be replaced.
     */
    UPDATE_REPLACE
}
