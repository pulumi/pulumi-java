package io.pulumi.deployment.internal;

import io.pulumi.resources.Resource;
import pulumirpc.Provider.CallRequest;
import pulumirpc.Provider.CallResponse;
import pulumirpc.Provider.InvokeRequest;
import pulumirpc.Provider.InvokeResponse;
import pulumirpc.Resource.*;

import java.util.concurrent.CompletableFuture;

public interface Monitor {
    CompletableFuture<SupportsFeatureResponse> supportsFeatureAsync(SupportsFeatureRequest request);

    CompletableFuture<InvokeResponse> invokeAsync(InvokeRequest request);

    CompletableFuture<CallResponse> callAsync(CallRequest request);

    CompletableFuture<ReadResourceResponse> readResourceAsync(Resource resource, ReadResourceRequest request);

    CompletableFuture<RegisterResourceResponse> registerResourceAsync(Resource resource, RegisterResourceRequest request);

    CompletableFuture<Void> registerResourceOutputsAsync(RegisterResourceOutputsRequest request);
}
