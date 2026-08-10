package com.pulumi.deployment.internal;

import com.pulumi.resources.Resource;
import pulumirpc.Resource.ResourceCallRequest;
import pulumirpc.Provider.CallResponse;
import pulumirpc.Resource.*;

import java.util.concurrent.CompletableFuture;

public interface Monitor {
    CompletableFuture<SupportsFeatureResponse> supportsFeatureAsync(SupportsFeatureRequest request);

    CompletableFuture<ResourceInvokeResponse> invokeAsync(ResourceInvokeRequest request);

    CompletableFuture<CallResponse> callAsync(ResourceCallRequest request);

    CompletableFuture<ReadResourceResponse> readResourceAsync(Resource resource, ReadResourceRequest request);

    CompletableFuture<RegisterResourceResponse> registerResourceAsync(Resource resource, RegisterResourceRequest request);

    CompletableFuture<Void> registerResourceOutputsAsync(RegisterResourceOutputsRequest request);

    CompletableFuture<RegisterPackageResponse> registerPackageAsync(RegisterPackageRequest request);
}