package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.deployment.internal.DeploymentInstanceHolder;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.StackOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
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
     * The result of {@code invokeAsync} will be a @see {@link CompletableFuture} resolved to the
     * result value of the provider plugin.
     * <p>
     * The {@code args} inputs can be a bag of computed values
     * (including, {@code T}s, @see {@link CompletableFuture}, @see {@link io.pulumi.core.Output}, s etc.).
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}
     */
    <T> CompletableFuture<T> invokeAsync(String token, TypeShape<T> targetType, InvokeArgs args);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsyncVoid(String token, InvokeArgs args, InvokeOptions options);

    /**
     * Same as @see {@link #invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}, however the return value is ignored.
     */
    CompletableFuture<Void> invokeAsyncVoid(String token, InvokeArgs args);


    /**
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
     * @param callback Callback that creates stack resources.
     * @return A dictionary of stack outputs.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsync(Supplier<Map<String, Optional<Object>>> callback) {
        return runAsyncFuture(() -> CompletableFuture.supplyAsync(callback));
    }

    /**
     * @param callback Callback that creates stack resources.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsyncFutureVoid(Supplier<CompletableFuture<Void>> callback) {
        return runAsyncFuture(() -> callback.get()
                .thenApply(ignore -> ImmutableMap.of())
        );
    }

    /**
     * @param callback Callback that creates stack resources.
     * @see #runAsyncFuture(Supplier, StackOptions) for more details.
     */
    static CompletableFuture<Integer> runAsyncFuture(
            Supplier<CompletableFuture<Map<String, Optional<Object>>>> callback
    ) {
        return runAsyncFuture(callback, null);
    }

    /**
     * @param callback Callback that creates stack resources.
     * @param options  optional Stack options.
     * @see #runAsyncFuture(Supplier, StackOptions) is an entry-point to a Pulumi application.
     * JVM applications should perform all startup logic
     * they need in their {@code Main} method and then end with:
     * <p>
     * <code>
     * public static void main(String[] args) {
     * // program initialization code ...
     * <p>
     * return Deployment.runAsync(() -> {
     * // Code that creates resources.
     * });
     * }
     * </code>
     * </p>
     * Importantly: Cloud resources cannot be created outside of the lambda passed
     * to any of the @see #runAsync overloads.
     * Because cloud Resource construction is inherently asynchronous,
     * the result of this function is a @see {@link CompletableFuture} which should then be returned or awaited.
     * This will ensure that any problems that are encountered during
     * the running of the program are properly reported.
     * Failure to do this may lead to the program ending early before all resources are properly registered.
     * <p/>
     * The function passed to @see #runAsyncFuture(Supplier, StackOptions)
     * can optionally return a @see {@link java.util.Map}.
     * The keys and values in this map will become the outputs for the Pulumi Stack that is created.
     */
    static CompletableFuture<Integer> runAsyncFuture(
            Supplier<CompletableFuture<Map<String, Optional<Object>>>> callback,
            @Nullable StackOptions options
    ) {
        return DeploymentInternal.createRunnerAndRunAsync(
                DeploymentInternal.deploymentFactory(),
                runner -> runner.runAsyncFuture(callback, options)
        );
    }
}
