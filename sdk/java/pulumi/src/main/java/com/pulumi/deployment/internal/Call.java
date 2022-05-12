package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.deployment.CallOptions;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public interface Call {

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args, @Nullable Resource self);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}, however the return value is ignored.
     */
    void call(String token, CallArgs args, @Nullable Resource self, CallOptions options);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args);

    /**
     * Same as {@link #call(String, TypeShape, CallArgs, Resource, CallOptions)}
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self);

    /**
     * Dynamically calls the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code call} will be an @see {@link Output}{@literal <T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.).
     */
    <T> Output<T> call(String token, TypeShape<T> targetType, CallArgs args, @Nullable Resource self, CallOptions options);
}
