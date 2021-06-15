package io.pulumi.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.Internal;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Maps;
import io.pulumi.core.internal.annotations.InputImport;
import io.pulumi.core.internal.annotations.InputMetadata;
import io.pulumi.serialization.internal.JsonFormatter;
import io.pulumi.serialization.internal.Serializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Base type for all input argument classes.
 */
@ParametersAreNonnullByDefault
public abstract class InputArgs {

    private final ImmutableList<InputMetadata> inputInfos;

    protected InputArgs() {
        this.inputInfos = extractInputInfos(this.getClass());
    }

    protected abstract void validateMember(Class<?> memberType, String fullName);

    @Internal
    public CompletableFuture<Map<Object, /* @Nullable */ Object>> internalUntypedNullableToMapAsync() {
        return internalTypedOptionalToMapAsync()
                .thenApply(Maps::typedOptionalMapToUntypedNullableMap);
    }

    @Internal
    public CompletableFuture<Map<String, Optional<Object>>> internalTypedOptionalToMapAsync() {
        BiFunction<String, Object, CompletableFuture<Optional<Object>>> convertToJson = (context, input) -> {
            Objects.requireNonNull(context);
            Objects.requireNonNull(input);

            final var serializer = new Serializer(false);
            return serializer.serializeAsync(context, input, false)
                    .thenApply(Serializer::createValue)
                    .thenApply(value -> JsonFormatter.format(value)
                            .mapOrThrow(Function.identity(), Optional::of));
        };

        var builder = CompletableFutures.builder(
                CompletableFuture.completedFuture(ImmutableMap.<String, Optional<Object>>builder())
        );

        for (var info : this.inputInfos) {
            var fullName = fullName(info);

            var value = info.getFieldValue(this);
            if (info.getAnnotation().required() && value.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("%s is required but was not given a value", fullName(info)));
            }

            CompletableFuture<Optional<Object>> valueFuture;
            if (info.getAnnotation().json()) {
                valueFuture = convertToJson.apply(fullName, value);
            } else {
                valueFuture = CompletableFuture.completedFuture(value);
            }

            builder.accumulate(
                    valueFuture, (b, m) -> b.put(info.getName(), m)
            );
        }

        return builder.build(ImmutableMap.Builder::build);
    }

    private <T> ImmutableList<InputMetadata> extractInputInfos(Class<T> type) {
        return InputMetadata.of(type).values().stream()
                .peek(info -> this.validateMember(info.getFieldType(), fullName(info)))
                .collect(toImmutableList());
    }

    private String fullName(InputMetadata input) {
        return String.format("@%s %s", InputImport.class.getSimpleName(), input.generateFullName(this.getClass()));
    }
}
