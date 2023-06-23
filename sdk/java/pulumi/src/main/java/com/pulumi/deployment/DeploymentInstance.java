package com.pulumi.deployment;


import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.Config;

public interface DeploymentInstance extends Deployment {
    @InternalUse
    Config getConfig();
}
