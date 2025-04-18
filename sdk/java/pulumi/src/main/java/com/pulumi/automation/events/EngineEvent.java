// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

import javax.annotation.Nullable;

/**
 * {@link EngineEvent} describes a Pulumi engine event, such as a change to a
 * resource or diagnostic message. EngineEvent is a discriminated union of all
 * possible event types, and exactly one field will be non-null.
 */
public class EngineEvent {
    private final int sequence;
    private final int timestamp;
    @Nullable
    private final CancelEvent cancelEvent;
    @Nullable
    private final StandardOutputEvent stdoutEvent;
    @Nullable
    private final DiagnosticEvent diagnosticEvent;
    @Nullable
    private final PreludeEvent preludeEvent;
    @Nullable
    private final SummaryEvent summaryEvent;
    @Nullable
    private final ResourcePreEvent resourcePreEvent;
    @Nullable
    private final ResourceOutputsEvent resOutputsEvent;
    @Nullable
    private final ResourceOperationFailedEvent resOpFailedEvent;
    @Nullable
    private final PolicyEvent policyEvent;

    public EngineEvent(
            int sequence,
            int timestamp,
            CancelEvent cancelEvent,
            StandardOutputEvent stdoutEvent,
            DiagnosticEvent diagnosticEvent,
            PreludeEvent preludeEvent,
            SummaryEvent summaryEvent,
            ResourcePreEvent resourcePreEvent,
            ResourceOutputsEvent resOutputsEvent,
            ResourceOperationFailedEvent resOpFailedEvent,
            PolicyEvent policyEvent) {
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.cancelEvent = cancelEvent;
        this.stdoutEvent = stdoutEvent;
        this.diagnosticEvent = diagnosticEvent;
        this.preludeEvent = preludeEvent;
        this.summaryEvent = summaryEvent;
        this.resourcePreEvent = resourcePreEvent;
        this.resOutputsEvent = resOutputsEvent;
        this.resOpFailedEvent = resOpFailedEvent;
        this.policyEvent = policyEvent;
    }

    /**
     * Gets the sequence number, a unique, and monotonically increasing number for
     * each engine event sent to the Pulumi Service. Since events may be sent
     * concurrently, and/or delayed via network routing, the sequence number is
     * to ensure events can be placed into a total ordering.
     *
     * <ul>
     * <li>No two events can have the same sequence number.</li>
     * <li>Events with a lower sequence number must have been emitted before those
     * with a higher sequence number.</li>
     * </ul>
     *
     * @return the sequence number
     */
    public int sequence() {
        return sequence;
    }

    /**
     * Gets the timestamp, a Unix timestamp (seconds) of when the event was emitted.
     *
     * @return the timestamp
     */
    public int timestamp() {
        return timestamp;
    }

    @Nullable
    public CancelEvent cancelEvent() {
        return cancelEvent;
    }

    @Nullable
    public StandardOutputEvent standardOutputEvent() {
        return stdoutEvent;
    }

    @Nullable
    public DiagnosticEvent diagnosticEvent() {
        return diagnosticEvent;
    }

    @Nullable
    public PreludeEvent preludeEvent() {
        return preludeEvent;
    }

    @Nullable
    public SummaryEvent summaryEvent() {
        return summaryEvent;
    }

    @Nullable
    public ResourcePreEvent resourcePreEvent() {
        return resourcePreEvent;
    }

    @Nullable
    public ResourceOutputsEvent resourceOutputsEvent() {
        return resOutputsEvent;
    }

    @Nullable
    public ResourceOperationFailedEvent resourceOperationFailedEvent() {
        return resOpFailedEvent;
    }

    @Nullable
    public PolicyEvent policyEvent() {
        return policyEvent;
    }
}
