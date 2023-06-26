package com.pulumi.context.internal;

import com.pulumi.Config;
import com.pulumi.context.ConfigContext;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.internal.ConfigInternal;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class ConfigContextInternal implements ConfigContext {

    private final ConfigInternal config;

    public ConfigContextInternal(ConfigInternal config) {
        this.config = requireNonNull(config);
    }

    @Override
    public Config config() {
        return this.config;
    }

    @Override
    public Config config(String name) {
        return this.config.withName(name);
    }
}
