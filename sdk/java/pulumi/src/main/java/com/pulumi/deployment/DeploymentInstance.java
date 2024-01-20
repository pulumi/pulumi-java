package com.pulumi.deployment;

import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.internal.ConfigInternal;

public interface DeploymentInstance extends Deployment {
    @InternalUse
    ConfigInternal getConfig();
}
