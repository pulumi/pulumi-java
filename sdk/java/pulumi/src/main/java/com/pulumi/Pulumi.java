package com.pulumi;

import com.pulumi.internal.PulumiInternal;
import com.pulumi.resources.StackOptions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Pulumi program entrypoint.
 * Possible exit codes:
 * <ul>
 *     <li>0 - Process Exited Successfully</li>
 *     <li>1 - Process Exited Before Logging User Actionable Message</li>
 *     <li>32 - Process Exited After Logging User Actionable Message</li>
 * </ul>
 */
public interface Pulumi {

    /**
     * Run a Pulumi stack callback and wait for result.
     * <br>
     * In case of an error terminates the process with {@link System#exit(int)}
     *
     * @param stack the stack to run in Pulumi runtime
     * @see #runAsync(Consumer)
     */
    static void run(Consumer<Context> stack) {
        withOptions(StackOptions.Empty).run(stack);
    }

    /**
     * Run a Pulumi stack callback asynchronously.
     *
     * @param stack the stack to run in Pulumi runtime
     * @return a future exit code from Pulumi runtime after running the stack
     * @see #run(Consumer)
     * @see #withOptions(StackOptions)
     */
    static CompletableFuture<Integer> runAsync(Consumer<Context> stack) {
        return withOptions(StackOptions.Empty).runAsync(stack);
    }

    /**
     * @param options the {@link StackOptions} to use
     * @return a Pulumi program entrypoint with given {@link StackOptions}
     * @see #run(Consumer)
     * @see #runAsync(Consumer)
     */
    static Pulumi.API withOptions(StackOptions options) {
        return PulumiInternal.APIInternal.fromEnvironment(options);
    }

    /**
     * Pulumi entrypoint operations.
     */
    interface API {

        /**
         * Run a Pulumi stack callback and wait for result.
         * <br>
         * In case of an error terminates the process with {@link System#exit(int)}
         *
         * @param stack the stack to run in Pulumi runtime
         * @see #runAsync(Consumer)
         */
        void run(Consumer<Context> stack);

        /**
         * Run a Pulumi stack callback asynchronously.
         *
         * @param stack the stack to run in Pulumi runtime
         * @return a future exit code from Pulumi runtime after running the stack
         * @see #run(Consumer)
         * @see #withOptions(StackOptions)
         */
        CompletableFuture<Integer> runAsync(Consumer<Context> stack);
    }
}