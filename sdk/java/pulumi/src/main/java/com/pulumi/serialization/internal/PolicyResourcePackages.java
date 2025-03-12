package com.pulumi.serialization.internal;

import com.google.common.base.Suppliers;
import com.pulumi.core.annotations.PolicyResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.PolicyResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@InternalUse
public class PolicyResourcePackages {
    public static class PolicyResourceEntry {
        public final String type;
        public final String version;

        public PolicyResourceEntry(String type,
                                   String version) {
            this.type = type;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PolicyResourceEntry)) return false;
            PolicyResourceEntry that = (PolicyResourceEntry) o;
            return Objects.equals(type, that.type) && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, version);
        }
    }

    /**
     * Holds a cache of all Pulumi policy packs found on the classpath.
     */
    private static final Supplier<Map<PolicyResourceEntry, Class<? extends PolicyResource>>> policyResourceTypes =
            Suppliers.memoize(PolicyResourcePackages::discoverPolicyResourceTypes); // lazy init

    public static Class<? extends PolicyResource> resolveType(String type,
                                                              String version) {
        var types = policyResourceTypes.get();
        var key = new PolicyResourceEntry(type, version);
        return types.get(key);
    }

    private static Map<PolicyResourceEntry, Class<? extends PolicyResource>> discoverPolicyResourceTypes() {
        var types = new HashMap<PolicyResourceEntry, Class<? extends PolicyResource>>();

        Reflection.enumerateClassesWithAnnotation(PolicyResourceType.class, (c, annotationType) -> {
            var key = new PolicyResourceEntry(annotationType.type(), annotationType.version());
            types.put(key, c.asSubclass(PolicyResource.class));
        });

        return types;
    }
}
