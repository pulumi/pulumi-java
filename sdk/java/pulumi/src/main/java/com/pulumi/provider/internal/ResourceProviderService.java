package com.pulumi.provider.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import com.pulumi.core.Alias;
import com.pulumi.deployment.internal.InlineDeploymentSettings;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomTimeouts;
import com.pulumi.resources.DependencyResource;
import com.pulumi.resources.StackOptions;
import com.pulumi.resources.internal.DependencyProviderResource;

/**
 * The {@code ResourceProviderService} class is responsible for managing the lifecycle of resource provider server.
 *
 * @see Provider
 * @see ResourceProviderService.ResourceProviderImpl
 */
public class ResourceProviderService {

    private static final Logger logger = Logger.getLogger(ResourceProviderService.class.getName());

    /**
     * The gRPC server instance that serves the resource provider.
     */
    Server server; // Exposed as private-package for testing.

    /**
     * The address of the Pulumi engine to which this provider service connects.
     */
    private final String engineAddress;

    /**
     * The provider implementation that contains the logic for handling resource operations.
     */
    private final Provider implementation;

    /**
     * Constructs a new {@code ResourceProviderService} with the specified engine address and provider implementation.
     * @param engineAddress the address of the Pulumi engine to connect to
     * @param implementation the provider implementation to expose via the gRPC server
     */
    public ResourceProviderService(String engineAddress, Provider implementation) {
        this.engineAddress = engineAddress;
        this.implementation = implementation;
    }

    /**
     * Starts the resource provider server and blocks the current thread until the server is shut down.
     * @throws IOException if the server fails to start due to I/O errors
     * @throws InterruptedException if the thread is interrupted while waiting for shutdown
     *
     * @see #start()
     * @see #blockUntilShutdown()
     */
    public void startAndBlockUntilShutdown() throws IOException, InterruptedException {
        start();
        blockUntilShutdown();
    }

    /**
     * Starts the gRPC server for the resource provider, making it available to handle requests from the engine.
     *
     * @throws IOException if the server fails to start due to I/O errors
     */
    void start() throws IOException {
        server = createServerBuilder()
            .addService(new ResourceProviderImpl(this.engineAddress, this.implementation))
            .intercept(new ErrorHandlingInterceptor())
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

    /**
     * Stops the gRPC server, preventing it from handling any further requests.
     *
     * @throws InterruptedException if the thread is interrupted while waiting for shutdown
     */
    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Blocks the current thread until the gRPC server is shut down.
     * @throws InterruptedException if the thread is interrupted while waiting for shutdown
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Creates a new {@link ServerBuilder} instance for configuring and building the gRPC server.
     *
     * @return a {@link ServerBuilder} instance for configuring the server
     */
    protected ServerBuilder<?> createServerBuilder() {
        return ServerBuilder.forPort(0);
    }

    static class ResourceProviderImpl extends pulumirpc.ResourceProviderGrpc.ResourceProviderImplBase {
        private final String engineAddress;
        private final Provider implementation;

        public ResourceProviderImpl(String engineAddress, Provider implementation) {
            this.engineAddress = engineAddress;
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
        public void getSchema(pulumirpc.Provider.GetSchemaRequest request, 
                StreamObserver<pulumirpc.Provider.GetSchemaResponse> responseObserver) {
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

            this.implementation.getSchema(domRequest)
                .whenComplete((domResponse, error) -> 
                    handleCompletion(error, domResponse, responseObserver, response -> {
                        var grpcResponse = pulumirpc.Provider.GetSchemaResponse.newBuilder()
                            .setSchema(response.getSchema())
                            .build();
                        responseObserver.onNext(grpcResponse);
                    })
                );
        }

        @Override
        public void configure(pulumirpc.Provider.ConfigureRequest request,
                            StreamObserver<pulumirpc.Provider.ConfigureResponse> responseObserver) {
            var domRequest = new com.pulumi.provider.internal.models.ConfigureRequest(
                request.getVariablesMap(), unmarshal(request.getArgs()), request.getAcceptSecrets(),
                request.getAcceptResources());

            this.implementation.configure(domRequest)
                .whenComplete((domResponse, error) -> 
                    handleCompletion(error, domResponse, responseObserver, response -> {
                        pulumirpc.Provider.ConfigureResponse grpcResponse = pulumirpc.Provider.ConfigureResponse.newBuilder()
                            .setAcceptSecrets(response.isAcceptSecrets())
                            .setAcceptResources(response.isAcceptResources())
                            .setAcceptOutputs(response.isAcceptOutputs())
                            .setSupportsPreview(response.isSupportsPreview())
                            .build();
                        responseObserver.onNext(grpcResponse);
                    })
                );
        }

        @Override
        public void construct(pulumirpc.Provider.ConstructRequest request,
                            StreamObserver<pulumirpc.Provider.ConstructResponse> responseObserver) {
            if (request.getParent().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Parent must be set for Component Providers.")
                    .asRuntimeException());
                return;
            }
            
            var aliases = request.getAliasesList().stream()
                    .map(urn -> Alias.withUrn(urn))
                    .toArray(Alias[]::new);
            var dependsOn = request.getDependenciesList().stream()
                    .map(urn -> new DependencyResource(urn))
                    .toArray(DependencyResource[]::new);
            var providers = request.getProvidersMap().values().stream()
                    .map(reference -> new DependencyProviderResource(reference))
                    .toArray(DependencyProviderResource[]::new);

            var opts = ComponentResourceOptions.builder()
                .aliases(aliases)
                .dependsOn(dependsOn)
                .protect(request.getProtect())
                .providers(providers)
                .parent(new DependencyResource(request.getParent()))
                .customTimeouts(deserializeTimeouts(request.getCustomTimeouts()))
                // TODO deletedWith: https://github.com/pulumi/pulumi-java/issues/944
                .ignoreChanges(request.getIgnoreChangesList())
                .retainOnDelete(request.getRetainOnDelete())
                .replaceOnChanges(request.getReplaceOnChangesList())
                .build();

            var domRequest = new com.pulumi.provider.internal.models.ConstructRequest(
                request.getType(), request.getName(), unmarshal(request.getInputs()), opts);

            var inlineDeploymentSettings = InlineDeploymentSettings.builder()
                .monitorAddr(request.getMonitorEndpoint())
                .engineAddr(this.engineAddress)
                .project(request.getProject())
                .stack(request.getStack())
                .organization(request.getOrganization())
                .isDryRun(request.getDryRun())
                .config(ImmutableMap.copyOf(request.getConfigMap()))
                .configSecretKeys(ImmutableSet.copyOf(request.getConfigSecretKeysList()))
                .build();

            var runner = PulumiInternal.fromInline(inlineDeploymentSettings, StackOptions.builder().build());
            runner.runInlineAsync(ctx -> this.implementation.construct(domRequest))
                .whenComplete((domResponse, error) -> 
                    handleCompletion(error, domResponse, responseObserver, response -> {
                        var domState = response.getState();
                        var state = PropertyValue.marshalProperties(domState);
                        var responseBuilder = pulumirpc.Provider.ConstructResponse.newBuilder()
                            .setUrn(response.getUrn())
                            .setState(state);

                        response.getStateDependencies().forEach((propertyName, dependencies) -> {
                            var propertyDeps = pulumirpc.Provider.ConstructResponse.PropertyDependencies.newBuilder()
                                .addAllUrns(dependencies)
                                .build();
                            responseBuilder.putStateDependencies(propertyName, propertyDeps);
                        });

                        var grpcResponse = responseBuilder.build();
                        responseObserver.onNext(grpcResponse);
                    })
                );
        }

        private <T> void handleCompletion(Throwable error, T response, 
                StreamObserver<?> responseObserver, 
                java.util.function.Consumer<T> successHandler) {
            if (error != null) {
                Throwable cause = error instanceof java.util.concurrent.CompletionException 
                    ? error.getCause() 
                    : error;
                responseObserver.onError(Status.INTERNAL
                    .withDescription(cause.getMessage())
                    .withCause(cause)
                    .asRuntimeException());
                return;
            }
            successHandler.accept(response);
            responseObserver.onCompleted();
            DeploymentInstanceHolder.internalUnsafeDestroyInstance();
        }

        /**
         * Deserializes the custom timeouts from the protobuf representation.
         * @param customTimeouts the protobuf representation of custom timeouts
         * @return a {@link CustomTimeouts} instance with the parsed timeouts
         */
        private static CustomTimeouts deserializeTimeouts(pulumirpc.Provider.ConstructRequest.CustomTimeouts customTimeouts)
        {
            return CustomTimeouts.builder()
                .create(CustomTimeouts.parseTimeoutString(customTimeouts.getCreate()))
                .update(CustomTimeouts.parseTimeoutString(customTimeouts.getUpdate()))
                .delete(CustomTimeouts.parseTimeoutString(customTimeouts.getDelete()))
                .build();
        }

        /**
         * Unmarshals the properties from a protobuf {@link Struct} into a map of {@link PropertyValue}.
         * @param properties the protobuf representation of properties
         * @return a map of property names to their corresponding {@link PropertyValue}
         */
        private static Map<String, PropertyValue> unmarshal(Struct properties) {
            if (properties == null) {
                return Collections.emptyMap();
            }
            return PropertyValue.unmarshalProperties(properties);
        }
    }
} 
