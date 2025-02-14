package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.Runner;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class OutputFactory {

    private final Runner runner;

    public OutputFactory(Runner runner) {
        this.runner = requireNonNull(runner);
    }

    public <T> Output<T> of(T value) {
        requireNonNull(value, "'Output.of(T)' expects a non-null value, 'Output.ofNullable(T)' accepts 'null'");
        return ofNullable(value);
    }

    public <T> Output<T> ofNullable(T value) {
        var dataFuture = ContextAwareCompletableFuture.supplyAsync(
                () -> OutputData.ofNullable(value)
        );
        var output = new OutputInternal<>(dataFuture, 0 /* dummy */); // FIXME: remove dummy in later steps
        register(dataFuture);
        return output;
    }

    private <T> void register(CompletableFuture<OutputData<T>> dataFuture) {
        this.runner.registerTask(
                this.getClass().getTypeName() + " -> " + dataFuture, dataFuture
        );
    }
}
