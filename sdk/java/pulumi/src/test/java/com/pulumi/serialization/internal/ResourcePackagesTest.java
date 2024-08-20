package com.pulumi.serialization.internal;

import com.pulumi.Log;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ResourcePackagesTest {

    @Test
    void testUnknownNotFound() {
        var resourcePackages = new ResourcePackages(Log.ignore());

        resourcePackages.tryGetResourceType("test:index/UnknownResource", null).ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        resourcePackages.tryGetResourceType("test:index/UnknownResource", "").ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        resourcePackages.tryGetResourceType("unknown:index/TestResource", "0.0.1").ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        resourcePackages.tryGetResourceType("unknown:index/AnotherResource", "1.0.0").ifPresent(
                r -> fail("Resource with non-matching package version found: %s", r)
        );
    }

    @Test
    void NullReturnsHighestVersion() {
        var resourcePackages = new ResourcePackages(Log.ignore());
        resourcePackages.tryGetResourceType("test:index/TestResource", null).ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version202TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void BlankReturnsHighestVersion() {
        var resourcePackages = new ResourcePackages(Log.ignore());
        resourcePackages.tryGetResourceType("test:index/TestResource", "").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version202TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void MajorVersionRespected() {
        var resourcePackages = new ResourcePackages(Log.ignore());
        resourcePackages.tryGetResourceType("test:index/TestResource", "1.0.0").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version102TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void WildcardSelectedIfOthersDontMatch() {
        var resourcePackages = new ResourcePackages(Log.ignore());
        resourcePackages.tryGetResourceType("test:index/TestResource", "3.0.0").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(WildcardTestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void HyphenedComponentResource() {
        var resourcePackages = new ResourcePackages(Log.ignore());
        resourcePackages.tryGetResourceType("test-hyphen:index/AComponentResource", "").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(AComponentResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void ExcludedResource() {
        System.setProperty("pulumi.resourcepackages.excludes", ExcludedResource.class.getPackageName());
        var resourcePackages = new ResourcePackages(Log.ignore());
        Optional<Class<Resource>> resourceType = resourcePackages.tryGetResourceType("test-excludePackages:index/ExcludedResource", "");

        if (resourceType.isPresent()) {
            fail("Test resource was found");
        } else {
            assertThat(resourceType).isNotPresent();
        }
    }

    @SuppressWarnings("unused") // Accessed by reflection
    @ResourceType(type = "test:index/TestResource", version = "1.0.1-alpha1")
    private static class Version101TestResource extends CustomResource {
        public Version101TestResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @ResourceType(type = "test:index/TestResource", version = "1.0.2")
    private static class Version102TestResource extends CustomResource {
        public Version102TestResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @ResourceType(type = "test:index/TestResource", version = "2.0.2")
    private static class Version202TestResource extends CustomResource {
        public Version202TestResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @ResourceType(type = "test:index/TestResource", version = "")
    private static class WildcardTestResource extends CustomResource {
        public WildcardTestResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @SuppressWarnings("unused") // Accessed by reflection
    @ResourceType(type = "test:index/UnrelatedResource", version = "1.0.3")
    private static class OtherResource extends CustomResource {
        public OtherResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @ResourceType(type = "test-hyphen:index/AComponentResource")
    private static class AComponentResource extends ComponentResource {
        public AComponentResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options) {
            super(type, name, args, options);
        }
    }

    @ResourceType(type = "test-excludePackages:index/ExcludedResource")
    private static class ExcludedResource extends ComponentResource {
        public ExcludedResource(String type, String name, @Nullable ResourceArgs args, @Nullable ComponentResourceOptions options) {
            super(type, name, args, options);
        }
    }
}
