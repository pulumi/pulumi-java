package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Output;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.core.internal.TypedInputOutput;
import io.pulumi.core.internal.annotations.OutputMetadata;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

@ParametersAreNonnullByDefault
public interface OutputCompletionSource<T> {

    void trySetException(Exception exception);

    void trySetDefaultResult(boolean isKnown);

    void setStringValue(String value, boolean isKnown);

    void setValue(InputOutputData</* @Nullable */ T> data);

    TypeShape<?> getTypeShape();

    static ImmutableMap<String, OutputCompletionSource<?>> initializeOutputs(Resource resource) {
        return extractOutputInfo(resource.getClass()).entrySet().stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> metadataToCompletionSource(resource, entry.getValue())
                ));
    }

    private static OutputCompletionSource<?> metadataToCompletionSource(Resource resource, OutputMetadata metadata) {
        var output = (Output<?>) metadata.getFieldValue(resource)
                .orElseGet(() -> {
                    var incomplete = Output.of(new CompletableFuture<>());
                    metadata.setFieldValue(resource, incomplete);
                    return incomplete;
                });

        return TypedInputOutput.outputCompletionSource(output, ImmutableSet.of(resource), metadata.getFieldTypeShape());
    }

    private static <T> ImmutableMap<String, OutputMetadata> extractOutputInfo(Class<T> type) {
        return OutputMetadata.of(type);
    }
}
