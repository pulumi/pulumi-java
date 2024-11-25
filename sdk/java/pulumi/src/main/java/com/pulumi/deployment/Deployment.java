package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.ReadOrRegisterResource;
import com.pulumi.deployment.internal.RegisterResourceOutputs;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@InternalUse
public interface Deployment extends ReadOrRegisterResource, RegisterResourceOutputs {

    /**
     * The current running deployment instance. This is only available from inside the function
     * passed to @see {@link com.pulumi.deployment.internal.Runner#runAsync(Supplier)} (or its overloads).
     *
     * @throws IllegalStateException if called before 'run' was called
     */
    @InternalUse
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
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOptions options);

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invoke} will be an @see {@link Output}{@literal <T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.)
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOptions options, CompletableFuture<String> packageRef);

    /**
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)} but takes
     * {@link InvokeOutputOptions} as options, which allows setting {@link InvokeOutputOptions#dependsOn} to specify
     * additional resource dependencies besides the ones that are automatically detected from the {@link InvokeArgs}.
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOutputOptions options);

    /**
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)} but takes
     * {@link InvokeOutputOptions} as options, which allows setting {@link InvokeOutputOptions#dependsOn} to specify
     * additional resource dependencies besides the ones that are automatically detected from the {@link InvokeArgs}.
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, @Nullable InvokeOutputOptions options, CompletableFuture<String> packageRef);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions, CompletableFuture)}
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options);

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invokeAsync} will be a @see {@link CompletableFuture} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.).
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options, CompletableFuture<String> packageRef);

    /**
     * Dynamically calls the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code call} will be an @see {@link Output}{@literal <T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.).
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

    /**
     * Registers a parameterization of a given provider, returning a package reference that can be used to instantiate
     * resources and call functions against it.
     *
     * @param baseProviderName        The name of the base provider being parameterized
     * @param baseProviderVersion     The version of the base provider being parameterized
     * @param baseProviderDownloadUrl The download URL of the base provider being parameterized
     * @param packageName             The name of the package being registered
     * @param packageVersion          The version of the package being registered
     * @param base64Parameter         The base64-encoded parameterization of the base provider
     *
     * @return A future that resolves to the package reference
     */
    CompletableFuture<String> registerPackage(
        String baseProviderName,
        String baseProviderVersion,
        String baseProviderDownloadUrl,
        String packageName,
        String packageVersion,
        String base64Parameter
    );
}