package io.pulumi.deployment.internal;

import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.CallOptions;
import io.pulumi.deployment.DeploymentInstance;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.CallArgs;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Metadata of the deployment that is currently running. Accessible via @see {@link io.pulumi.deployment.Deployment#getInstance()}.
 */
@InternalUse
public final class DeploymentInstanceInternal implements DeploymentInstance {

    private final DeploymentInternal deployment;

    @InternalUse
    DeploymentInstanceInternal(DeploymentInternal deployment) {
        this.deployment = deployment;
    }

    @InternalUse
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
    public DeploymentImpl.Config getConfig() {
        return deployment.getConfig();
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOptions options) {
        return deployment.invoke(token, targetType, args, options);
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args) {
        return deployment.invoke(token, targetType, args);
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
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options) {
        return deployment.invokeAsync(token, args, options);
    }

    @Override
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args) {
        return deployment.invokeAsync(token, args);
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, @Nullable CallOptions options) {
        return deployment.call(token, targetType, args, self, options);
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self) {
        return deployment.call(token, targetType, args, self);
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args) {
        return deployment.call(token, targetType, args);
    }

    @Override
    public void call(String token, CallArgs args, @Nullable Resource self, @Nullable CallOptions options) {
        deployment.call(token, args, self, options);
    }

    @Override
    public void call(String token, CallArgs args, @Nullable Resource self) {
        deployment.call(token, args, self);
    }

    @Override
    public void call(String token, CallArgs args) {
        deployment.call(token, args);
    }

    @InternalUse
    public static DeploymentInstanceInternal cast(DeploymentInstance instance) {
        Objects.requireNonNull(instance);
        if (instance instanceof DeploymentInstanceInternal) {
            return (DeploymentInstanceInternal) instance;
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Expected a 'DeploymentInstanceInternal' instance, got: %s",
                            instance.getClass().getSimpleName())
            );
        }
    }
}
