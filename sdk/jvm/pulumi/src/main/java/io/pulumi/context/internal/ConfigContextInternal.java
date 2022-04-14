package io.pulumi.context.internal;

import io.pulumi.Config;
import io.pulumi.context.ConfigContext;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class ConfigContextInternal implements ConfigContext {

    private final Config config;

    public ConfigContextInternal(Config config) {
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
