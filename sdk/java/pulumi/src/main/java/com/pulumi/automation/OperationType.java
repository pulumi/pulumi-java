// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * The type of operation being performed on a resource.
 */
public enum OperationType {
    UNKNOWN,
    SAME,
    CREATE,
    UPDATE,
    DELETE,
    REPLACE,
    CREATE_REPLACEMENT,
    DELETE_REPLACED,
    READ,
    READ_REPLACEMENT,
    REFRESH,
    READ_DISCARD,
    DISCARD_REPLACED,
    REMOVE_PENDING_REPLACE,
    IMPORT,
    IMPORT_REPLACEMENT,
}
