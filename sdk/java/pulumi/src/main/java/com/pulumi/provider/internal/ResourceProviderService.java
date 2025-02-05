package com.pulumi.provider.internal;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.Empty;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.function.Function;

public class ResourceProviderService {

    private static final Logger logger = Logger.getLogger(ResourceProviderService.class.getName());

    private Server server;
    private final Provider implementation;

    public ResourceProviderService(Provider implementation) {
        this.implementation = implementation;
    }

    public void startAndBlockUntilShutdown() throws IOException, InterruptedException {
        start();
        blockUntilShutdown();
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(0) // Use port 0 to let system assign a free port
            .addService(new ResourceProviderImpl(this.implementation))
            .build()
            .start();
        
        // Print the actual bound port for the parent process to read
        System.out.println(server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    ResourceProviderService.this.stop();
                } catch (InterruptedException e) {
                    logger.severe(e.toString());
                }
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class ResourceProviderImpl extends pulumirpc.ResourceProviderGrpc.ResourceProviderImplBase {
        private final Provider implementation;

        public ResourceProviderImpl(Provider implementation) {
            this.implementation = implementation;
        }

        @Override
        public void getPluginInfo(Empty request, StreamObserver<pulumirpc.Plugin.PluginInfo> responseObserver) {
            // Return basic plugin information
            pulumirpc.Plugin.PluginInfo info = pulumirpc.Plugin.PluginInfo.newBuilder()
                .setVersion("1.0.0")
                .build();
            
            responseObserver.onNext(info);
            responseObserver.onCompleted();
        }

        @Override
        public void getSchema(pulumirpc.Provider.GetSchemaRequest request, StreamObserver<pulumirpc.Provider.GetSchemaResponse> responseObserver) {
            // protobuf sends an empty string for subpackageName/subpackageVersion, but really
            // that means null in the domain model.
            Function<String, String> nullIfEmpty = s -> {
                if (s == null || s.equals("")) {
                    return null;
                }
                return s;
            };

            var domRequest = new com.pulumi.provider.internal.models.GetSchemaRequest(
                request.getVersion(),
                nullIfEmpty.apply(request.getSubpackageName()),
                nullIfEmpty.apply(request.getSubpackageVersion())
            );

            this.implementation.getSchema(domRequest).thenAccept(domResponse -> {
                var grpcResponse = pulumirpc.Provider.GetSchemaResponse.newBuilder()
                    .setSchema(domResponse.getSchema())
                    .build();
                responseObserver.onNext(grpcResponse);
                responseObserver.onCompleted();
            });
        }
    }
} 
