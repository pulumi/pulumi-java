package io.pulumi;

import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.internal.PulumiInternal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Pulumi program entrypoint.
 */
public interface Pulumi {

    /**
     * Run a Pulumi stack callback and wait for result.
     * @param stack the stack to run in Pulumi runtime
     * @return exit code from Pulumi runtime after running the stack
     * @see #runAsync(Function)
     */
    static Integer run(Function<StackContext, ExportContext> stack) {
        return runAsync(stack).join();
    }

    /**
     * Run a Pulumi stack callback asynchronously.
     * @param stack the stack to run in Pulumi runtime
     * @return a future exit code from Pulumi runtime after running the stack
     * @see #run(Function)
     */
    static CompletableFuture<Integer> runAsync(Function<StackContext, ExportContext> stack) {
        var pulumi = PulumiInternal.fromEnvironment();
        return pulumi.runAsync(stack);
    }
}