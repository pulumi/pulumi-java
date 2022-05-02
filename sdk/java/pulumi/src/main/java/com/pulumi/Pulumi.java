package com.pulumi;

import com.pulumi.internal.PulumiInternal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * @param stack the stack to run in Pulumi runtime
     * @see #runAsync(Consumer)
     */
    static void run(Consumer<Context> stack) {
        System.exit(runAsync(stack).join());
    }

    /**
     * Run a Pulumi stack callback asynchronously.
     * @param stack the stack to run in Pulumi runtime
     * @return a future exit code from Pulumi runtime after running the stack
     * @see #run(Consumer)
     */
    static CompletableFuture<Integer> runAsync(Consumer<Context> stack) {
        var pulumi = PulumiInternal.fromEnvironment();
        return pulumi.runAsync(stack);
    }
}