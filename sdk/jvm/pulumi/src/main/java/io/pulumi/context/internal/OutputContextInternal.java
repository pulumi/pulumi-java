package io.pulumi.context.internal;

import io.pulumi.context.OutputContext;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputFactory;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class OutputContextInternal implements OutputContext {

    private final OutputFactory output;

    public OutputContextInternal(OutputFactory output) {
        this.output = requireNonNull(output);
    }

    @Override
    public <T> Output<T> output(T value) {
        return this.output.of(value);
    }

    // TODO: <T> OutputBuilder<T> output() for nullables, lists, maps, json, etc...
}
