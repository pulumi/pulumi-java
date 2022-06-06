package com.pulumi.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.exceptions.RunException;
import com.pulumi.resources.Resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A test result information.
 */
public class TestResult {
    private final int exitCode;
    private final List<Resource> resources;
    private final List<Exception> exceptions;
    private final List<String> errors;
    private final Map<String, Output<?>> outputs;

    public TestResult(int exitCode,
                      List<Resource> resources,
                      List<Exception> exceptions,
                      List<String> errors,
                      Map<String, Output<?>> outputs
    ) {
        this.exitCode = exitCode;
        this.resources = ImmutableList.copyOf(resources);
        this.exceptions = ImmutableList.copyOf(exceptions);
        this.errors = ImmutableList.copyOf(errors);
        this.outputs = ImmutableMap.copyOf(outputs);
    }

    public int exitCode() {
        return this.exitCode;
    }

    public List<Resource> resources() {
        return this.resources;
    }

    public List<Exception> exceptions() {
        return this.exceptions;
    }

    public List<String> errors() {
        return this.errors;
    }

    public Map<String, Output<?>> outputs() {
        return this.outputs;
    }

    /**
     * Gets an output associated with the given name.
     *
     * @param name the stack output name
     * @return an output associated with the given {@code name}
     */
    public Output<Object> output(String name) {
        return output(name, Object.class);
    }

    /**
     * Gets an output associated with the given {@code name}
     * and casts the value to the given {@code type}.
     *
     * @param name the stack output name
     * @param type the stack output Class to use for casting
     * @param <T>  the stack output type
     * @return an output associated with the given {@code name}
     */
    public <T> Output<T> output(String name, Class<T> type) {
        if (!this.outputs.containsKey(name)) {
            return Output.of(CompletableFuture.failedFuture(
                    new IllegalArgumentException(String.format(
                            "Can't find stack output: '%s', available outputs: %s",
                            name, String.join(", ", this.outputs.keySet())
                    ))
            ));
        }
        var output = this.outputs.get(name);
        return output.applyValue(o -> {
            if (type.isAssignableFrom(o.getClass())) {
                return type.cast(o);
            }
            throw new IllegalArgumentException(String.format(
                    "Cannot cast '%s' to the given type: '%s'",
                    o.getClass().getTypeName(),
                    type.getTypeName()
            ));
        });
    }

    /**
     * Throw {@link RunException} if we've encountered an exception
     * or return {@link TestResult} otherwise.
     *
     * @return this {@link TestResult}
     */
    public TestResult throwOnError() {
        if (!this.exceptions.isEmpty()) {
            throw new RunException(String.format("Error count: %d, errors: %s",
                    this.exceptions.size(), this.exceptions.stream()
                            .map(Throwable::getMessage)
                            .collect(Collectors.joining(", "))
            ));
        }
        return this;
    }
}
