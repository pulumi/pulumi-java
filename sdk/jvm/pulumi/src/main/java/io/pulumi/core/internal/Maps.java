package io.pulumi.core.internal;

import com.google.common.collect.ImmutableMap;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@InternalUse
public class Maps {

    private Maps() {
        throw new UnsupportedOperationException("static class");
    }

    public static <K, V> Map<K, Optional<V>> untypedNullableMapToTypedOptionalMap(
            Map<Object, /* @Nullable */ Object> map,
            Function<Object, K> typedKey,
            Function</* @Nullable */ Object, Optional<V>> typedValue
    ) {
        Objects.requireNonNull(map);
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(typedKey, typedValue));
    }

    public static <K, V> Optional<V> tryGetValue(Map<K, V> map, K key) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equals(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public static <K, V> ImmutableMap<K, V> merge(
            final Map<? extends K, ? extends V> map1,
            final Map<? extends K, ? extends V> map2
    ) {
        return ImmutableMap.<K, V>builder().putAll(map1).putAll(map2).build();
    }
}
