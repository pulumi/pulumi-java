package io.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;
import io.pulumi.resources.Resource;
import io.pulumi.resources.ResourceArgs;
import io.pulumi.resources.ResourceOptions;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@InternalUse
public interface DeploymentInternal extends Deployment {

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

    @InternalUse
    static Supplier<DeploymentInternal> deploymentFactory() {
        return DeploymentImpl::new;
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
