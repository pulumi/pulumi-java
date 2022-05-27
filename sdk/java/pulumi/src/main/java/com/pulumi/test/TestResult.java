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
    public final int exitCode;
    public final List<Resource> resources;
    public final List<Exception> exceptions;
    public final List<String> errors;
    public final Map<String, Output<?>> stackOutputs;

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
        this.stackOutputs = ImmutableMap.copyOf(outputs);
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
        if (!this.stackOutputs.containsKey(name)) {
            return Output.of(CompletableFuture.failedFuture(
                    new IllegalArgumentException(String.format(
                            "Can't find stack output: '%s', available outputs: %s",
                            name, String.join(", ", this.stackOutputs.keySet())
                    ))
            ));
        }
        var output = this.stackOutputs.get(name);
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
