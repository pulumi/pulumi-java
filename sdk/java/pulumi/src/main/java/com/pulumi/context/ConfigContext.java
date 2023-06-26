package com.pulumi.context;

import com.pulumi.Config;

/**
 * Provides {@link Config} in the current context.
 */
public interface ConfigContext {

    /**
     * Creates a new {@link Config} instance, with the default name,
     * the name of the current project, using the format
     * of {@code [<project-name>:]<key-name>}.
     *
     * @return the default {@link Config}
     */
    Config config();

    /**
     * Creates a new {@link Config} instance for a given namespace prefix.
     *
     * Configuration keys use the format {@code [<namespace>:]<key-name>},
     * with a colon delimiting the optional namespace and the actual key name.
     *
     * @param name unique logical name
     * @return a {@link Config} with the given {@code name}
     */
    Config config(String name);
}
