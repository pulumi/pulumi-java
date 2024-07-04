package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Maps;
import com.pulumi.core.internal.Optionals;
import com.pulumi.core.internal.Reflection;
import com.pulumi.core.internal.SemanticVersion;
import com.pulumi.core.internal.Urn;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceOptions;
import pulumirpc.EngineGrpc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.pulumi.core.internal.PulumiCollectors.toSingleton;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;

@InternalUse
public class ResourcePackages {

    /**
     * Holds a cache of all Pulumi resources found on the classpath.
     * The resource type name is the key.
     */
    private static final Supplier<ImmutableMap<String, ImmutableList<ResourceTypeEntry>>> resourceTypes =
            Suppliers.memoize(ResourcePackages::discoverResourceTypes); // lazy init

    private final Log log;

    @SuppressWarnings("UnstableApiUsage")
    private static ImmutableMap<String, ImmutableList<ResourceTypeEntry>> discoverResourceTypes() {
        var loader = MoreObjects.firstNonNull(
                ResourcePackages.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        );
        final ClassPath classpath;
        try {
            classpath = ClassPath.from(loader);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to read class path: %s", e.getMessage()), e);
        }

        return classpath.getAllClasses().stream()
                // exclude early our dependencies and common packages almost certain to not contain what we want
                .filter(ResourcePackages::excludePackages)
                .map(c -> {
                    try {
                        return c.load();
                    } catch (LinkageError e) {
                        throw new IllegalStateException(String.format(
                                "Failed to load class '%s' (package: '%s') from class path: %s",
                                c, c.getPackageName(), e.getMessage()
                        ), e);
                    }
                })
                .filter(c -> c.isAnnotationPresent(ResourceType.class))
                .filter(c -> {
                    /* must be a subclass of Resource */
                    if (Resource.class.isAssignableFrom(c)) {
                        /* must be static or standalone class (top level class) */
                        if (Reflection.isNestedClass(c)) {
                            throw new IllegalStateException(String.format(
                                    "Expected class '%s' annotated with '%s' to be static or standalone, but was nested. " +
                                            "Make the class static or standalone.",
                                    c.getTypeName(), ResourceType.class.getTypeName()
                            ));
                        }
                        return true;
                    } else {
                        throw new IllegalStateException(String.format(
                                "Expected class '%s' to be annotated with '%s', to be assignable from '%s', but it was not.",
                                c.getTypeName(), ResourceType.class.getTypeName(), Resource.class.getTypeName()
                        ));
                    }
                })
                .filter(c -> !Reflection.isNestedClass(c))
                .map(c -> {
                    //noinspection unchecked
                    return (Class<Resource>) c; // checked in filters above
                })
                .map(c -> new ResourceTypeAndClass(c.getAnnotation(ResourceType.class), c))
                .collect(
                        collectingAndThen(
                                groupingBy(
                                        g -> g.annotation.type(),
                                        Collectors.mapping(
                                                g -> new ResourceTypeEntry(Optionals.ofBlank(g.annotation.version()), g.type),
                                                ImmutableList.<ResourceTypeEntry>toImmutableList()
                                        )
                                ),
                                ImmutableMap::copyOf
                        )
                );
    }

    @SuppressWarnings("UnstableApiUsage")
    private static boolean excludePackages(ClassInfo c) {
        return !c.getPackageName().isBlank()
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
                && !c.getPackageName().startsWith(Resource.class.getPackageName())
                && !c.getPackageName().startsWith(EngineGrpc.class.getPackageName())
                && !excludePackagesFromProperties(c);
    }

    private static boolean excludePackagesFromProperties(ClassInfo c) {
        String packages = System.getProperty("ADDITIONAL_EXCLUDES");
        if (packages != null) {
            String[] items = packages.split(",");
            for (String item : items){
                if (c.getPackageName().startsWith(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ResourcePackages(Log log) {
        this.log = requireNonNull(log);
    }

    @InternalUse
    Optional<Resource> tryConstruct(String type, String version, String urn) {
        this.log.excessive(
                "Deserialize/ResourcePackages: searching for type=%s version=%s urn=%s",
                type, version, urn
        );
        var resourceType = tryGetResourceType(type, version);
        if (resourceType.isEmpty()) {
            var message = String.format(
                    "Deserialize/ResourcePackages: can't find a resource: '%s'; version=%s urn=%s",
                    type, version, urn
            );
            this.log.debugOrExcessive(message, String.format(
                    "; Available resources type names:\n %s",
                    String.join("\n", ResourcePackages.resourceTypes.get().keySet())
            ));
            return Optional.empty();
        }
        log.excessive(String.format(
                "Deserialize/ResourcePackages: found resource: '%s'; type=%s version=%s urn=%s",
                resourceType, type, version, urn
        ));

        if (Reflection.isNestedClass(resourceType.get())) {
            throw new IllegalArgumentException(String.format(
                    "tryConstruct(String, String, String) cannot be used with nested classes, " +
                            "make class '%s' static or standalone",
                    resourceType.get().getTypeName()
            ));
        }

        var urnParsed = Urn.parse(urn);
        var urnName = urnParsed.name;

        var constructorInfo =
                Arrays.stream(resourceType.get().getDeclaredConstructors())
                        .filter(c -> c.getParameterCount() == 3)
                        // Remove confusion of constructors with the second param of type:
                        //     Output<String> id
                        .filter(c -> !c.getParameterTypes()[1].equals(Output.class))
                        .collect(toSingleton(cause ->
                                new IllegalArgumentException(String.format(
                                        "Resource provider error. Could not find a constructor for resource %s" +
                                                " with the following signature:" +
                                                " `(String name, SomeResourceArgs args, CustomResourceOptions options)`" +
                                                ", got: `%s`",
                                        resourceType.get(), cause
                                ))
                        ));

        constructorInfo.setAccessible(true);

        this.log.excessive(
                "Deserialize/ResourcePackages: constructing using constructor=%s type=%s version=%s urn=%s",
                constructorInfo, resourceType.get().getTypeName(), version, urn
        );
        var resourceOptions = resolveResourceOptions(resourceType.get(), urn);
        try {
            var resource = (Resource) constructorInfo.newInstance(new Object[]{urnName, null, resourceOptions});
            return Optional.of(resource);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(
                    "Couldn't instantiate class '%s' using constructor: '%s', for resource type: '%s'",
                    resourceType.get().getTypeName(), constructorInfo, type
            ));
        } finally {
            constructorInfo.setAccessible(false);
        }
    }

    private ResourceOptions resolveResourceOptions(Class<?> resourceType, String urn) {
        if (CustomResource.class.isAssignableFrom(resourceType)) {
            return CustomResourceOptions.builder().urn(urn).build();
        }
        if (ComponentResource.class.isAssignableFrom(resourceType)) {
            return ComponentResourceOptions.builder().urn(urn).build();
        }
        throw new IllegalStateException(String.format("Unexpected resource type: '%s'", resourceType.getTypeName()));
    }

    @InternalUse
    Optional<Class<Resource>> tryGetResourceType(String name, @Nullable String version) {
        requireNonNull(name);

        var minimalVersion = !isNullOrEmpty(version)
                ? SemanticVersion.parse(version) : SemanticVersion.of(0);

        var types = Maps.tryGetValue(ResourcePackages.resourceTypes.get(), name);
        if (types.isEmpty()) {
            return Optional.empty();
        }

        return types.get().stream()
                .map(resource -> new SemanticVersionAndClass(
                        resource.version.map(SemanticVersion::parse).orElse(minimalVersion), resource.type
                ))
                /* must be at least the minimal version */
                .filter(resource -> resource.semanticVersion.isGreaterOrEqualTo(minimalVersion))
                /* must have same major version */
                .filter(resource ->
                        isNullOrEmpty(version) || resource.semanticVersion.getMajor() == minimalVersion.getMajor()
                )
                /* latest, filtered version */
                .max(comparing(resource -> resource.semanticVersion))
                .map(resource -> resource.type);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class ResourceTypeEntry {
        public final Optional<String> version;
        public final Class<Resource> type;

        private ResourceTypeEntry(Optional<String> version, Class<Resource> type) {
            this.version = requireNonNull(version);
            this.type = requireNonNull(type);
        }
    }

    private static final class ResourceTypeAndClass {
        public final ResourceType annotation;
        public final Class<Resource> type;

        private ResourceTypeAndClass(ResourceType annotation, Class<Resource> type) {
            this.annotation = requireNonNull(annotation);
            this.type = requireNonNull(type);
        }
    }

    private static final class SemanticVersionAndClass {
        public final SemanticVersion semanticVersion;
        public final Class<Resource> type;

        private SemanticVersionAndClass(SemanticVersion semanticVersion, Class<Resource> type) {
            this.semanticVersion = requireNonNull(semanticVersion);
            this.type = requireNonNull(type);
        }
    }
}
