package com.pulumi.example.provider;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.example.provider.HelloWorld;
import com.pulumi.deployment.InlineDeploymentSettings;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.DependencyResource;
import com.pulumi.resources.CustomTimeouts;
import java.io.IOException;
import java.security.Provider.Service;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import io.grpc.Status;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    private Server server;
    private final String engineAddress;

    public App(String[] args) {
        // First argument should be the engine address
        this.engineAddress = args.length > 0 ? args[0] : null;
        if (this.engineAddress == null) {
            logger.warning("No engine address provided in arguments");
        }
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(0) // Use port 0 to let system assign a free port
            .addService(new ResourceProviderImpl(engineAddress))
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
        final App server = new App(args);
        server.start();
        server.blockUntilShutdown();
    }

    static class ResourceProviderImpl extends pulumirpc.ResourceProviderGrpc.ResourceProviderImplBase {
        private final String engineAddress;
        private String monitorAddress;

        public ResourceProviderImpl(String engineAddress) {
            this.engineAddress = engineAddress;
        }

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
                String errorDetails = getDetailedErrorMessage(e, "Failed to read schema.json");
                logger.severe(errorDetails);
                responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
            }
        }

        @Override
        public void construct(pulumirpc.Provider.ConstructRequest request,
                            StreamObserver<pulumirpc.Provider.ConstructResponse> responseObserver) {
            try {
                // Extract inputs from the request
                // Struct inputs = request.getInputs();
                // String type = request.getType();
                // String name = request.getName();
                // String monitorEndpoint = request.getMonitorEndpoint();
                // this.monitorAddress = monitorEndpoint;

                // // Initialize Pulumi deployment settings
                // // var settings = InlineDeploymentSettings.builder()
                // //     .monitorAddr(monitorEndpoint)
                // //     .engineAddr(this.engineAddress)
                // //     .build();
                
                // // // Create deployment instance
                // // var deployment = new DeploymentInstance(settings);
                // // PulumiInternal.setDeployment(deployment);


                // // Get the result and build properties
                // //String result = randomString.result();
                // Struct properties = Struct.newBuilder()
                //     .putFields("result", com.google.protobuf.Value.newBuilder().setStringValue("result").build())
                //     .build();

                var inlineDeploymentSettings = InlineDeploymentSettings.builder()
                    .monitorAddr(request.getMonitorEndpoint())
                    .engineAddr(this.engineAddress)
                    .project(request.getProject())
                    .stack(request.getStack())
                    .organization(request.getOrganization())
                    .isDryRun(request.getDryRun())
                    .build();

                var opts = ComponentResourceOptions.builder()
                    .parent(request.getParent().isEmpty() 
                        ? null 
                        : new DependencyResource(request.getParent()))
                    .protect(request.getProtect())
                    //.dependsOn(request.getDependsOnList())
                    //.aliases(request.getAliasesList())
                    // .customTimeouts(request.getCustomTimeouts() != null 
                    //     ? CustomTimeouts.deserialize(request.getCustomTimeouts()) 
                    //     : null)
                    // .deletedWith(request.getDeletedWith().isEmpty()
                    //     ? null
                    //     : new DependencyResource(request.getDeletedWith()))
                    // .ignoreChanges(request.getIgnoreChangesList())
                    // .retainOnDelete(request.getRetainOnDelete())
                    // .replaceOnChanges(request.getReplaceOnChangesList())
                    .build();

                Pulumi.runInlineAsync(inlineDeploymentSettings, ctx -> {
                    new HelloWorld("hello", opts);
                }).join();

                // Build the response with the new resource state
                pulumirpc.Provider.ConstructResponse response = pulumirpc.Provider.ConstructResponse.newBuilder()
                    //.setUrn(String.format("urn:pulumi:stack::project::%s::%s", type, name))
                    .setUrn("urn:pulumi:dev::java-yaml::javap:index:HelloWorld::hello")
                    //.setState(properties)
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

            } catch (Exception e) {
                String errorDetails = getDetailedErrorMessage(e, "Construction failed");
                logger.severe(errorDetails);
                responseObserver.onError(io.grpc.Status.UNKNOWN
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
            }
        }

        @Override
        public void getPluginInfo(Empty request,
                                StreamObserver<pulumirpc.Plugin.PluginInfo> responseObserver) {
            try {
                // Return basic plugin information
                pulumirpc.Plugin.PluginInfo info = pulumirpc.Plugin.PluginInfo.newBuilder()
                    .setVersion("1.0.0")
                    .build();
                
                responseObserver.onNext(info);
                responseObserver.onCompleted();
            } catch (Exception e) {
                String errorDetails = getDetailedErrorMessage(e, "Plugin info request failed");
                logger.severe(errorDetails);
                responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
            }
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
                String errorDetails = getDetailedErrorMessage(e, "Configuration failed");
                logger.severe(errorDetails);
                responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
            }
        }

        @Override
        public void attach(pulumirpc.Plugin.PluginAttach request,
                          StreamObserver<Empty> responseObserver) {
            try {                
                // Return empty response to indicate successful attachment
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            } catch (Exception e) {
                String errorDetails = getDetailedErrorMessage(e, "Attach failed");
                logger.severe(errorDetails);
                responseObserver.onError(Status.INTERNAL
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
            }
        }

        private String getDetailedErrorMessage(Throwable t, String context) {
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(context).append(":\n");
            errorDetails.append("Exception class: ").append(t.getClass().getName()).append("\n");
            errorDetails.append("Message: ").append(t.getMessage() != null ? t.getMessage() : "No message").append("\n");
            errorDetails.append("Stack trace:\n");
            for (StackTraceElement element : t.getStackTrace()) {
                errorDetails.append("\tat ").append(element.toString()).append("\n");
            }
            
            Throwable cause = t.getCause();
            if (cause != null) {
                errorDetails.append("Caused by: ").append(cause.getClass().getName()).append(": ")
                          .append(cause.getMessage() != null ? cause.getMessage() : "No message").append("\n");
                for (StackTraceElement element : cause.getStackTrace()) {
                    errorDetails.append("\tat ").append(element.toString()).append("\n");
                }
            }
            return errorDetails.toString();
        }
    }
} 
