// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

import javax.annotation.Nullable;

/**
 * {@link DiagnosticEvent} is emitted whenever a diagnostic message is provided,
 * for example errors from a cloud resource provider while trying to create or
 * update a resource.
 */
public class DiagnosticEvent {
    @Nullable
    private final String urn;
    @Nullable
    private final String prefix;
    private final String message;
    private final String color;
    private final String severity;
    @Nullable
    private final String streamId;
    @Nullable
    private final Boolean ephemeral;

    public DiagnosticEvent(
            String urn,
            String prefix,
            String message,
            String color,
            String severity,
            String streamId,
            Boolean ephemeral) {
        this.urn = urn;
        this.prefix = prefix;
        this.message = message;
        this.color = color;
        this.severity = severity;
        this.streamId = streamId;
        this.ephemeral = ephemeral;
    }

    /**
     * Gets the URN.
     *
     * @return the URN, may be null
     */
    @Nullable
    public String urn() {
        return urn;
    }

    /**
     * Gets the prefix.
     *
     * @return the prefix, may be null
     */
    @Nullable
    public String prefix() {
        return prefix;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String message() {
        return message;
    }

    /**
     * Gets the color.
     *
     * @return the color
     */
    public String color() {
        return color;
    }

    /**
     * Gets the severity, one of "info", "info#err", "warning", or "error".
     *
     * @return the severity
     */
    public String severity() {
        return severity;
    }

    /**
     * Gets the stream ID.
     *
     * @return the stream ID, may be null
     */
    @Nullable
    public String streamId() {
        return streamId;
    }

    /**
     * Gets whether this diagnostic is ephemeral.
     *
     * @return whether this diagnostic is ephemeral, may be null
     */
    @Nullable
    public Boolean ephemeral() {
        return ephemeral;
    }
}
