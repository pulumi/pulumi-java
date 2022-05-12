package com.pulumi.test;

import com.google.common.collect.ImmutableList;
import com.pulumi.resources.Resource;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TestResult {
    private final int exitCode;
    private final ImmutableList<Resource> resources;
    private final ImmutableList<Exception> exceptions;

    public TestResult(
            int exitCode,
            ImmutableList<Resource> resources,
            ImmutableList<Exception> exceptions
    ) {
        this.exitCode = exitCode;
        this.resources = requireNonNull(resources);
        this.exceptions = requireNonNull(exceptions);
    }

    public int exitCode() {
        return exitCode;
    }

    public List<Resource> resources() {
        return resources;
    }

    public List<Exception> exceptions() {
        return exceptions;
    }
}