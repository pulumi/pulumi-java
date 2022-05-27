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
     * @see #run(Consumer, StackOptions)
     * @see #runAsync(Consumer)
     * @see #runAsync(Consumer, StackOptions)
     */
    static void run(Consumer<Context> stack) {
        System.exit(runAsync(stack, StackOptions.Empty).join());
    }

    /**
     * Run a Pulumi stack callback and wait for result.
     * <br>
     * In case of an error terminates the process with {@link System#exit(int)}
     *
     * @param stack        the stack to run in Pulumi runtime
     * @param stackOptions the optional stack options use
     * @see #run(Consumer)
     * @see #runAsync(Consumer)
     * @see #runAsync(Consumer, StackOptions)
     */
    static void run(Consumer<Context> stack, StackOptions stackOptions) {
        System.exit(runAsync(stack, stackOptions).join());
    }


    /**
     * Run a Pulumi stack callback asynchronously.
     *
     * @param stackCallback the stack callback to run in Pulumi runtime
     * @return a future exit code from Pulumi runtime after running the stack
     * @see #run(Consumer)
     * @see #run(Consumer, StackOptions)
     * @see #runAsync(Consumer, StackOptions)
     */
    static CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        return runAsync(stackCallback, StackOptions.Empty);
    }

    /**
     * Run a Pulumi stack callback asynchronously.
     *
     * @param stackCallback the stack callback to run in Pulumi runtime
     * @param stackOptions  the optional stack options use
     * @return a future exit code from Pulumi runtime after running the stack
     * @see #run(Consumer)
     * @see #run(Consumer, StackOptions)
     * @see #runAsync(Consumer)
     */
    static CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback, StackOptions stackOptions) {
        var pulumi = PulumiInternal.fromEnvironment();
        return pulumi.runAsync(stackCallback, stackOptions);
    }
}