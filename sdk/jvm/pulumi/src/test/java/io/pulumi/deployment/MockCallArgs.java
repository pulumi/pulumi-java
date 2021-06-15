package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

/**
 * MockCallArgs for use in CallAsync
 */
public class MockCallArgs {

    /**
     * Resource identifier.
     */
    @Nullable
    public final String token;

    @Nullable
    public final ImmutableMap<String, Object> args;

    @Nullable
    public final String provider;

    public MockCallArgs(@Nullable String token, @Nullable ImmutableMap<String, Object> args, @Nullable String provider) {
        this.token = token;
        this.args = args;
        this.provider = provider;
    }
}
