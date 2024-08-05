package com.pulumi.deployment.internal;

import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ReadOrRegisterResource {
    void readOrRegisterResource(
            Resource resource, boolean remote, Function<String, Resource> newDependency,
            ResourceArgs args, ResourceOptions options, Resource.LazyFields lazy,
            CompletableFuture<String> packageRef
    );
}