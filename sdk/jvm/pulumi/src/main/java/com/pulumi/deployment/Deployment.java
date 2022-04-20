package com.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.Resource;
import com.pulumi.resources.Stack;
import com.pulumi.resources.StackOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Deployment {

    /**
     * The current running deployment instance. This is only available from inside the function
     * passed to @see {@link Deployment#runAsync(Supplier)} (or its overloads).
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
     * Whether or not the application is currently being previewed or actually applied.
     *
     * @return true if application is being applied
     */
    boolean isDryRun();

    /**
     * Dynamically invokes the function {@code token}, which is offered by a provider plugin.
     * <p>
     * The result of {@code invoke} will be an @see {@link Output}{@literal <T>} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.)
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
     * (including, {@code T}s, @see {@link CompletableFuture}s, @see {@link com.pulumi.core.Output}s, etc.).
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
     * An entry-point to a Pulumi application.
     * @param callback Callback that creates stack resources.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsyncRunnable(Runnable callback) {
        return runAsync(() -> {
            callback.run();
            return ImmutableMap.of();
        });
    }

    /**
     * An entry-point to a Pulumi application.
     * Deployment will instantiate a default stack instance based on the callback passed as {@code callback} parameter.
     * @param callback Callback that creates stack resources.
     * @return A dictionary of stack outputs.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsync(Supplier<Map<String, Output<?>>> callback) {
        return runAsyncFuture(() -> CompletableFuture.supplyAsync(callback));
    }

    /**
     * An entry-point to a Pulumi application.
     * Deployment will instantiate a default stack instance based on the callback passed as {@code callback} parameter.
     * @param callback Callback that creates stack resources.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsyncFuture(
            Supplier<CompletableFuture<Map<String, Output<?>>>> callback
    ) {
        return runAsyncFuture(callback, null);
    }

    /**
     * An entry-point to a Pulumi application.
     * Deployment will instantiate a default stack instance based on the callback passed as {@code callback} parameter.
     * @param callback Callback that creates stack resources.
     * @param options  Optional Stack options.
     * JVM applications should perform all startup logic
     * they need in their {@code main} method and then end with:
     * <p>
     * <code>
     * public static void main(String[] args) {
     *     // program initialization code ...
     *
     *     return Deployment.runAsync(() -{@literal >} {
     *        // Code that creates resources.
     *     });
     * }
     * </code>
     * </p>
     * <p>
     * Importantly: Cloud resources cannot be created outside of the lambda passed
     * to any of the @see #runAsync overloads.
     * </p>
     * <p>
     * Because cloud Resource construction is inherently asynchronous,
     * the result of this function is a @see {@link CompletableFuture} which should
     * then be returned or awaited. This will ensure that any problems that are
     * encountered during the running of the program are properly reported.
     * Failure to do this may lead to the program ending early before all
     * resources are properly registered.
     * </p>
     * The function passed to @see #runAsyncFuture(Supplier, StackOptions)
     * can optionally return a @see {@link java.util.Map}.
     * The keys and values in this map will become the outputs for the Pulumi Stack that is created.
     * @see #runAsyncStack(Supplier) for more information.
     */
    static CompletableFuture<Integer> runAsyncFuture(
            Supplier<CompletableFuture<Map<String, Output<?>>>> callback,
            @Nullable StackOptions options
    ) {
        return DeploymentInternal.createRunnerAndRunAsync(
                DeploymentInternal.deploymentFactory(),
                runner -> runner.runAsyncFuture(callback, options)
        );
    }

    /**
     * An entry-point to a Pulumi application.
     * Deployment will instantiate a new stack instance using the supplier passed as {@code stackFactory} parameter.
     * @param stackFactory The stack supplier used to create stack instances
     * JVM applications should perform all startup logic they
     * need in their {@code main} method and then end with:
     * <p>
     * <code>
     * public static void main(String[] args) {
     *     // program initialization code ...
     *
     *     {@literal return Deployment.runAsyncStack(() -> new MyStack()));}
     * }
     * </code>
     * </p>
     * <p>
     * Importantly: cloud resources cannot be created outside of the @see Stack component.
     * </p>
     * <p>
     * Because cloud Resource construction is inherently asynchronous, the
     * result of this function is a @see {@link CompletableFuture} which should then
     * be returned or awaited. This will ensure that any problems that are
     * encountered during the running of the program are properly reported.
     * Failure to do this may lead to the program ending early before all
     * resources are properly registered.
     * </p>
     * @see #runAsyncFuture(Supplier, StackOptions) for more information.
     */
    static <S extends Stack> CompletableFuture<Integer> runAsyncStack(Supplier<S> stackFactory) {
        return DeploymentInternal.createRunnerAndRunAsync(
                DeploymentInternal.deploymentFactory(),
                runner -> runner.runAsync(stackFactory)
        );
    }
}
