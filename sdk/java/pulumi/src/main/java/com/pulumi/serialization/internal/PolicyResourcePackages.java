package com.pulumi.serialization.internal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.reflect.ClassPath;
import com.pulumi.core.annotations.PolicyResourceType;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.PolicyResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@InternalUse
public class PolicyResourcePackages
{
    public static class PolicyResourceEntry
    {
        public final String type;
        public final String version;

        public PolicyResourceEntry(String type,
                                   String version)
        {
            this.type = type;
            this.version = version;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof PolicyResourceEntry)) return false;
            PolicyResourceEntry that = (PolicyResourceEntry) o;
            return Objects.equals(type, that.type) && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, version);
        }
    }

    /**
     * Holds a cache of all Pulumi policy packs found on the classpath.
     */
    private static final Supplier<Map<PolicyResourceEntry, Class<? extends PolicyResource>>> policyResourceTypes =
            Suppliers.memoize(PolicyResourcePackages::discoverPolicyResourceTypes); // lazy init

    public static Class<? extends PolicyResource> resolveType(String type,
                                                              String version)
    {
        var types = policyResourceTypes.get();
        var key   = new PolicyResourceEntry(type, version);
        return types.get(key);
    }

    private static Map<PolicyResourceEntry, Class<? extends PolicyResource>> discoverPolicyResourceTypes()
    {
        var loader = MoreObjects.firstNonNull(
                PolicyResourcePackages.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        );
        final ClassPath classpath;
        try
        {
            classpath = ClassPath.from(loader);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(String.format("Failed to read class path: %s", e.getMessage()), e);
        }

        var types = new HashMap<PolicyResourceEntry, Class<? extends PolicyResource>>();

        for (var classInfo : classpath.getAllClasses())
        {
            // exclude early our dependencies and common packages almost certain to not contain what we want
            if (!ResourcePackages.excludePackages(classInfo)) continue;

            Class<?> c;
            try
            {
                c = classInfo.load();
            }
            catch (LinkageError e)
            {
                throw new IllegalStateException(String.format(
                        "Failed to load class '%s' (package: '%s') from class path: %s",
                        classInfo, classInfo.getPackageName(), e.getMessage()
                ), e);
            }

            var annotationType = c.getAnnotation(PolicyResourceType.class);
            if (annotationType != null)
            {
                var key = new PolicyResourceEntry(annotationType.type(), annotationType.version());
                types.put(key, c.asSubclass(PolicyResource.class));
            }
        }

        return types;
    }
}
