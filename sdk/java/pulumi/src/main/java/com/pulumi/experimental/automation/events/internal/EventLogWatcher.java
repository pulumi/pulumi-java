// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.events.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.experimental.automation.events.EngineEvent;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

/**
 * Watches a Pulumi engine log file for events and invokes a callback for each
 * event.
 */
@InternalUse
public final class EventLogWatcher implements AutoCloseable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final CompletableFuture<Void> future;

    public EventLogWatcher(Path logFile, Consumer<EngineEvent> onEvent) {
        this.future = CompletableFuture.runAsync(() -> {
            var serializer = new LocalSerializer();
            try (var reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                while (true) {
                    var line = reader.readLine();
                    if (line == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    if (!line.isBlank()) {
                        var event = serializer.deserializeJson(line, EngineEvent.class);
                        if (event != null) {
                            onEvent.accept(event);

                            // When we see the cancel event, we can stop watching the log file.
                            if (event.getCancelEvent() != null) {
                                break;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executorService);
    }

    @Override
    public void close() throws Exception {
        future.join();
        executorService.shutdown();
    }
}
