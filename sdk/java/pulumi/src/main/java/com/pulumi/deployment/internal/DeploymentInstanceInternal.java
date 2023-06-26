package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.CallOptions;
import com.pulumi.deployment.DeploymentInstance;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.internal.ConfigInternal;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Metadata of the deployment that is currently running. Accessible via @see {@link com.pulumi.deployment.Deployment#getInstance()}.
 */
@InternalUse
public final class DeploymentInstanceInternal implements DeploymentInstance {

    private final DeploymentInternal deployment;

    @InternalUse
    public DeploymentInstanceInternal(DeploymentInternal deployment) {
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
    public ConfigInternal getConfig() {
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

    @Override
    public void readOrRegisterResource(Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args, ResourceOptions options, Resource.LazyFields lazy) {
        this.deployment.readOrRegisterResource(resource, remote, newDependency, args, options, lazy);
    }

    @Override
    public void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs) {
        this.deployment.registerResourceOutputs(resource, outputs);
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
