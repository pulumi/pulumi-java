package com.pulumi.context.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Exports;
import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class ExportsInternal implements Exports {

    private final ImmutableMap.Builder<String, Output<?>> exports;

    public ExportsInternal() {
        this.exports = ImmutableMap.builder();
    }

    public ExportsInternal(Map<String, Output<?>> exports) {
        this();
        requireNonNull(exports, "Expected non-null 'exports'");
        this.exports.putAll(exports);
    }

    public Exports export(String name, Output<?> output) {
        requireNonNull(name, "The 'name' of an 'export' cannot be 'null'");
        requireNonNull(output, "The 'output' of an 'export' cannot be 'null'");
        this.exports.put(name, output);
        return this;
    }

    @Override
    public Map<String, Output<?>> exports() {
        return this.exports.build();
    }
}
