package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

/**
 * MockResourceArgs for use in NewResourceAsync
 */
public class MockResourceArgs {

    /**
     * Resource type name.
     */
    @Nullable
    public final String type;

    /**
     * Resource name.
     */
    @Nullable
    public final String name;

    /**
     * Dictionary of resource input properties.
     */
    @Nullable
    public final ImmutableMap<String, Object> inputs;

    @Nullable
    public final String provider;

    /**
     * Resource identifier.
     */
    @Nullable
    public final String id;

    public MockResourceArgs(
            @Nullable String type,
            @Nullable String name,
            @Nullable ImmutableMap<String, Object> inputs,
            @Nullable String provider,
            @Nullable String id
    ) {
        this.type = type;
        this.name = name;
        this.inputs = inputs;
        this.provider = provider;
        this.id = id;
    }
}
