package com.pulumi.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.CompletableFutures;
import com.pulumi.core.internal.annotations.ImportMetadata;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.serialization.internal.JsonFormatter;
import com.pulumi.serialization.internal.Serializer;

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

    private final ImmutableList<ImportMetadata<?, ?, ?>> inputInfos;

    protected InputArgs() {
        this.inputInfos = extractInputInfos(this.getClass());
    }

    protected abstract void validateMember(Class<?> memberType, String fullName);

    
    /**
     * Internal utility class of {@link InputArgs} instances.
     *
     * @see InputArgs
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class InputArgsInternal {

        /**
         * The {@link InputArgs} instance.
         */
        private final InputArgs inputArgs;

        /**
         * Constructs a new internal utility for the given {@link InputArgs} instance.
         *
         * @param inputArgs the input arguments to manage; must not be null
         */
        private InputArgsInternal(InputArgs inputArgs) {
            this.inputArgs = requireNonNull(inputArgs);
        }

        /**
         * Creates a new {@link InputArgsInternal} instance from the given {@link InputArgs}.
         *
         * @param inputArgs the input arguments to wrap; must not be null
         * @return a new internal utility instance managing the given input arguments
         */
        public static InputArgsInternal from(InputArgs inputArgs) {
            return new InputArgsInternal(inputArgs);
        }

        // TODO: try to remove, this only casts the type
        public CompletableFuture<Map<Object, /* @Nullable */ Object>> toNullableMapAsync(Log log) {
            return toMapAsync(log)
                    .thenApply(map -> map.entrySet()
                            .stream()
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }

        // TODO: this method should probably be move to the serializer
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

            for (var info : this.inputArgs.inputInfos) {
                var fullName = this.inputArgs.fullName(info);

                var value = info.getFieldOutput(this.inputArgs);
                if (info.getAnnotation().required() && value.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("'%s' is required but was not given a value", this.inputArgs.fullName(info)));
                }

                if (info.getAnnotation().json()) {
                    var valueFuture = value.map(v -> convertToJson.apply(fullName, v))
                            .orElse(CompletableFuture.completedFuture(Output.ofNullable(null)));
                    builder.accumulate(
                            valueFuture, (b, m) -> b.put(info.getName(), m)
                    );
                } else {
                    var valueFuture = value.map(CompletableFuture::completedFuture)
                        .orElse(CompletableFuture.completedFuture(Output.ofNullable(null)));
                    builder.accumulate(
                            valueFuture, (b, m) -> b.put(info.getName(), m)
                    );
                }
            }

            return builder.build(ImmutableMap.Builder::build);
        }
    }

    private <T> ImmutableList<ImportMetadata<?, ?, ?>> extractInputInfos(Class<T> type) {
        return ImportMetadata.of(type).values().stream()
                .peek(info -> this.validateMember(info.getFieldType(), fullName(info)))
                .collect(toImmutableList());
    }

    private String fullName(ImportMetadata<?, ?, ?> input) {
        return String.format("@%s %s", Import.class.getSimpleName(), input.generateFullName(this.getClass()));
    }
}
