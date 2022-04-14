package io.pulumi.context;

import io.pulumi.core.Output;

/**
 * Provides the current context to the stack callback.
 */
public interface StackContext extends OutputContext, ConfigContext {

    /**
     * @return the name of the current project
     */
    String projectName();

    /**
     * @return the name of the current stack
     */
    String stackName();

    /**
     * @return the logger context
     */
    LoggingContext log();

    /**
     * Exports and {@link Output} from a Pulumi stack.
     * @param name name of the {@code Output}
     * @param output the {@code Output} value
     * @return the {@link ExportContext} associated with current {@link StackContext}
     */
    ExportContext export(String name, Output<?> output);

    /**
     * Used to finish a stack callback.
     * @return the {@link ExportContext} associated with current {@link StackContext}
     */
    ExportContext exports();
}
