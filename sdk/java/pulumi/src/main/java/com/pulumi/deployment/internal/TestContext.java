package com.pulumi.deployment.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.context.LoggingContext;
import com.pulumi.core.Output;

import java.util.Map;
import java.util.Objects;

public class TestContext implements Context {
    private final ImmutableMap.Builder<String, Output<?>> exports = ImmutableMap.builder();

    public Map<String, Output<?>> getStackOutputs() {
        return exports.build();
    }

    @Override
    public String projectName() {
        throw new IllegalStateException("Not implemented yet: projectName()");
    }

    @Override
    public String stackName() {
        throw new IllegalStateException("Not implemented yet: stackName()");
    }

    @Override
    public LoggingContext log() {
        throw new IllegalStateException("Not implemented yet: log()");
    }

    @Override
    public Context export(String name, Output<?> output) {
        Objects.requireNonNull(name, "The 'name' of an 'export' cannot be 'null'");
        Objects.requireNonNull(output, "The 'output' of an 'export' cannot be 'null'");
        this.exports.put(name, output);
        return this;
    }

    @Override
    public Config config() {
        throw new IllegalStateException("Not implemented yet: config()");
    }

    @Override
    public Config config(String name) {
        throw new IllegalStateException("Not implemented yet: config(String name)");
    }

    @Override
    public <T> Output<T> output(T value) {
        return Output.of(value);
    }
}
