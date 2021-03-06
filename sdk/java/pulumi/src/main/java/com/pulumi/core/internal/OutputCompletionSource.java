package com.pulumi.core.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Value;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.annotations.ExportMetadata;
import com.pulumi.resources.Resource;
import com.pulumi.serialization.internal.Converter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * Lazy initialization object used to set {@link Output}
 * field values with data that will come from Pulumi engine.
 * The lazy initialization is done with reflection during the resource registration in:
 * {@link com.pulumi.deployment.internal.DeploymentImpl.ReadOrRegisterResource}.
 * @param <T> type of the field value
 */
@SuppressWarnings("JavadocReference") // ReadOrRegisterResource is private but this is an internal class
@ParametersAreNonnullByDefault
public class OutputCompletionSource<T> {

    protected final ImmutableSet<Resource> resources;
    protected final TypeShape<T> dataTypeShape;
    protected CompletableFuture<OutputData<T>> mutableData;

    public OutputCompletionSource(
            CompletableFuture<OutputData<T>> data,
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
        mutableData.complete(OutputData.ofNullable(
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
        mutableData.complete(OutputData.ofNullable(
                this.resources,
                (T) value,
                isKnown,
                false
        ));
    }

    public void setValue(OutputData<T> data) {
        mutableData.complete(OutputData.ofNullable(
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

    @VisibleForTesting
    static <T> OutputCompletionSource<T> of(
            Output<T> output,
            ImmutableSet<Resource> resources,
            TypeShape<T> fieldTypeShape
    ) {
        return new OutputCompletionSource<>(Internal.of(output).getDataAsync(), resources, fieldTypeShape);
    }

    private static <T> OutputCompletionSource<T> from(
            Resource resource,
            ExportMetadata<T> metadata
    ) {
        var output = metadata.getFieldValueOrElse(resource, () -> {
            var incomplete = Output.of(new CompletableFuture<T>());
            metadata.setFieldValue(resource, incomplete);
            return incomplete;
        });
        return of(output, ImmutableSet.of(resource), metadata.getDataShape());
    }

    /**
     * Finds all {@link Output} fields annotated with {@link Export}
     * and uses reflection to replace their values with a lazy initialization object {@link OutputCompletionSource}
     * It skips two special fields: {@code id} and {@code urn}
     * @param resource the resource to replace fields on
     * @return a map of export name and an output a lazy initialization object {@link OutputCompletionSource}
     */
    public static ImmutableMap<String, OutputCompletionSource<?>> from(Resource resource) {
        return ExportMetadata.of(resource.getClass()).entrySet().stream()
                .filter(x -> {
                    var name = x.getValue().getAnnotation().name();
                    return !name.equals(Constants.UrnPropertyName) && !name.equals(Constants.IdPropertyName);
                })
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> OutputCompletionSource.from(resource, entry.getValue())
                ));
    }
}
