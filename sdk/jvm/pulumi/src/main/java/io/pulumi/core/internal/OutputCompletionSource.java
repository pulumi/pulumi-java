package io.pulumi.core.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Value;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.annotations.OutputMetadata;
import io.pulumi.resources.Resource;
import io.pulumi.serialization.internal.Converter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

@ParametersAreNonnullByDefault
public class OutputCompletionSource<T> {

    protected final ImmutableSet<Resource> resources;
    protected final TypeShape<T> dataTypeShape;
    protected CompletableFuture<InputOutputData<T>> mutableData;

    public OutputCompletionSource(
            CompletableFuture<InputOutputData<T>> data,
            ImmutableSet<Resource> resources,
            TypeShape<T> dataTypeShape
    ) {
        this.mutableData = java.util.Objects.requireNonNull(data);
        this.resources = java.util.Objects.requireNonNull(resources);
        this.dataTypeShape = Objects.requireNonNull(dataTypeShape);
    }

    public void trySetException(Exception exception) {
        mutableData.completeExceptionally(exception);
    }

    public void trySetDefaultResult(boolean isKnown) {
        mutableData.complete(InputOutputData.ofNullable(
                ImmutableSet.of(), null, isKnown, false) // TODO: check if this does not break things later on
        );
    }

    public void setStringValue(String value, boolean isKnown) {
        setObjectValue(value, TypeShape.of(String.class), isKnown);
    }

    public void setObjectValue(Object value, TypeShape<?> valueShape, boolean isKnown) {
        if (value != null && !dataTypeShape.getType().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "Expected 'setObjectValue' with matching types, got 'OutputCompletionSource<%s>' and value class '%s'",
                    value.getClass().getTypeName(), dataTypeShape.getType().getTypeName())
            );
        }
        if (!dataTypeShape.isAssignableFrom(valueShape)) {
            throw new IllegalArgumentException(String.format(
                    "Expected 'setObjectValue' with matching types, got 'OutputCompletionSource<%s>' and value shape '%s'",
                    valueShape.asString(), dataTypeShape.asString())
            );
        }
        //noinspection unchecked
        mutableData.complete(InputOutputData.ofNullable(
                this.resources,
                (T) value,
                isKnown,
                false
        ));
    }

    public void setValue(InputOutputData<T> data) {
        mutableData.complete(InputOutputData.ofNullable(
                Sets.union(this.resources, data.getResources()).immutableCopy(),
                data.getValueNullable(),
                data.isKnown(),
                data.isSecret()
        ));
    }

    public void setValue(Converter converter, String context, Value value, ImmutableSet<Resource> depsOrEmpty) {
        // we need to call the converter here, inside this class where we know the T,
        // the T type will get lost at the call site due to Java generics limitations
        setValue(converter.convertValue(
                context,
                value,
                getTypeShape(),
                depsOrEmpty
        ));
    }

    public TypeShape<T> getTypeShape() {
        return dataTypeShape;
    }

    public static <T> OutputCompletionSource<T> of(
            Output<T> output,
            ImmutableSet<Resource> resources,
            TypeShape<T> fieldTypeShape
    ) {
        return new OutputCompletionSource<>(Internal.of(output).dataFuture, resources, fieldTypeShape);
    }

    public static <T> OutputCompletionSource<T> from(
            Resource resource,
            OutputMetadata<T> metadata
    ) {
        var output = metadata.getFieldValue(resource).orElseGet(() -> {
            var incomplete = Output.of(new CompletableFuture<T>());
            metadata.setFieldValue(resource, incomplete);
            return incomplete;
        });
        return of(output, ImmutableSet.of(resource), metadata.getDataShape());
    }

    public static ImmutableMap<String, OutputCompletionSource<?>> from(Resource resource) {
        return OutputMetadata.of(resource.getClass()).entrySet().stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> OutputCompletionSource.from(resource, entry.getValue())
                ));
    }
}
