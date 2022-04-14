package io.pulumi.context.internal;

import io.pulumi.Config;
import io.pulumi.context.ExportContext;
import io.pulumi.context.LoggingContext;
import io.pulumi.context.StackContext;
import io.pulumi.core.Output;
import io.pulumi.core.internal.Strings;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;

import static io.pulumi.core.internal.Objects.require;
import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class StackContextInternal implements StackContext {

    private final String projectName;
    private final String stackName;
    private final LoggingContextInternal logging;
    private final ConfigContextInternal config;
    private final OutputContextInternal outputs;
    private final ExportContextInternal exports;

    public StackContextInternal(
            String projectName,
            String stackName,
            LoggingContextInternal logging,
            ConfigContextInternal config,
            OutputContextInternal outputs,
            ExportContextInternal exports
    ) {
        this.projectName = require(Strings::isNonEmptyOrNull, projectName, () -> "expected a project name, got empty string or null");
        this.stackName = require(Strings::isNonEmptyOrNull, stackName, () -> "expected a stack name, got empty string or null");
        this.logging = requireNonNull(logging);
        this.config = requireNonNull(config);
        this.outputs = requireNonNull(outputs);
        this.exports = requireNonNull(exports);
    }

    @Override
    public String projectName() {
        return this.projectName;
    }

    @Override
    public String stackName() {
        return this.stackName;
    }

    @Override
    public LoggingContext log() {
        return this.logging;
    }

    @Override
    public <T> Output<T> output(T value) {
        return this.outputs.output(value);
    }

    @Override
    public ExportContext export(String name, Output<?> output) {
        this.exports.export(name, output);
        return this.exports;
    }

    @Override
    public ExportContext exports() {
        return this.exports;
    }

    @Override
    public Config config() {
        return config.config();
    }

    @Override
    public Config config(String name) {
        return config.config(name);
    }
}
