// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public enum OperationType {
    UNKNOWN(-1),
    SAME(0),
    CREATE(1),
    UPDATE(2),
    DELETE(3),
    REPLACE(4),
    CREATE_REPLACEMENT(5),
    DELETE_REPLACED(6),
    READ(7),
    READ_REPLACEMENT(8),
    REFRESH(9),
    READ_DISCARD(10),
    DISCARD_REPLACED(11),
    REMOVE_PENDING_REPLACE(12),
    IMPORT(13),
    IMPORT_REPLACEMENT(14);

    private final int value;

    OperationType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
