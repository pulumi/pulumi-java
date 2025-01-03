package com.pulumi;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.context.ConfigContext;
import com.pulumi.context.LoggingContext;
import com.pulumi.context.OutputContext;
import com.pulumi.core.Output;

/**
 * Provides the current stack context used in the stack callback.
 */
public interface Context extends OutputContext, ConfigContext {

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
     * Append an {@link Output} to exported stack outputs.
     * <p>
     * This method mutates the context internal state.
     * @param name name of the {@code Output}
     * @param output the {@code Output} value
     * @return the current {@link Context}
     */
    @CanIgnoreReturnValue
    Context export(String name, Output<?> output);

    /**
     * Append an {@code Output} value to exported stack outputs.
     * <p>
     * This method mutates the context internal state.
     * @param name name of the {@code Output}
     * @param output the plain output value
     * @return the current {@link Context}
     */
    @CanIgnoreReturnValue
    default <T> Context export(String name, T output) {
        return export(name, Output.of(output));
    }
}
