package com.pulumi.context.internal;

import com.pulumi.Config;
import com.pulumi.context.ConfigContext;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class ConfigContextInternal implements ConfigContext {

    private final String projectName;
    private final Function<String, Config> configFactory;

    public ConfigContextInternal(String projectName, Function<String, Config> configFactory) {
        this.projectName = requireNonNull(projectName);
        this.configFactory = requireNonNull(configFactory);
    }

    @Override
    public Config config() {
        return this.configFactory.apply(this.projectName);
    }

    @Override
    public Config config(String name) {
        return this.configFactory.apply(name);
    }
}
