package com.pulumi.context;

import com.pulumi.Config;

/**
 * Provides {@link Config} in the current context.
 */
public interface ConfigContext {

    /**
     * Creates a new {@link Config} instance, with the default name, the name of the current project.
     * @return the default {@link Config}
     */
    Config config();

    /**
     * Creates a new {@link Config} instance.
     *
     * @param name unique logical name
     * @return a {@link Config} with the given {@code name}
     */
    Config config(String name);
}
