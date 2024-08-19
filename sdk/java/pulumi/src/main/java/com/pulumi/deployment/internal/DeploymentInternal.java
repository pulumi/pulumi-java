package com.pulumi.deployment.internal;

import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.resources.internal.Stack;

import java.util.Optional;

@InternalUse
public interface DeploymentInternal extends Deployment {

    DeploymentImpl.Config getConfig();

    Optional<String> getConfig(String fullKey);

    boolean isConfigSecret(String fullKey);

    Stack getStack();

    void setStack(Stack stack);

    Runner getRunner();

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