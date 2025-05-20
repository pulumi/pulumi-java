// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * The kind of update being performed.
 */
public enum UpdateKind {
    UPDATE,
    PREVIEW,
    REFRESH,
    RENAME,
    DESTROY,
    IMPORT,
    RESOURCE_IMPORT
}
