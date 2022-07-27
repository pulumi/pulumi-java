package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.internal.CompletableFutures;
import com.pulumi.core.internal.Constants;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static com.pulumi.core.internal.CompletableFutures.ignoreNullMapValues;
import static com.pulumi.core.internal.PulumiCollectors.toTupleOfMaps2;

public final class PropertiesSerializer {

    private final Log log;

    public PropertiesSerializer(Log log) {
        this.log = Objects.requireNonNull(log);
    }

    /**
     * Walks the props object passed in, awaiting all interior promises besides those
     * for @see {@link Resource#urn()} and @see {@link CustomResource#id()},
     * creating a reasonable POJO object that can be remoted over to registerResource.
     */
    public CompletableFuture<SerializationResult> serializeResourcePropertiesAsync(
            String label, Map<String, Output<?>> args, boolean keepResources
    ) {
        Predicate<String> filter = key -> !Constants.IdPropertyName.equals(key) && !Constants.UrnPropertyName.equals(key);
        return serializeFilteredPropertiesAsync(label, args, filter, keepResources);
    }

    public CompletableFuture<Struct> serializeAllPropertiesAsync(
            String label, Map<String, Output<?>> args, boolean keepResources
    ) {
        return serializeFilteredPropertiesAsync(label, args, unused -> true, keepResources)
                .thenApply(result -> result.serialized);
    }

    /**
     * walks the props object passed in, awaiting all interior promises for properties
     * with keys that match the provided filter, creating a reasonable POJO object that
     * can be remoted over to registerResource.
     */
    public CompletableFuture<SerializationResult> serializeFilteredPropertiesAsync(
            String label, Map<String, Output<?>> args, Predicate<String> acceptKey, boolean keepResources) {
        var resultFutures = new HashMap<String, CompletableFuture</* @Nullable */ Object>>();
        var temporaryResources = new HashMap<String, Set<Resource>>();

        // FIXME: this is ugly, try to factor out a method with named tuple as result type
        for (var arg : args.entrySet()) {
            var key = arg.getKey();
            var value = arg.getValue();
            if (acceptKey.test(key)) {
                var serializer = new Serializer(this.log); // serializer is mutable, that's why it's inside the loop
                var v = serializer.serializeAsync(String.format("%s.%s", label, key), value, keepResources);
                resultFutures.put(key, v);
                temporaryResources.put(key, serializer.dependentResources);
            }
        }

        return CompletableFutures.flatAllOf(resultFutures)
                .thenApply(results -> results.entrySet().stream()
                        .filter(ignoreNullMapValues())
                        .collect(toTupleOfMaps2(
                                Map.Entry::getKey,
                                Map.Entry::getKey,
                                e -> e.getValue().join(),
                                e -> temporaryResources.get(e.getKey())
                        ))
                ).thenApply(
                        results -> new SerializationResult(
                                Serializer.createStruct(results.t1),
                                results.t2
                        )
                );
    }

    @ParametersAreNonnullByDefault
    public static final class SerializationResult {
        public final Struct serialized;
        public final boolean containsUnknowns;
        public final ImmutableMap<String, Set<Resource>> propertyToDependentResources;

        public SerializationResult(
                Struct result,
                ImmutableMap<String, Set<Resource>> propertyToDependentResources) {
            this.serialized = result;
            this.containsUnknowns = detectUnknowns(Value.newBuilder().setStructValue(result).build());
            this.propertyToDependentResources = propertyToDependentResources;
        }

        private static boolean detectUnknowns(Value value) {
            var result = new AtomicBoolean();
            ValueVisitor.visit(value, v -> {
                if (isUnknown(v)) {
                    result.set(true);
                }
            });
            return result.get();
        }

        private static boolean isUnknown(Value v) {
            return (v.getKindCase() == Value.KindCase.STRING_VALUE) &&
                    v.getStringValue() == Constants.UnknownValue;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", SerializationResult.class.getSimpleName() + "[", "]")
                    .add("serialized=" + serialized)
                    .add("propertyToDependentResources=" + propertyToDependentResources)
                    .toString();
        }
    }
}
