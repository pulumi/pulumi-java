package com.pulumi.context;

import com.pulumi.Config;

/**
 * Provides {@link Config} in current context.
 */
public interface ConfigContext {

    /**
     * Creates a new {@link Config} instance, with default, the name of the current project.
     */
    Config config();

    /**
     * Creates a new {@link Config} instance.
     *
     * @param name unique logical name
     */
    Config config(String name);
}
