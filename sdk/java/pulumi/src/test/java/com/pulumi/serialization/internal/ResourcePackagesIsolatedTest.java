package com.pulumi.serialization.internal;

import com.pulumi.Log;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;



public class ResourcePackagesIsolatedTest {

    @Test
    void ExcludedResource() {
        try {
            System.setProperty("pulumi.resourcepackages.excludes", ResourcePackagesIsolatedTest.ExcludedResource.class.getPackageName());
            var resourcePackages = new ResourcePackages(Log.ignore());
            Optional<Class<Resource>> resourceType = resourcePackages.tryGetResourceType("test-excludePackages:index/ExcludedResource", null);

            if (resourceType.isPresent()) {
                fail("Test resource was found");
            } else {
                assertThat(resourceType).isNotPresent();
            }
        } finally {
            // Clear the system property after the test
            System.clearProperty("pulumi.resourcepackages.excludes");
        }
    }

    @ResourceType(type = "test-excludePackages:index/ExcludedResource")
    private static class ExcludedResource extends ComponentResource {
        public ExcludedResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options) {
            super(type, name, args, options);
        }
    }

}
