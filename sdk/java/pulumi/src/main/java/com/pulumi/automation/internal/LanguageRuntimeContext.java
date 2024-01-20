package com.pulumi.automation.internal;

import com.pulumi.Context;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class LanguageRuntimeContext {

    private final Consumer<Context> program;

    public LanguageRuntimeContext(Consumer<Context> program) {
        this.program = requireNonNull(program);
    }

    public Consumer<Context> program() {
        return this.program;
    }
}
