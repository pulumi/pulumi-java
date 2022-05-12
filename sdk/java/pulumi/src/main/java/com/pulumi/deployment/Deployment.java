package com.pulumi.deployment;

import com.pulumi.deployment.internal.Call;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.Invoke;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public interface Deployment extends Invoke, Call {

    /**
     * The current running deployment instance. This is only available from inside the function
     * passed to @see {@link com.pulumi.deployment.internal.Runner#runAsyncFuture(Supplier)} (or its overloads).
     *
     * @throws IllegalStateException if called before 'run' was called
     */
    static DeploymentInstance getInstance() {
        return DeploymentInstanceHolder.getInstance();
    }

    /**
     * @return the current stack name
     */
    @Nonnull
    String getStackName();

    /**
     * @return the current project name
     */
    @Nonnull
    String getProjectName();

    /**
     * Whether the application is currently being previewed or actually applied.
     *
     * @return true if application is being applied
     */
    boolean isDryRun();
}
