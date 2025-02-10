// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PreludeEvent} is emitted at the start of an update.
 */
public class PreludeEvent {
    private final Map<String, String> config;

    public PreludeEvent(Map<String, String> config) {
        this.config = config != null ? Collections.unmodifiableMap(new HashMap<>(config)) : Collections.emptyMap();
    }

    /**
     * Gets the configuration keys and values for the update.
     * Encrypted configuration values may be blinded.
     *
     * @return an unmodifiable map of configuration keys and values
     */
    public Map<String, String> config() {
        return config;
    }
}
