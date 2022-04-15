package io.pulumi.deployment;


import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.internal.DeploymentImpl;

public interface DeploymentInstance extends Deployment {
    @InternalUse
    DeploymentImpl.Config getConfig();
}
