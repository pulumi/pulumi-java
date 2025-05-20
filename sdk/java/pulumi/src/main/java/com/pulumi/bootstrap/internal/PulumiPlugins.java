package com.pulumi.bootstrap.internal;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.core.internal.RegexPattern;
import com.pulumi.core.internal.annotations.InternalUse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

@InternalUse
class PulumiPlugins {

    // Make sure capturing groups are the same in the patterns, very easy to miss when refactoring
    private static final String PACKAGE_CAPTURING_GROUP = "package";
    private static final String NAMESPACE_CAPTURING_GROUP = "namespace";
    private static final String NAME_CAPTURING_GROUP = "name";
    private static final RegexPattern PLUGIN_PATTERN = RegexPattern.of("^(?<package>com/(?<namespace>.+)/(?<name>.+))/plugin.json$");
    private static final RegexPattern VERSION_PATTERN = RegexPattern.of("(?<package>com/(?<namespace>.+)/(?<name>.+))/version.txt$");

    private PulumiPlugins() {
        throw new UnsupportedOperationException("static class");
    }

    @InternalUse
    public static ImmutableMap<String, PulumiPlugin> fromClasspath(Class<?> type) {
        return getResourcesFrom(requireNonNull(type)).stream()
                .collect(Accumulator::new,
                        (packages, info) -> {
                            RawResource.resolve(PLUGIN_PATTERN, info).ifPresent(packages::putPlugin);
                            RawResource.resolve(VERSION_PATTERN, info).ifPresent(packages::putVersion);
                        },
                        Accumulator::putAll)
                .build();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ImmutableSet<ResourceInfo> getResourcesFrom(Class<?> type) {
        var loader = MoreObjects.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                requireNonNull(type).getClassLoader()
        );
        try {
            return ClassPath.from(loader).getResources();
        } catch (IOException e) {
            throw new RuntimeException("Cannot get resources from ClassPath", e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String asString(ResourceInfo info) {
        try {
            return Resources.toString(requireNonNull(info).url(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static final class RawResource {
        public final String name;
        public final String namespace;
        public final String content;

        RawResource(String name, String namespace, String content) {
            this.name = requireNonNull(name);
            this.namespace = requireNonNull(namespace);
            this.content = requireNonNull(content);
        }

        @SuppressWarnings("UnstableApiUsage")
        public static Optional<Map.Entry<String, RawResource>> resolve(RegexPattern pattern, ResourceInfo info) {
            var matcher = pattern.matcher(info.getResourceName());
            if (!matcher.hasMatch()) {
                return Optional.empty();
            }
            Function<String, RuntimeException> capturingExceptionSupplier = (String group) -> {
                throw new IllegalStateException(String.format(
                        "Expected capturing group '%s' to be present when there is a match for '%s'. This should never happen. It's a bug.",
                        group, pattern
                ));
            };
            var package_ = matcher.namedMatch(PACKAGE_CAPTURING_GROUP)
                    .orElseThrow(() -> capturingExceptionSupplier.apply(PACKAGE_CAPTURING_GROUP));
            var namespace = matcher.namedMatch(NAMESPACE_CAPTURING_GROUP)
                    .orElseThrow(() -> capturingExceptionSupplier.apply(NAMESPACE_CAPTURING_GROUP));
            var name = matcher.namedMatch(NAME_CAPTURING_GROUP)
                    .orElseThrow(() -> capturingExceptionSupplier.apply(NAME_CAPTURING_GROUP));
            return Optional.of(Map.entry(package_, new RawResource(name, namespace, asString(info))));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RawResource that = (RawResource) o;
            return Objects.equals(name, that.name)
                    && Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, content);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RawResource.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("namespace='" + namespace + "'")
                    .add("content='" + content + "'")
                    .toString();
        }
    }

    private static final class Accumulator {
        private final Map<String, RawResource> plugins = new HashMap<>();
        private final Map<String, RawResource> versions = new HashMap<>();

        @CanIgnoreReturnValue
        private Accumulator putPlugin(Map.Entry<String, RawResource> plugin) {
            this.plugins.put(plugin.getKey(), plugin.getValue());
            return this;
        }

        @CanIgnoreReturnValue
        private Accumulator putVersion(Map.Entry<String, RawResource> version) {
            this.versions.put(version.getKey(), version.getValue());
            return this;
        }

        @CanIgnoreReturnValue
        public Accumulator putAll(Accumulator builder) {
            this.plugins.putAll(builder.plugins);
            this.versions.putAll(builder.versions);
            return this;
        }

        public ImmutableMap<String, PulumiPlugin> build() {
            // We tolerate incomplete pairs, so a key with missing version or plugin is allowed
            return Stream.of(plugins, versions)
                    .flatMap(m -> m.keySet().stream())
                    .distinct() // all keys can be twice because we have two key sets
                    .collect(toImmutableMap(
                            k -> k,
                            k -> PulumiPlugin.resolve(
                                    plugins.getOrDefault(k, null),
                                    versions.getOrDefault(k, null)
                            )
                    ));
        }
    }
}
