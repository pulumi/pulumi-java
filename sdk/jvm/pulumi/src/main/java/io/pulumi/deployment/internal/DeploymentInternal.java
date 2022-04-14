package io.pulumi.deployment.internal;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;
import io.pulumi.resources.Resource;
import io.pulumi.resources.ResourceArgs;
import io.pulumi.resources.ResourceOptions;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@InternalUse
public interface DeploymentInternal extends Deployment {

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
