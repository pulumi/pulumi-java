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

    // this method *must* remain async
    // in order to protect the scope of the Deployment#instance we cannot elide the task (return it early)
    // if the task is returned early and not awaited, than it is possible for any code that runs before the eventual await
    // to be executed synchronously and thus have multiple calls to one of the run methods affecting each others Deployment#instance
    @InternalUse
    @VisibleForTesting
    static CompletableFuture<Integer> createRunnerAndRunAsync(
            Supplier<DeploymentInternal> deploymentFactory,
            Function<Runner, CompletableFuture<Integer>> runAsync
    ) {
        return CompletableFuture.supplyAsync(deploymentFactory)
                .thenApply(deployment -> {
                    var newInstance = new DeploymentInstanceInternal(deployment);
                    DeploymentInstanceHolder.setInstance(newInstance);
                    return newInstance.getInternal().getRunner();
                })
                .thenCompose(runAsync);
    }
}
