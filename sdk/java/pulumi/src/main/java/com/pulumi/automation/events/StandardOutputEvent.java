// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events;

/**
 * {@link StandardOutputEvent} is emitted whenever a generic message is
 * written, for example warnings from the pulumi CLI itself. Less common than
 * {@link DiagnosticEvent}.
 */
public class StandardOutputEvent {
    private final String message;
    private final String color;

    public StandardOutputEvent(String message, String color) {
        this.message = message;
        this.color = color;
    }

    /**
     * Gets the message content.
     *
     * @return the message
     */
    public String message() {
        return message;
    }

    /**
     * Gets the color for display.
     *
     * @return the color
     */
    public String color() {
        return color;
    }
}
