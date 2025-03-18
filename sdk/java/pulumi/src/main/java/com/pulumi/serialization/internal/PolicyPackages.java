package com.pulumi.serialization.internal;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.annotations.PolicyPackResource;
import com.pulumi.core.annotations.PolicyPackStack;
import com.pulumi.core.annotations.PolicyPackType;
import com.pulumi.core.annotations.PolicyResourceType;
import com.pulumi.core.internal.Exceptions;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.PolicyResource;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@InternalUse
public class PolicyPackages {
    private static final Type listOfString = new Reflection.TypeReference<List<String>>() {
    }.type;

    public static class PolicyForResource {
        public final PolicyPackResource annotation;
        public final Method target;
        public final String type;
        public final Class<? extends PolicyResource> resourceClass;

        public PolicyForResource(PolicyPackResource annotation,
                                 Method target,
                                 String type,
                                 Class<? extends PolicyResource> resourceClass) {
            this.annotation = annotation;
            this.target = target;
            this.type = type;
            this.resourceClass = resourceClass;
        }
    }

    public static class PolicyForStack {
        public final PolicyPackStack annotation;
        public final Method target;

        public PolicyForStack(PolicyPackStack annotation,
                              Method target) {
            this.annotation = annotation;
            this.target = target;
        }
    }

    public static class PolicyPack {
        public final PolicyPackType annotation;
        public final PolicyForStack stackPolicy;
        public final ImmutableMap<String, PolicyForResource> resourcePolicies;

        public PolicyPack(PolicyPackType annotation,
                          PolicyForStack stackPolicy,
                          List<PolicyForResource> resourcePolicies) {
            var map = new HashMap<String, PolicyForResource>();
            for (var policy : resourcePolicies) {
                map.put(policy.type, policy);
            }

            this.annotation = annotation;
            this.stackPolicy = stackPolicy;
            this.resourcePolicies = ImmutableMap.copyOf(map);
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
        var policyPacks = new ArrayList<PolicyPack>();

        Reflection.enumerateClassesWithAnnotation(PolicyPackType.class, (c, annotationType) -> {
            var resourcePolicies = new ArrayList<PolicyForResource>();
            PolicyForStack stackPolicy = null;

            for (var m : c.getMethods()) {
                var annotationResource = m.getAnnotation(PolicyPackResource.class);
                if (annotationResource != null) {
                    if (!Reflection.isStaticMethod(m)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should be static", m, c);
                    }

                    var types = m.getGenericParameterTypes();
                    if (types.length != 2) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should have two parameters, a PolicyResource and a list of strings", m, c);
                    }

                    var classForResource = annotationResource.value();
                    var typeForResource = types[0];
                    var typeForViolations = types[1];

                    PolicyResourceType annotation = classForResource.getAnnotation(PolicyResourceType.class);
                    if (annotation == null || classForResource != typeForResource) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': first parameter has to be a subclass of Pulumi PolicyResource", m, c);
                    }

                    if (!Reflection.isSubclassOf(typeForViolations, List.class, String.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': second parameter has to be List<String>", m, c);
                    }

                    resourcePolicies.add(new PolicyForResource(annotationResource, m, annotation.type(), classForResource));
                }

                var annotationStack = m.getAnnotation(PolicyPackStack.class);
                if (annotationStack != null) {
                    if (!Reflection.isStaticMethod(m)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should be static", m, c);
                    }

                    var types = m.getGenericParameterTypes();
                    if (types.length != 2) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should have two parameters, a list of PolicyResource and a list of strings", m, c);
                    }

                    var typeForResources = types[0];
                    var typeForViolations = types[1];

                    if (!Reflection.isSubclassOf(typeForResources, Map.class, String.class, PolicyResource.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': first parameter has to be Map<String, PolicyResource>", m, c);
                    }

                    if (!Reflection.isSubclassOf(typeForViolations, Map.class, String.class, listOfString)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': second parameter has to be Map<String, List<String>>", m, c);
                    }

                    if (stackPolicy != null) {
                        throw Exceptions.newIllegalState(null, "Multiple methods of class '%s' declared as stack policies: %s and %s", c, stackPolicy.target, m);
                    }

                    stackPolicy = new PolicyForStack(annotationStack, m);
                }
            }

            if (!resourcePolicies.isEmpty() || stackPolicy != null) {
                policyPacks.add(new PolicyPack(annotationType, stackPolicy, resourcePolicies));
            }
        });

        return ImmutableList.copyOf(policyPacks);
    }
}
