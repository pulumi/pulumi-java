package io.pulumi.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pulumi.Log;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Internal.Field;
import io.pulumi.core.internal.annotations.InputMetadata;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.serialization.internal.JsonFormatter;
import io.pulumi.serialization.internal.Serializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * Base type for all input argument classes.
 */
@ParametersAreNonnullByDefault
public abstract class InputArgs {

    private final ImmutableList<InputMetadata> inputInfos;

    @SuppressWarnings("unused")
    @Field
    private final Internal internal = new Internal();

    protected InputArgs() {
        this.inputInfos = extractInputInfos(this.getClass());
    }

    protected abstract void validateMember(Class<?> memberType, String fullName);

    @InternalUse
    @ParametersAreNonnullByDefault
    public final class Internal {

        private Internal() { /* Emmpty */ }

        // TODO: try to remove, this only casts the type
        public CompletableFuture<Map<Object, /* @Nullable */ Object>> toNullableMapAsync(Log log) {
            return toOptionalMapAsync(log)
                    .thenApply(map -> map.entrySet()
                            .stream()
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }

        @InternalUse
        public CompletableFuture<Map<String, Optional<Object>>> toOptionalMapAsync(Log log) {
            BiFunction<String, Object, CompletableFuture<Optional<Object>>> convertToJson = (context, input) -> {
                requireNonNull(context);
                requireNonNull(input);

                final var serializer = new Serializer(log);
                return serializer.serializeAsync(context, input, false)
                        .thenApply(Serializer::createValue)
                        .thenApply(value -> JsonFormatter.format(value)
                                .mapOrThrow(Function.identity(), Optional::of));
            };

            var builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(ImmutableMap.<String, Optional<Object>>builder())
            );

            for (var info : InputArgs.this.inputInfos) {
                var fullName = InputArgs.this.fullName(info);

                var value = info.getFieldValue(InputArgs.this);
                if (info.getAnnotation().required() && value.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("%s is required but was not given a value", InputArgs.this.fullName(info)));
                }

                CompletableFuture<Optional<Object>> valueFuture;
                if (info.getAnnotation().json()) {
                    if (value.isPresent()) {
                        valueFuture = convertToJson.apply(fullName, value.get());
                    } else {
                        valueFuture = CompletableFuture.completedFuture(Optional.empty());
                    }
                } else {
                    valueFuture = CompletableFuture.completedFuture(value);
                }

                builder.accumulate(
                        valueFuture, (b, m) -> b.put(info.getName(), m)
                );
            }

            return builder.build(ImmutableMap.Builder::build);
        }
    }

    private <T> ImmutableList<InputMetadata> extractInputInfos(Class<T> type) {
        return InputMetadata.of(type).values().stream()
                .peek(info -> this.validateMember(info.getFieldType(), fullName(info)))
                .collect(toImmutableList());
    }

    private String fullName(InputMetadata input) {
        return String.format("@%s %s", Import.class.getSimpleName(), input.generateFullName(this.getClass()));
    }
}
