package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.pulumi.core.annotations.PolicyPackMethod;
import com.pulumi.core.annotations.PolicyPackType;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@InternalUse
public class PolicyPackages {
    public static class Policy {
        public final PolicyPackMethod annotation;
        public final Method target;
        public final String type;
        public final Class<? extends Resource> resourceClass;
        public final Class<? extends ResourceArgs> resourceArgsClass;

        public Policy(PolicyPackMethod annotation,
                      Method target,
                      String type,
                      Class<? extends Resource> resourceClass,
                      Class<? extends ResourceArgs> resourceArgsClass) {
            this.annotation = annotation;
            this.target = target;
            this.type = type;
            this.resourceClass = resourceClass;
            this.resourceArgsClass = resourceArgsClass;
        }
    }

    public static class PolicyPack {
        public final PolicyPackType annotation;
        public final ImmutableMap<String, Policy> policies;

        public PolicyPack(PolicyPackType annotation, List<Policy> policies) {
            var map = new HashMap<String, Policy>();
            for (var policy : policies) {
                map.put(policy.type, policy);
            }

            this.annotation = annotation;
            this.policies = ImmutableMap.copyOf(map);
        }
    }

    /**
     * Holds a cache of all Pulumi policy packs found on the classpath.
     */
    private static final Supplier<ImmutableList<PolicyPack>> policyTypes =
            Suppliers.memoize(PolicyPackages::discoverPolicyTypes); // lazy init

    public static ImmutableList<PolicyPack> get() {
        return policyTypes.get();
    }

    private static ImmutableList<PolicyPack> discoverPolicyTypes() {
        var loader = MoreObjects.firstNonNull(
                PolicyPackages.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        );
        final ClassPath classpath;
        try {
            classpath = ClassPath.from(loader);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to read class path: %s", e.getMessage()), e);
        }

        var policyPacks = new ArrayList<PolicyPack>();

        for (var classInfo : classpath.getAllClasses()) {
            // exclude early our dependencies and common packages almost certain to not contain what we want
            if (!ResourcePackages.excludePackages(classInfo)) continue;

            Class<?> c;
            try {
                c = classInfo.load();
            } catch (LinkageError e) {
                throw new IllegalStateException(String.format(
                        "Failed to load class '%s' (package: '%s') from class path: %s",
                        classInfo, classInfo.getPackageName(), e.getMessage()
                ), e);
            }

            var annotationType = c.getAnnotation(PolicyPackType.class);
            if (annotationType == null) continue;

            var policies = new ArrayList<Policy>();
            for (var m : c.getMethods()) {
                var annotationMethod = m.getAnnotation(PolicyPackMethod.class);
                if (annotationMethod != null) {
                    if (!Reflection.isStaticMethod(m)) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': it should be static", m, c));
                    }

                    var types = m.getGenericParameterTypes();
                    if (types.length != 2) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': it should have two parameters, a Resource and a list of strings", m, c));
                    }

                    var classForResource = annotationMethod.value();
                    var classForResourceArgs = Reflection.getRawType(types[0]);
                    var classForViolations = types[1];

                    ResourceType annotation = annotationMethod.value().getAnnotation(ResourceType.class);
                    if (!Resource.class.isAssignableFrom(classForResource) || annotation == null) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': first parameter has to be a subclass of Pulumi Resource", m, c));
                    }

                    if (!Reflection.isSubclassOf(List.class, classForViolations)) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': second parameter has to be List<String>", m, c));
                    }

                    if (Reflection.getTypeArgument(classForViolations, 0) != String.class) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': second parameter has to be List<String>", m, c));
                    }

                    policies.add(new Policy(annotationMethod, m, annotation.type(), Reflection.getRawType(classForResource), extractResourceArgsClass(classForResource)));
                }
            }

            if (!policies.isEmpty()) {
                policyPacks.add(new PolicyPack(annotationType, policies));
            }
        }

        return ImmutableList.copyOf(policyPacks);
    }

    private static Class<? extends ResourceArgs> extractResourceArgsClass(Class<?> t) {
        for (var constructor : t.getConstructors()) {
            var constructorTypeParameters = constructor.getParameterTypes();
            if (constructorTypeParameters.length == 2 && constructorTypeParameters[0] == String.class) {
                return Reflection.getRawType(constructorTypeParameters[1]);
            }
        }

        throw new IllegalStateException(String.format("Unable to extract constructor with (string, ResourceArgs) signature from class %s", t));
    }
}
