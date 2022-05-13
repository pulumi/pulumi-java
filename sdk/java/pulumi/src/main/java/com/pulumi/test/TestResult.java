package com.pulumi.test;

import com.pulumi.resources.Resource;
import com.pulumi.resources.Stack;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TestResult {

    private final int exitCode;
    private final List<Exception> exceptions;
    private final List<Resource> resources;
    private final List<String> errors;
    private final Stack stack;

    public TestResult(
            int exitCode,
            List<Exception> exceptions,
            List<Resource> resources,
            List<String> errors,
            Stack stack
    ) {
        this.exitCode = exitCode;
        this.exceptions = requireNonNull(exceptions);
        this.resources = requireNonNull(resources);
        this.errors = requireNonNull(errors);
        this.stack = requireNonNull(stack);
    }

    public int exitCode() {
        return this.exitCode;
    }

    public List<Exception> exceptions() {
        return this.exceptions;
    }

    public List<Resource> resources() {
        return this.resources;
    }

    public List<String> errors() {
        return this.errors;
    }

    public Stack stack() {
        return this.stack;
    }
}