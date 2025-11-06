// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.events.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import com.pulumi.automation.events.EngineEvent;
import com.pulumi.automation.serialization.internal.LocalSerializer;
import com.pulumi.core.internal.annotations.InternalUse;

import io.grpc.stub.StreamObserver;
import pulumirpc.EventsGrpc;
import pulumirpc.EventsOuterClass.EventRequest;

/**
 * gRPC server implementation for receiving engine events from the Pulumi engine.
 * This server implements the Events service defined in events.proto.
 */
@InternalUse
public class EventsServer extends EventsGrpc.EventsImplBase {
    private static final Logger logger = Logger.getLogger(EventsServer.class.getName());
    private final Consumer<EngineEvent> onEvent;
    private final LocalSerializer serializer;
    private final CompletableFuture<Void> completionFuture;

    public EventsServer(Consumer<EngineEvent> onEvent) {
        this.onEvent = onEvent;
        this.serializer = new LocalSerializer();
        this.completionFuture = new CompletableFuture<>();
    }

    /**
     * Return a future that completes when the event stream is finished.
     *
     * @return CompletableFuture that completes when the stream ends
     */
    public CompletableFuture<Void> getCompletionFuture() {
        return completionFuture;
    }

    @Override
    public StreamObserver<EventRequest> streamEvents(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<EventRequest>() {
            @Override
            public void onNext(EventRequest request) {
                try {
                    var eventJson = request.getEvent();
                    if (eventJson != null && !eventJson.isBlank()) {
                        var event = serializer.deserializeJson(eventJson, EngineEvent.class);
                        if (event != null) {
                            onEvent.accept(event);
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to parse engine event", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "Error in event stream", t);
                completionFuture.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
                completionFuture.complete(null);
            }
        };
    }
}
