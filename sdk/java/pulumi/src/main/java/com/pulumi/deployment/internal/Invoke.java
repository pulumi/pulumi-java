package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.resources.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public interface Invoke {

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invoke} will be an @see {@link Output}{@literal <T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.)
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invoke(String, TypeShape, InvokeArgs, InvokeOptions)}
     */
    <T> Output<T> invoke(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsync(String token, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invokeAsync} will be a @see {@link CompletableFuture} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.).
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options);
}