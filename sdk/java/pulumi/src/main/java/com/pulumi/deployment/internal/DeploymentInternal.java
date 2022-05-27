package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;
import com.pulumi.resources.internal.StackDefinition;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@InternalUse
public interface DeploymentInternal extends Deployment {

    DeploymentImpl.Config getConfig();

    Optional<String> getConfig(String fullKey);

    boolean isConfigSecret(String fullKey);

    StackDefinition getStack();

    void setStack(StackDefinition stack);

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
