package io.pulumi.serialization.internal;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import io.grpc.Internal;
import io.pulumi.core.Input;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.internal.Maps;
import io.pulumi.core.internal.Optionals;
import io.pulumi.core.internal.Reflection;
import io.pulumi.core.internal.SemanticVersion;
import io.pulumi.core.internal.annotations.ResourceType;
import io.pulumi.resources.*;
import pulumirpc.EngineGrpc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.pulumi.core.internal.PulumiCollectors.toSingleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;

public class ResourcePackages {

    private static final Supplier<ImmutableMap<String, ImmutableList<Tuple2<Optional<String>, Class<Resource>>>>> resourceTypes =
            Suppliers.memoize(ResourcePackages::discoverResourceTypes); // lazy init

    @SuppressWarnings("UnstableApiUsage")
    private static ImmutableMap<String, ImmutableList<Tuple2<Optional<String>, Class<Resource>>>> discoverResourceTypes() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            var classpath = ClassPath.from(loader);
            return classpath.getAllClasses().stream()
                    .filter(c -> // exclude early our dependencies and common packages almost certain to not contain what we want
                            !c.getPackageName().isBlank()
                                    && !c.getPackageName().startsWith("META-INF")
                                    && !c.getPackageName().startsWith("java.")
                                    && !c.getPackageName().startsWith("javax.")
                                    && !c.getPackageName().equals("kotlin")
                                    && !c.getPackageName().startsWith("kotlin.")
                                    && !c.getPackageName().startsWith("io.grpc")
                                    && !c.getPackageName().startsWith("com.google.common")
                                    && !c.getPackageName().startsWith("com.google.rpc")
                                    && !c.getPackageName().startsWith("com.google.protobuf")
                                    && !c.getPackageName().startsWith("com.google.gson")
                                    && !c.getPackageName().startsWith("com.google.type")
                                    && !c.getPackageName().startsWith("com.google.thirdparty")
                                    && !c.getPackageName().startsWith("com.google.longrunning")
                                    && !c.getPackageName().startsWith("com.google.api")
                                    && !c.getPackageName().startsWith("com.google.logging")
                                    && !c.getPackageName().startsWith("com.google.geo")
                                    && !c.getPackageName().startsWith("com.google.cloud")
                                    && !c.getPackageName().startsWith("com.google.errorprone")
                                    && !c.getPackageName().startsWith("com.google.j2objc")
                                    && !c.getPackageName().startsWith("net.javacrumbs.futureconverter")
                                    && !c.getPackageName().startsWith("org.intellij.lang.annotations")
                                    && !c.getPackageName().startsWith("org.jetbrains.annotations")
                                    && !c.getPackageName().startsWith("android.annotation")
                                    && !c.getPackageName().startsWith("worker.org.gradle")
                                    && !c.getPackageName().startsWith("junit.")
                                    && !c.getPackageName().equals("org.junit")
                                    && !c.getPackageName().startsWith("org.junit.")
                                    && !c.getPackageName().equals("org.assertj")
                                    && !c.getPackageName().startsWith("org.assertj.")
                                    && !c.getPackageName().equals("org.hamcrest")
                                    && !c.getPackageName().startsWith("org.hamcrest.")
                                    && !c.getPackageName().equals("org.mockito")
                                    && !c.getPackageName().startsWith("org.mockito.")
                                    && !c.getPackageName().equals("nl.jqno.equalsverifier")
                                    && !c.getPackageName().startsWith("nl.jqno.equalsverifier.")
                                    && !c.getPackageName().startsWith("org.checkerframework.")
                                    && !c.getPackageName().equals("net.bytebuddy")
                                    && !c.getPackageName().startsWith("net.bytebuddy.")
                                    && !c.getPackageName().startsWith(Input.class.getPackageName())
                                    && !c.getPackageName().startsWith(Resource.class.getPackageName())
                                    && !c.getPackageName().startsWith(EngineGrpc.class.getPackageName())
                    )
                    .map(c -> {
                        try {
                            return c.load();
                        } catch (LinkageError e) {
                            throw new IllegalStateException(String.format("Failed to load class '%s' (package: '%s') from class path: %s", c, c.getPackageName(), e.getMessage()), e);
                        }
                    })
                    .filter(c -> c.isAnnotationPresent(ResourceType.class))
                    .filter(c -> {
                        if (Resource.class.isAssignableFrom(c)) { // must be a subclass of Resource
                            if (Reflection.isNestedClass(c)) { // must be static or standalone class (top level class)
                                throw new IllegalStateException(String.format(
                                        "Expected class '%s' annotated with '%s' to be static or standalone, but was nested. Make the class static or standalone.",
                                        c.getTypeName(), ResourceType.class.getTypeName()
                                ));
                            }
                            return true;
                        } else {
                            throw new IllegalStateException(String.format(
                                    "Expected class '%s' annotated with '%s' to be assignable from '%s', but was not.",
                                    c.getTypeName(), ResourceType.class.getTypeName(), Resource.class.getTypeName()
                            ));
                        }
                    })
                    .filter(c -> !Reflection.isNestedClass(c))
                    .map(c -> {
                        //noinspection unchecked
                        return (Class<Resource>) c; // checked in filter above
                    })
                    .map(c -> Tuples.of(c.getAnnotation(ResourceType.class), c))
                    .collect(
                            collectingAndThen(
                                    Collectors.groupingBy(
                                            g -> g.t1.type(),
                                            Collectors.mapping(
                                                    g -> Tuples.of(Optionals.ofBlank(g.t1.version()), g.t2),
                                                    ImmutableList.<Tuple2<Optional<String>, Class<Resource>>>toImmutableList()
                                            )
                                    ),
                                    ImmutableMap::copyOf
                            )
                    );

        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to read class path: %s", e.getMessage()), e);
        }
    }

    @Internal
    static Optional<Resource> tryConstruct(String type, String version, String urn) {
        var resourceType = tryGetResourceType(type, version);
        if (resourceType.isEmpty()) {
            return Optional.empty();
        }

        if (Reflection.isNestedClass(resourceType.get())) {
            throw new IllegalArgumentException(String.format(
                    "tryConstruct(String, String, String) cannot be used with nested classes, make class '%s' static or standalone",
                    resourceType.get().getTypeName()
            ));
        }

        // TODO: good candidate for an utility function in the Urn class, or a proper class
        var urnParts = urn.split("::");
        var urnName = urnParts[3];

        Constructor<?> constructorInfo = Arrays.stream(resourceType.get().getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 3)
                .peek(c -> c.setAccessible(true))
                .collect(toSingleton());

        var resourceOptions = resolveResourceOptions(resourceType.get(), urn);
        try {
            var resource = (Resource) constructorInfo.newInstance(new Object[]{urnName, null, resourceOptions});
            return Optional.of(resource);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(
                    "Couldn't instantiate the '%s' class using constructor: '%s', for resource type: '%s'",
                    resourceType.get().getTypeName(), constructorInfo, type
            ));
        }
    }

    private static ResourceOptions resolveResourceOptions(Class<?> resourceType, String urn) {
        if (CustomResource.class.isAssignableFrom(resourceType)) {
            return CustomResourceOptions.builder().setUrn(urn).build();
        }
        if (ComponentResource.class.isAssignableFrom(resourceType)) {
            return ComponentResourceOptions.builder().setUrn(urn).build();
        }
        throw new IllegalStateException(String.format("Unexpected resource type: '%s'", resourceType.getTypeName()));
    }

    @Internal
    static Optional<Class<Resource>> tryGetResourceType(String name, @Nullable String version) {
        Objects.requireNonNull(name);

        var minimalVersion = !Strings.isNullOrEmpty(version)
                ? SemanticVersion.parse(version) : SemanticVersion.of(0);

        var types = Maps.tryGetValue(ResourcePackages.resourceTypes.get(), name);
        if (types.isEmpty()) {
            return Optional.empty();
        }

        return types.get().stream()
                .map(vt -> Tuples.of(vt.t1.map(SemanticVersion::parse).orElse(minimalVersion), vt.t2))
                .filter(vt -> vt.t1.isGreaterOrEqualTo(minimalVersion)) // must be at least the minimal version
                .filter(vt -> Strings.isNullOrEmpty(version) || vt.t1.getMajor() == minimalVersion.getMajor()) // must have same major version
                .max(comparing(vt -> vt.t1)) // latest, filtered version
                .map(vt -> vt.t2);
    }

    // TODO
}
