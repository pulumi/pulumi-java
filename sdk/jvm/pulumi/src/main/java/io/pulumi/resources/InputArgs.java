package io.pulumi.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pulumi.Log;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Internal.InternalField;
import io.pulumi.core.internal.annotations.InputMetadata;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.serialization.internal.JsonFormatter;
import io.pulumi.serialization.internal.Serializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
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

    private final ImmutableList<InputMetadata<?, ?, ?>> inputInfos;

    @SuppressWarnings("unused")
    @InternalField
    private final InputArgsInternal internal = new InputArgsInternal();

    protected InputArgs() {
        this.inputInfos = extractInputInfos(this.getClass());
    }

    protected abstract void validateMember(Class<?> memberType, String fullName);

    @InternalUse
    @ParametersAreNonnullByDefault
    public final class InputArgsInternal {

        private InputArgsInternal() { /* Empty */ }

        // TODO: try to remove, this only casts the type
        public CompletableFuture<Map<Object, /* @Nullable */ Object>> toNullableMapAsync(Log log) {
            return toMapAsync(log)
                    .thenApply(map -> map.entrySet()
                            .stream()
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }

        @InternalUse
        public CompletableFuture<ImmutableMap<String, Output<?>>> toMapAsync(Log log) {
            BiFunction<String, Object, CompletableFuture<Output<String>>> convertToJson = (context, input) -> {
                requireNonNull(context);
                requireNonNull(input);

                final var serializer = new Serializer(log);
                return serializer.serializeAsync(context, input, false)
                        .thenApply(Serializer::createValue)
                        .thenApply(value -> JsonFormatter.format(value)
                                .mapOrThrow(Function.identity(), Output::of));
            };

            var builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(ImmutableMap.<String, Output<?>>builder())
            );

            for (var info : InputArgs.this.inputInfos) {
                var fullName = InputArgs.this.fullName(info);

                var value = info.getFieldOutput(InputArgs.this);
                if (info.getAnnotation().required() && value.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("%s is required but was not given a value", InputArgs.this.fullName(info)));
                }

                if (info.getAnnotation().json()) {
                    var valueFuture = value.map(v -> convertToJson.apply(fullName, v))
                            .orElse(CompletableFuture.completedFuture(Output.empty()));
                    builder.accumulate(
                            valueFuture, (b, m) -> b.put(info.getName(), m)
                    );
                } else {
                    var valueFuture = value.map(CompletableFuture::completedFuture)
                        .orElse(CompletableFuture.completedFuture(Output.empty()));
                    builder.accumulate(
                            valueFuture, (b, m) -> b.put(info.getName(), m)
                    );
                }
            }

            return builder.build(ImmutableMap.Builder::build);
        }
    }

    private <T> ImmutableList<InputMetadata<?, ?, ?>> extractInputInfos(Class<T> type) {
        return InputMetadata.of(type).values().stream()
                .peek(info -> this.validateMember(info.getFieldType(), fullName(info)))
                .collect(toImmutableList());
    }

    private String fullName(InputMetadata<?, ?, ?> input) {
        return String.format("@%s %s", Import.class.getSimpleName(), input.generateFullName(this.getClass()));
    }
}
