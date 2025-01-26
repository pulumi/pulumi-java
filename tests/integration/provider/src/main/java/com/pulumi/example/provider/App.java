package com.pulumi.example.provider;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    private Server server;

    private void start() throws IOException {
        server = ServerBuilder.forPort(0) // Use port 0 to let system assign a free port
            .addService(new ResourceProviderImpl())
            .build()
            .start();
        
        // Print the actual bound port for the parent process to read
        System.out.println(server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    App.this.stop();
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

    public static void main(String[] args) throws IOException, InterruptedException {
        final App server = new App();
        server.start();
        server.blockUntilShutdown();
    }

    static class ResourceProviderImpl extends pulumirpc.ResourceProviderGrpc.ResourceProviderImplBase {

        @Override
        public void getSchema(pulumirpc.Provider.GetSchemaRequest request,
                            StreamObserver<pulumirpc.Provider.GetSchemaResponse> responseObserver) {
            try {
                String schemaPath = "schema.json";
                String schema = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(schemaPath)));
                
                pulumirpc.Provider.GetSchemaResponse response = pulumirpc.Provider.GetSchemaResponse.newBuilder()
                    .setSchema(schema)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (IOException e) {
                logger.severe("Failed to read schema.json: " + e.getMessage());
                responseObserver.onError(e);
            }
        }

        @Override
        public void getPluginInfo(Empty request,
                                StreamObserver<pulumirpc.Plugin.PluginInfo> responseObserver) {
            // Return basic plugin information
            pulumirpc.Plugin.PluginInfo info = pulumirpc.Plugin.PluginInfo.newBuilder()
                .setVersion("1.0.0")
                .build();
            
            responseObserver.onNext(info);
            responseObserver.onCompleted();
        }

        @Override
        public void configure(pulumirpc.Provider.ConfigureRequest request,
                            StreamObserver<pulumirpc.Provider.ConfigureResponse> responseObserver) {
            try {                
                pulumirpc.Provider.ConfigureResponse response = pulumirpc.Provider.ConfigureResponse.newBuilder()
                    .setAcceptSecrets(true)
                    .setAcceptResources(true)
                    .setAcceptOutputs(true)
                    .setSupportsPreview(true)
                    .build();
                    
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.severe("Configuration failed: " + e.getMessage());
                responseObserver.onError(e);
            }
        }
    }
}
