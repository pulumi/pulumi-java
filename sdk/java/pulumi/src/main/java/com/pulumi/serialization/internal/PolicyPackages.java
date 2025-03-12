package com.pulumi.serialization.internal;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.annotations.PolicyPackMethod;
import com.pulumi.core.annotations.PolicyPackType;
import com.pulumi.core.annotations.PolicyResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.PolicyResource;

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
        public final Class<? extends PolicyResource> resourceClass;

        public Policy(PolicyPackMethod annotation,
                      Method target,
                      String type,
                      Class<? extends PolicyResource> resourceClass) {
            this.annotation = annotation;
            this.target = target;
            this.type = type;
            this.resourceClass = resourceClass;
        }
    }

    public static class PolicyPack {
        public final PolicyPackType annotation;
        public final ImmutableMap<String, Policy> policies;

        public PolicyPack(PolicyPackType annotation,
                          List<Policy> policies) {
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
        var policyPacks = new ArrayList<PolicyPack>();

        Reflection.enumerateClassesWithAnnotation(PolicyPackType.class, (c, annotationType) -> {
            var policies = new ArrayList<Policy>();
            for (var m : c.getMethods()) {
                var annotationMethod = m.getAnnotation(PolicyPackMethod.class);
                if (annotationMethod != null) {
                    if (!Reflection.isStaticMethod(m)) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': it should be " +
                                "static", m, c));
                    }

                    var types = m.getGenericParameterTypes();
                    if (types.length != 2) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': it should have two " +
                                "parameters, a PolicyResource and a " +
                                "list of strings", m, c));
                    }

                    var classForResource = annotationMethod.value();
                    var classForViolations = types[1];

                    PolicyResourceType annotation = annotationMethod.value().getAnnotation(PolicyResourceType.class);
                    if (!PolicyResource.class.isAssignableFrom(classForResource) || annotation == null) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': first parameter has" +
                                " to be a subclass of Pulumi " +
                                "PolicyResource", m, c));
                    }

                    if (!Reflection.isSubclassOf(List.class, classForViolations)) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': second parameter " +
                                "has to be List<String>", m, c));
                    }

                    if (Reflection.getTypeArgument(classForViolations, 0) != String.class) {
                        throw new IllegalStateException(String.format("Method '%s' of class '%s': second parameter " +
                                "has to be List<String>", m, c));
                    }

                    policies.add(new Policy(annotationMethod, m, annotation.type(),
                            Reflection.getRawType(classForResource)));
                }
            }

            if (!policies.isEmpty()) {
                policyPacks.add(new PolicyPack(annotationType, policies));
            }
        });

        return ImmutableList.copyOf(policyPacks);
    }
}
