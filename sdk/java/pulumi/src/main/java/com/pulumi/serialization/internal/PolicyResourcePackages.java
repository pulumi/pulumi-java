package com.pulumi.serialization.internal;

import com.google.common.base.Suppliers;
import com.pulumi.core.annotations.PolicyResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.PolicyResource;
import com.pulumi.resources.PolicyResourceInput;
import com.pulumi.resources.PolicyResourceOutput;

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

    private static class InputsAndOutputs {
        final Map<PolicyResourceEntry, Class<? extends PolicyResourceInput>> inputs = new HashMap<>();
        final Map<PolicyResourceEntry, Class<? extends PolicyResourceOutput>> outputs = new HashMap<>();
    }

    /**
     * Holds a cache of all Pulumi policy packs found on the classpath.
     */
    private static final Supplier<InputsAndOutputs> policyResourceTypes =
            Suppliers.memoize(PolicyResourcePackages::discoverPolicyResourceTypes); // lazy init

    public static Class<? extends PolicyResourceInput> resolveInputType(String type,
                                                                        String version) {
        var types = policyResourceTypes.get();
        var key = new PolicyResourceEntry(type, version);
        return types.inputs.get(key);
    }

    public static Class<? extends PolicyResourceOutput> resolveOutputType(String type,
                                                                          String version) {
        var types = policyResourceTypes.get();
        var key = new PolicyResourceEntry(type, version);
        return types.outputs.get(key);
    }

    private static InputsAndOutputs discoverPolicyResourceTypes() {
        var res = new InputsAndOutputs();

        Reflection.enumerateClassesWithAnnotation(PolicyResourceType.class, (c, annotationType) -> {
            var key = new PolicyResourceEntry(annotationType.type(), annotationType.version());

            if (Reflection.isSubclassOf(c, PolicyResourceInput.class)) {
                res.inputs.put(key, c.asSubclass(PolicyResourceInput.class));
            } else if (Reflection.isSubclassOf(c, PolicyResourceOutput.class)) {
                res.outputs.put(key, c.asSubclass(PolicyResourceOutput.class));
            }
        });

        return res;
    }
}
