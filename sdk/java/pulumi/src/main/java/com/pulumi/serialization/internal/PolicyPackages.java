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
import com.pulumi.resources.PolicyManager;
import com.pulumi.resources.PolicyResource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@InternalUse
public class PolicyPackages {
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
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should have two parameters, a PolicyManager and a PolicyResource", m, c);
                    }

                    var typeForManager = types[0];
                    var typeForResource = types[1];
                    Class<PolicyResource> classForResource = Reflection.getRawType(typeForResource);

                    if (!Reflection.sameType(typeForManager, PolicyManager.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': first parameter has to be PolicyManager", m, c);
                    }

                    if (!Reflection.isSubclassOf(typeForResource, PolicyResource.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': second parameter has to be a subclass of Pulumi PolicyResource", m, c);
                    }

                    PolicyResourceType annotation = classForResource.getAnnotation(PolicyResourceType.class);
                    if (annotation == null) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': second parameter has to be a subclass of Pulumi PolicyResource", m, c);
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
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': it should have two parameters, a PolicyManager and a list of PolicyResource", m, c);
                    }

                    var typeForManager = types[0];
                    var typeForResources = types[1];

                    if (!Reflection.sameType(typeForManager, PolicyManager.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': first parameter has to be PolicyManager", m, c);
                    }

                    if (!Reflection.isSubclassOf(typeForResources, List.class, PolicyResource.class)) {
                        throw Exceptions.newIllegalState(null, "Method '%s' of class '%s': second parameter has to be List<PolicyResource>", m, c);
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
