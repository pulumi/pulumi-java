// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import com.pulumi.experimental.automation.events.SummaryEvent;

public final class NoSummaryEventException extends MissingExpectedEventException {
    public NoSummaryEventException(String message) {
        super(SummaryEvent.class.getSimpleName(), message);
    }
}
