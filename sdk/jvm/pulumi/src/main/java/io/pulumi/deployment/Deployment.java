package io.pulumi.deployment;

import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.internal.DeploymentInstanceHolder;
import io.pulumi.resources.CallArgs;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Deployment {

    /**
     * The current running deployment instance. This is only available from inside the function
     * passed to @see {@link io.pulumi.deployment.internal.Runner#runAsync(Supplier)} (or its overloads).
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

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invoke} will be an @see {@link Output<T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link io.pulumi.core.Output}s, etc.)
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOptions options);

    /**
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions)}
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invokeAsync} will be a @see {@link CompletableFuture} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link io.pulumi.core.Output}s, etc.).
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args);

    /**
     * Dynamically calls the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code call} will be an @see {@link Output<T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link io.pulumi.core.Output}s, etc.).
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, @Nullable CallOptions options);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args, @Nullable Resource self, @Nullable CallOptions options);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args, @Nullable Resource self);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args);
}
