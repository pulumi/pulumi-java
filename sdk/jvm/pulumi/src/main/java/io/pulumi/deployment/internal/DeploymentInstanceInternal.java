package io.pulumi.deployment.internal;

import io.grpc.Internal;
import io.pulumi.core.internal.Reflection;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.deployment.DeploymentInstance;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.InvokeArgs;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Metadata of the deployment that is currently running. Accessible via @see {@link io.pulumi.deployment.Deployment#getInstance()}.
 */
public final class DeploymentInstanceInternal implements DeploymentInstance {

    private final DeploymentInternal deployment;

    DeploymentInstanceInternal(DeploymentInternal deployment) {
        this.deployment = deployment;
    }

    @Internal
    public DeploymentInternal getInternal() {
        return deployment;
    }

    @Nonnull
    @Override
    public String getStackName() {
        return deployment.getStackName();
    }

    @Nonnull
    @Override
    public String getProjectName() {
        return deployment.getProjectName();
    }

    @Override
    public boolean isDryRun() {
        return deployment.isDryRun();
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
        return deployment.invokeAsync(token, targetType, args, options);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args) {
        return deployment.invokeAsync(token, targetType, args);
    }

    @Override
    public CompletableFuture<Void> invokeAsyncVoid(String token, InvokeArgs args, InvokeOptions options) {
        return deployment.invokeAsyncVoid(token, args, options);
    }

    @Override
    public CompletableFuture<Void> invokeAsyncVoid(String token, InvokeArgs args) {
        return deployment.invokeAsyncVoid(token, args);
    }
}
