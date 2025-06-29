package com.pulumi.deployment;


import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentImpl;

/**
 * An instance of a Pulumi deployment, providing access to deployment configuration,
 * validity state, and lifecycle management for asynchronous operations.
 *
 * @see Deployment
 * @see com.pulumi.deployment.internal.DeploymentImpl
 */
public interface DeploymentInstance extends Deployment {
    @InternalUse
    DeploymentImpl.Config getConfig();

    boolean isInvalid();

    /**
     * We store the context in thread local storage, and we also capture it in completable futures.
     * Calling this method makes sure async callbacks that complete after the core program loop completes do not access the stale context.
     */
    void markInvalid();
}
