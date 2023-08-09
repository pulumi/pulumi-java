package com.pulumi.automation.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class AutoResult {

    private final int exitCode;
    private final List<Exception> exceptions;
    private final Map<String, Output<?>> exports;

    public AutoResult(int exitCode, List<Exception> exceptions, Map<String, Output<?>> exports) {
        this.exitCode = exitCode;
        this.exceptions = requireNonNull(ImmutableList.copyOf(exceptions));
        this.exports = requireNonNull(ImmutableMap.copyOf(exports));
    }

    public int exitCode() {
        return exitCode;
    }

    public List<Exception> exceptions() {
        return exceptions;
    }

    public Map<String, Output<?>> exports() {
        return exports;
    }
}
