package com.pulumi.deployment;


import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentImpl;

public interface DeploymentInstance extends Deployment {
    @InternalUse
    DeploymentImpl.Config getConfig();

    boolean isInvalid();

    void markInvalid();
}
