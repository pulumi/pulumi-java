package io.pulumi.bootstrap.internal;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.RegexPattern;
import io.pulumi.core.internal.annotations.InternalUse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

@InternalUse
class PulumiPackages {

    // Make sure the capturing group is the same in the patterns, very easy to miss when refactoring
    private static final String PACKAGE_CAPTURING_GROUP = "package";
    private static final RegexPattern VERSION_PATTERN = RegexPattern.of("^(?<package>io/pulumi/.+)/version.txt$");
    private static final RegexPattern PLUGIN_PATTERN = RegexPattern.of("^(?<package>io/pulumi/.+)/plugin.json$");

    private PulumiPackages() {
        throw new UnsupportedOperationException("static class");
    }

    @InternalUse
    @SuppressWarnings("UnstableApiUsage")
    public static ImmutableMap<String, PulumiPackage> fromClasspath(Class<?> type) {
        return getResourcesFrom(requireNonNull(type)).stream()
                .collect(PulumiPackages.Builder::new,
                        (packages, info) -> {
                            VERSION_PATTERN.matcher(info.getResourceName())
                                    .namedMatch(PACKAGE_CAPTURING_GROUP)
                                    .ifPresent(key -> packages.putVersion(key, asString(info)));
                            PLUGIN_PATTERN.matcher(info.getResourceName())
                                    .namedMatch(PACKAGE_CAPTURING_GROUP)
                                    .ifPresent(key -> packages.putPlugin(key, asString(info)));
                        },
                        Builder::putAll)
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

    public static class PulumiPackage {
        public final String version;
        public final String plugin;

        private PulumiPackage(String version, String plugin) {
            this.version = requireNonNull(version);
            this.plugin = requireNonNull(plugin);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PulumiPackage.class.getSimpleName() + "[", "]")
                    .add("version='" + version + "'")
                    .add("plugin='" + plugin + "'")
                    .toString();
        }
    }

    private static final class Builder {
        private final Map<String, String> versions = new HashMap<>();
        private final Map<String, String> plugins = new HashMap<>();

        @CanIgnoreReturnValue
        private Builder putVersion(String key, String version) {
            this.versions.put(key, version);
            return this;
        }

        @CanIgnoreReturnValue
        private Builder putPlugin(String key, String plugins) {
            this.plugins.put(key, plugins);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder putAll(Builder builder) {
            this.versions.putAll(builder.versions);
            this.plugins.putAll(builder.plugins);
            return this;
        }

        public ImmutableMap<String, PulumiPackage> build() {
            // We tolerate incomplete pairs, so a key with missing version or plugin is allowed
            return Stream.of(versions, plugins)
                    .flatMap(m -> m.keySet().stream())
                    .distinct() // all keys can be twice because we have two key sets
                    .collect(toImmutableMap(
                    k -> k,
                    k -> new PulumiPackage(
                            versions.getOrDefault(k, ""),
                            plugins.getOrDefault(k, "")
                    )
            ));
        }
    }
}
