// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events.internal;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.experimental.automation.events.EngineEvent;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * Watches a Pulumi engine log file for events and invokes a callback for each
 * event.
 */
@InternalUse
public final class EventLogWatcher implements AutoCloseable {
    private final LocalSerializer serializer = new LocalSerializer();
    private final Tailer tailer;

    public EventLogWatcher(Path logFile, Consumer<EngineEvent> onEvent) {
        var listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                if (line != null && !line.isBlank()) {
                    EngineEvent event = deserialize(line);
                    if (event != null) {
                        onEvent.accept(event);
                    }
                }
            }
        };

        this.tailer = Tailer.builder()
                .setPath(logFile)
                .setTailerListener(listener)
                .setDelayDuration(Duration.ofMillis(100))
                .setExecutorService(Executors.newSingleThreadExecutor())
                .setStartThread(true)
                .get();
    }

    @Override
    public void close() throws Exception {
        this.tailer.close();
    }

    private EngineEvent deserialize(String json) {
        return serializer.deserializeJson(json, EngineEvent.class);
    }
}
