package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;
import com.pulumi.resources.Stack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@InternalUse
public interface DeploymentInternal extends Deployment {

    DeploymentImpl.Config getConfig();

    Optional<String> getConfig(String fullKey);

    boolean isConfigSecret(String fullKey);

    Stack getStack();

    void setStack(Stack stack);

    Runner getRunner();

    void readOrRegisterResource(Resource resource, boolean remote, Function<String, Resource> newDependency,
                                ResourceArgs args, ResourceOptions opts, Resource.LazyFields lazy);

    void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs);

    @InternalUse
    static DeploymentInternal getInstance() {
        return DeploymentInstanceInternal.cast(Deployment.getInstance()).getInternal();
    }

    @InternalUse
    static Optional<DeploymentInternal> getInstanceOptional() {
        return DeploymentInstanceHolder.getInstanceOptional()
                .map(DeploymentInstanceInternal::cast)
                .map(DeploymentInstanceInternal::getInternal);
    }
}
