package io.pulumi.core.internal;

import io.grpc.Internal;
import io.pulumi.core.InputOutput;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Internal interface to allow our code to operate on outputs in an untyped manner. Necessary
 * as there is no reasonable way to write algorithms over heterogeneous instantiations of
 * generic types.
 */
@ParametersAreNonnullByDefault
@Internal
public interface UntypedInputOutput {
    @Internal
    CompletableFuture<Set<Resource>> internalGetResourcesUntypedAsync();

    /**
     * Returns an @see {@link io.pulumi.core.Output} unsafe equivalent to this,
     * except with the underlying value of @see {@link InputOutputData#getValueOptional()} casted to an Object.
     */
    @Internal
    CompletableFuture<InputOutputData<Object>> internalGetDataUntypedAsync();

    static UntypedInputOutput cast(
            InputOutput<?, ?> inputOutput) {
        Objects.requireNonNull(inputOutput);
        if (inputOutput instanceof UntypedInputOutput) {
            return (UntypedInputOutput) inputOutput;
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Expected an 'UntypedInputOutput' instance, got: %s",
                            inputOutput.getClass().getSimpleName())
            );
        }
    }
}
