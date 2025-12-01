package com.pulumi.test.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.pulumi.Log;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import com.pulumi.core.internal.Maps;
import com.pulumi.core.internal.Urn;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.Monitor;
import com.pulumi.resources.Resource;
import com.pulumi.serialization.internal.Deserializer;
import com.pulumi.serialization.internal.Serializer;
import com.pulumi.test.Mocks;
import pulumirpc.Resource.ResourceCallRequest;
import pulumirpc.Provider.CallResponse;
import pulumirpc.Provider.InvokeResponse;
import pulumirpc.Resource.ReadResourceRequest;
import pulumirpc.Resource.ReadResourceResponse;
import pulumirpc.Resource.RegisterPackageRequest;
import pulumirpc.Resource.RegisterPackageResponse;
import pulumirpc.Resource.RegisterResourceOutputsRequest;
import pulumirpc.Resource.RegisterResourceRequest;
import pulumirpc.Resource.RegisterResourceResponse;
import pulumirpc.Resource.SupportsFeatureRequest;
import pulumirpc.Resource.SupportsFeatureResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.resources.internal.Stack.RootPulumiStackTypeName;


/**
 * MockMonitor is a test implementation of the {@link Monitor} interface for use with unit tests..
 *
 * @see com.pulumi.test.Mocks
 * @see com.pulumi.deployment.internal.Monitor
 */
@InternalUse
public class MockMonitor implements Monitor {

    /**
     * The user-provided mocks implementation used to simulate resource operations.
     */
    private final Mocks mocks;
    /**
     * Serializer for converting Java objects to protocol buffer structs.
     */
    private final Serializer serializer;
    /**
     * Deserializer for converting protocol buffer structs to Java objects.
     */
    private final Deserializer deserializer;

    /**
     * Map of URN to registered resource state, used to track resources created during the test.
     */
    private final Map<String, ImmutableMap<String, Object>> registeredResources;
    /**
     * List of all resources registered via this monitor during the test lifecycle.
     */
    public final List<Resource> resources;

    /**
     * Constructs a new MockMonitor instance.
     *
     * @param mocks the {@link Mocks} implementation to use for simulating resource operations
     * @param log   the {@link Log} instance for logging serialization/deserialization events
     * @throws NullPointerException if mocks is null
     */
    public MockMonitor(Mocks mocks, Log log) {
        this.mocks = Objects.requireNonNull(mocks);
        this.serializer = new Serializer(log);
        this.deserializer = new Deserializer(log);
        this.registeredResources = Collections.synchronizedMap(new HashMap<>());
        this.resources = Collections.synchronizedList(new LinkedList<>());
    }

    /**
     * Checks if a given feature is supported by this monitor implementation.
     *
     * @param request the feature support request
     * @return a future containing the feature support response
     */
    @Override
    public CompletableFuture<SupportsFeatureResponse> supportsFeatureAsync(SupportsFeatureRequest request) {
        var hasSupport = !"outputValues".equals(request.getId());
        return CompletableFuture.completedFuture(
                SupportsFeatureResponse.newBuilder().setHasSupport(hasSupport).build()
        );
    }

    /**
     * Invokes a provider function or retrieves a resource, delegating to the mocks implementation.
     *
     * @param request the invoke request
     * @return a future containing the invoke response with serialized results
     * @throws IllegalArgumentException if a requested resource URN is not found
     */
    @Override
    public CompletableFuture<InvokeResponse> invokeAsync(pulumirpc.Resource.ResourceInvokeRequest request) {
        var args = deserializeToMap(request.getArgs());

        CompletableFuture<Map<String, Object>> toBeSerialized;
        if ("pulumi:pulumi:getResource".equals(request.getTok())) {
            var urn = (String) args.get("urn");
            Map<String, Object> registeredResource = Maps.tryGetValue(registeredResources, urn)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Unknown resource '%s', got: %s", urn, registeredResources
                    )));
            toBeSerialized = CompletableFuture.completedFuture(registeredResource);
        } else {
            toBeSerialized = mocks.callAsync(new Mocks.CallArgs(request.getTok(), args, request.getProvider()));
        }

        return toBeSerialized
                .thenCompose(this::serializeToStruct)
                .thenApply(struct -> InvokeResponse.newBuilder().setReturn(struct).build());
    }

    /**
     * Invokes a provider call, delegating to the mocks implementation.
     *
     * @param request the call request
     * @return a future containing the call response with serialized results
     */
    @Override
    public CompletableFuture<CallResponse> callAsync(ResourceCallRequest request) {
        // For now, we'll route both Invoke and Call through IMocks.CallAsync.
        var args = deserializeToMap(request.getArgs());

        var toBeSerialized = mocks.callAsync(
                new Mocks.CallArgs(request.getTok(), args, request.getProvider())
        );
        return toBeSerialized
                .thenCompose(this::serializeToStruct)
                .thenApply(struct -> CallResponse.newBuilder().setReturn(struct).build());
    }

    /**
     * Reads the state of an existing resource, simulating a resource read operation.
     *
     * @param resource the resource instance being read
     * @param request  the read resource request
     * @return a future containing the read resource response with serialized state
     */
    @Override
    public CompletableFuture<ReadResourceResponse> readResourceAsync(Resource resource, ReadResourceRequest request) {
        return ContextAwareCompletableFuture.wrap(mocks.newResourceAsync(new Mocks.ResourceArgs(
                request.getType(),
                request.getName(),
                deserializeToMap(request.getProperties()),
                request.getProvider(),
                request.getId()
        ))).thenCompose(idAndState -> {
            var id = idAndState.id;
            var state = idAndState.state;
            var urn = Urn.create(
                    Deployment.getInstance().getStackName(),
                    Deployment.getInstance().getProjectName(),
                    Optional.of(request.getParent()),
                    request.getType(),
                    request.getName()
            );
            return serializeToMap(state)
                    .thenApply(serializedState -> {
                        var builder = ImmutableMap.<String, Object>builder();
                        builder.put("urn", urn);
                        if (id.isPresent()) {
                            builder.put("id", id.get());
                        }
                        builder.put("state", serializedState);

                        registeredResources.put(urn, builder.build());

                        return ReadResourceResponse.newBuilder()
                                .setUrn(urn)
                                .setProperties(Serializer.createStruct(serializedState))
                                .build();
                    });
        });
    }

    /**
     * Registers a new resource, simulating resource creation and tracking its state.
     *
     * @param resource the resource instance being registered
     * @param request  the register resource request
     * @return a future containing the register resource response with URN and serialized state
     */
    @Override
    public CompletableFuture<RegisterResourceResponse> registerResourceAsync(Resource resource, RegisterResourceRequest request) {
        this.resources.add(resource);

        if (RootPulumiStackTypeName.equals(request.getType())) {
            return CompletableFuture.completedFuture(
                    RegisterResourceResponse.newBuilder()
                            .setUrn(Urn.create(
                                    Deployment.getInstance().getStackName(),
                                    Deployment.getInstance().getProjectName(),
                                    Optional.of(request.getParent()),
                                    request.getType(),
                                    request.getName()
                            ))
                            .setObject(Struct.newBuilder().build())
                            .build()
            );
        }

        return ContextAwareCompletableFuture.wrap(mocks.newResourceAsync(new Mocks.ResourceArgs(
                request.getType(),
                request.getName(),
                deserializeToMap(request.getObject()),
                request.getProvider(),
                request.getImportId()
        ))).thenCompose(idAndState -> {
            var id = idAndState.id;
            var state = idAndState.state;
            var urn = Urn.create(
                    Deployment.getInstance().getStackName(),
                    Deployment.getInstance().getProjectName(),
                    Optional.of(request.getParent()),
                    request.getType(),
                    request.getName()
            );
            return serializeToMap(state)
                    .thenApply(serializedState -> {
                        registeredResources.put(urn, ImmutableMap.of(
                                "urn", urn,
                                "id", id.isPresent() ? id : request.getImportId(),
                                "state", serializedState
                        ));

                        return RegisterResourceResponse.newBuilder()
                                .setId(id.isPresent() ? id.get() : request.getImportId())
                                .setUrn(urn)
                                .setObject(Serializer.createStruct(serializedState))
                                .build();
                    });
        });
    }

    /**
     * Registers the outputs of a resource. This is a no-op in the mock implementation.
     *
     * @param request the register resource outputs request
     * @return a completed future
     */
    @Override
    public CompletableFuture<Void> registerResourceOutputsAsync(RegisterResourceOutputsRequest request) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Deserializes a protocol buffer struct to an immutable map of arguments.
     *
     * @param args the struct to deserialize
     * @return an immutable map of deserialized arguments
     */
    private ImmutableMap<String, Object> deserializeToMap(Struct args) {
        var builder = ImmutableMap.<String, Object>builder();
        for (var entry : args.getFieldsMap().entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            var data = deserializer.deserialize(value);
            if (data.isKnown() && data.getValueOptional().isPresent()) {
                builder.put(key, data.getValueOptional().get());
            }
        }
        return builder.build();
    }

    /**
     * Serializes an object to a protocol buffer struct asynchronously.
     *
     * @param o the object to serialize
     * @return a future containing the serialized struct
     * @throws IllegalArgumentException if the input is a CompletableFuture
     */
    private CompletableFuture<Struct> serializeToStruct(Object o) {
        if (o instanceof CompletableFuture) {
            throw new IllegalArgumentException("Unexpected CompletableFuture");
        }
        return serializeToMap(o).thenApply(Serializer::createStruct);
    }

    /**
     * Serializes an object to an immutable map asynchronously.
     *
     * @param o the object to serialize
     * @return a future containing the serialized immutable map
     * @throws UnsupportedOperationException if the serialization result is not a map
     */
    private CompletableFuture<ImmutableMap<String, Object>> serializeToMap(Object o) {
        if (o instanceof Map) {
            o = ImmutableMap.copyOf((Map<String, Object>) o); // defensive copy
        }
        if (o instanceof List) {
            o = ImmutableList.copyOf((List<Object>) o);
        }
        var objectType = o == null ? Void.class : o.getClass();
        return serializer.serializeAsync("MockMonitor", o, true)
                .thenApply(result -> {
                            if (result instanceof Map) {
                                return ImmutableMap.copyOf((Map<String, Object>) result);
                            } else {
                                throw new UnsupportedOperationException(String.format(
                                        "'%s' is not a supported result type, with argument '%s'",
                                        result.getClass().getTypeName(),
                                        objectType.getTypeName()
                                ));
                            }
                        }
                );
    }

    /**
     * Registers a provider package. This is a stub implementation for testing.
     *
     * @param request the register package request
     * @return a future containing the register package response with a mock reference
     */
    @Override
    public CompletableFuture<RegisterPackageResponse> registerPackageAsync(RegisterPackageRequest request) {
        return CompletableFuture.completedFuture(RegisterPackageResponse.newBuilder()
                .setRef("mock-uuid")
                .build());
    }
}