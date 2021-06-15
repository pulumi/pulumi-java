package io.pulumi.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import io.pulumi.Stack;
import io.pulumi.core.Urn;
import io.pulumi.core.internal.Maps;
import io.pulumi.deployment.internal.Monitor;
import io.pulumi.serialization.internal.Deserializer;
import io.pulumi.serialization.internal.Serializer;
import pulumirpc.Provider;
import pulumirpc.Resource;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MockMonitor implements Monitor {

    private final Serializer serializer = new Serializer(false);
    private final Map<String, ImmutableMap<String, Object>> registeredResources = Collections.synchronizedMap(new HashMap<>());
    public final List<io.pulumi.resources.Resource> resources = Collections.synchronizedList(new LinkedList<>());
    private final Mocks mocks;

    public MockMonitor(Mocks mocks) {
        this.mocks = Objects.requireNonNull(mocks);
    }

    @Override
    public CompletableFuture<Resource.SupportsFeatureResponse> supportsFeatureAsync(Resource.SupportsFeatureRequest request) {
        var hasSupport = "secrets".equals(request.getId()) || "resourceReferences".equals(request.getId());
        return CompletableFuture.completedFuture(
                Resource.SupportsFeatureResponse.newBuilder().setHasSupport(hasSupport).build()
        );
    }

    @Override
    public CompletableFuture<Provider.InvokeResponse> invokeAsync(Provider.InvokeRequest request) {
        var args = deserializeToMap(request.getArgs());

        CompletableFuture<Map<String, Object>> toBeSerialized;
        if ("pulumi:pulumi:getResource".equals(request.getTok())) {
            var urn = (String) args.get("urn");
            Map<String, Object> registeredResource = Maps.tryGetValue(registeredResources, urn)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown resource '%s'", urn)));
            toBeSerialized = CompletableFuture.completedFuture(registeredResource);
        } else {
            toBeSerialized = mocks.callAsync(new MockCallArgs(request.getTok(), args, request.getProvider()));
        }

        return toBeSerialized
                .thenCompose(this::serializeToStruct)
                .thenApply(struct -> Provider.InvokeResponse.newBuilder().setReturn(struct).build());
    }

    @Override
    public CompletableFuture<Resource.ReadResourceResponse> readResourceAsync(io.pulumi.resources.Resource resource, Resource.ReadResourceRequest request) {
        return mocks.newResourceAsync(new MockResourceArgs(
                request.getType(),
                request.getName(),
                deserializeToMap(request.getProperties()),
                request.getProvider(),
                request.getId()
        )).thenCompose(idAndState -> {
            var id = idAndState.t1;
            var state = idAndState.t2;
            var urn = Urn.create(request.getParent(), request.getType(), request.getName());
            return serializeToMap(state)
                    .thenApply(serializedState -> {
                        var builder = ImmutableMap.<String, Object>builder();
                        builder.put("urn", urn);
                        if (id.isPresent()) {
                            builder.put("id", id.get());
                        }
                        builder.put("state", serializedState);

                        registeredResources.put(urn, builder.build());

                        return Resource.ReadResourceResponse.newBuilder()
                                .setUrn(urn)
                                .setProperties(Serializer.createStruct(serializedState))
                                .build();
                    });
        });
    }

    @Override
    public CompletableFuture<Resource.RegisterResourceResponse> registerResourceAsync(io.pulumi.resources.Resource resource, Resource.RegisterResourceRequest request) {
        this.resources.add(resource);

        if (Stack.InternalRootPulumiStackTypeName.equals(request.getType())) {
            return CompletableFuture.completedFuture(
                    Resource.RegisterResourceResponse.newBuilder()
                            .setUrn(Urn.create(request.getParent(), request.getType(), request.getName()))
                            .setObject(Struct.newBuilder().build())
                            .build()
            );
        }

        return mocks.newResourceAsync(new MockResourceArgs(
                request.getType(),
                request.getName(),
                deserializeToMap(request.getObject()),
                request.getProvider(),
                request.getImportId()
        )).thenCompose(idAndState -> {
            var id = idAndState.t1;
            var state = idAndState.t2;
            var urn = Urn.create(request.getParent(), request.getType(), request.getName());
            return serializeToMap(state)
                    .thenApply(serializedState -> {
                        registeredResources.put(urn, ImmutableMap.of(
                                "urn", urn,
                                "id", id.isPresent() ? id : request.getImportId(),
                                "state", serializedState
                        ));

                        return Resource.RegisterResourceResponse.newBuilder()
                                .setId(id.isPresent() ? id.get() : request.getImportId())
                                .setUrn(urn)
                                .setObject(Serializer.createStruct(serializedState))
                                .build();
                    });
        });
    }

    @Override
    public CompletableFuture<Void> registerResourceOutputsAsync(Resource.RegisterResourceOutputsRequest request) {
        return CompletableFuture.completedFuture(null);
    }

    private ImmutableMap<String, Object> deserializeToMap(Struct args) {
        var deserializer = new Deserializer();
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

    private CompletableFuture<Struct> serializeToStruct(Object o) {
        if (o instanceof CompletableFuture) {
            throw new IllegalArgumentException("Unexpected CompletableFuture");
        }
        return serializeToMap(o).thenApply(Serializer::createStruct);
    }

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
}
