package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;
import com.pulumi.resources.internal.StackDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Simple mock of {@link Deployment} that does mostly nothing.
 */
@InternalUse
public class MockDeployment extends DeploymentInstanceHolder implements Deployment, DeploymentInternal {
    public final DeploymentImpl.DeploymentState state;

    public MockDeployment(DeploymentImpl.DeploymentState state) {
        this.state = requireNonNull(state);
    }

    @Nonnull
    @Override
    public String getStackName() {
        return this.state.stackName;
    }

    @Nonnull
    @Override
    public String getProjectName() {
        return this.state.projectName;
    }

    @Override
    public boolean isDryRun() {
        return this.state.isDryRun;
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOptions options) {
        return null;
    }

    @Override
    public <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args) {
        return null;
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options) {
        return null;
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args) {
        return null;
    }

    @Override
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options) {
        return null;
    }

    @Override
    public CompletableFuture<Void> invokeAsync(String token, InvokeArgs args) {
        return null;
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, @Nullable CallOptions options) {
        return null;
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self) {
        return null;
    }

    @Override
    public <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args) {
        return null;
    }

    @Override
    public void call(String token, CallArgs args, @Nullable Resource self, @Nullable CallOptions options) {
        // Empty
    }

    @Override
    public void call(String token, CallArgs args, @Nullable Resource self) {
        // Empty
    }

    @Override
    public void call(String token, CallArgs args) {
        // Empty
    }

    @Override
    public DeploymentImpl.Config getConfig() {
        return this.state.config;
    }

    @Override
    public Optional<String> getConfig(String fullKey) {
        return this.state.config.getConfig(fullKey);
    }

    @Override
    public boolean isConfigSecret(String fullKey) {
        return this.state.config.isConfigSecret(fullKey);
    }

    @Override
    public StackDefinition getStack() {
        return null;
    }

    @Override
    public void setStack(StackDefinition stack) {
        // Empty
    }

    @Override
    public Runner getRunner() {
        return this.state.runner;
    }

    @Override
    public void readOrRegisterResource(Resource resource, boolean remote, Function<String, Resource> newDependency, ResourceArgs args, ResourceOptions opts, Resource.LazyFields lazy) {
        // Empty
    }

    @Override
    public void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs) {
        // Empty
    }
}
