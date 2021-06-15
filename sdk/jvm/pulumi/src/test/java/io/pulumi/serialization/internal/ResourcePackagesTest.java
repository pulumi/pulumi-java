package io.pulumi.serialization.internal;

import io.pulumi.core.internal.annotations.ResourceType;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ResourcePackagesTest {

    @Test
    void testUnknownNotFound() {
        ResourcePackages.tryGetResourceType("test:index/UnknownResource", null).ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        ResourcePackages.tryGetResourceType("test:index/UnknownResource", "").ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        ResourcePackages.tryGetResourceType("unknown:index/TestResource", "0.0.1").ifPresent(
                r -> fail("Unknown resource found: %s", r)
        );
        ResourcePackages.tryGetResourceType("unknown:index/AnotherResource", "1.0.0").ifPresent(
                r -> fail("Resource with non-matching package version found: %s", r)
        );
    }

    @Test
    void NullReturnsHighestVersion() {
        ResourcePackages.tryGetResourceType("test:index/TestResource", null).ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version202TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void BlankReturnsHighestVersion() {
        ResourcePackages.tryGetResourceType("test:index/TestResource", "").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version202TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void MajorVersionRespected() {
        ResourcePackages.tryGetResourceType("test:index/TestResource", "1.0.0").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(Version102TestResource.class),
                () -> fail("Test resource not found")
        );
    }

    @Test
    void WildcardSelectedIfOthersDontMatch() {
        ResourcePackages.tryGetResourceType("test:index/TestResource", "3.0.0").ifPresentOrElse(
                type -> assertThat(type).isEqualTo(WildcardTestResource.class),
                () -> fail("Test resource not found")
        );
    }

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

    @ResourceType(type = "test:index/UnrelatedResource", version = "1.0.3")
    private static class OtherResource extends CustomResource {
        public OtherResource(String type, String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super(type, name, args, options);
        }
    }
}
