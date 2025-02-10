// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import com.pulumi.automation.events.SummaryEvent;

/**
 * An exception thrown when a {@link SummaryEvent} is missing.
 */
public final class NoSummaryEventException extends MissingExpectedEventException {
    public NoSummaryEventException(String message) {
        super(SummaryEvent.class.getSimpleName(), message);
    }
}
