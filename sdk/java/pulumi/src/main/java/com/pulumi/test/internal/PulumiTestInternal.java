package com.pulumi.test.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.deployment.MockEngine;
import com.pulumi.deployment.MockMonitor;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.test.TestResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class PulumiTestInternal extends PulumiInternal {

    private final MockEngine engine;
    private final MockMonitor monitor;

    public PulumiTestInternal(
            Runner runner,
            MockEngine engine,
            MockMonitor monitor,
            ContextInternal stackContext
    ) {
        super(runner, stackContext);
        this.engine = requireNonNull(engine);
        this.monitor = requireNonNull(monitor);
    }

    public CompletableFuture<TestResult> runTestAsync(Consumer<Context> stackCallback) {
        return runAsyncResult(stackCallback)
                .thenApply(r -> new TestResult(
                        r.exitCode(),
                        this.monitor.resources,
                        r.exceptions(),
                        ImmutableList.copyOf(this.engine.getErrors()),
                        r.result()
                                .map(ctx -> ctx.exports())
                                .orElse(ImmutableMap.of())
                ));
    }
}
