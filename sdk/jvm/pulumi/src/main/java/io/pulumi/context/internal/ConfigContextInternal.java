package io.pulumi.context.internal;

import io.pulumi.Config;
import io.pulumi.context.ConfigContext;
import io.pulumi.core.internal.annotations.InternalUse;

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
