package io.pulumi.resources;

import io.pulumi.core.Alias;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutputTests;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

import static io.pulumi.test.internal.assertj.PulumiAssertions.assertThatNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ResourceOptionsTest {
    @SuppressWarnings("unused")
    private static Stream<Arguments> testMergeSharedOptions() {
        return Stream.of(
                arguments(new TestResourceOptions(), new TestResourceOptions(), new TestResourceOptions(
                        null, null, Input.empty(), false, null,
                        null, null, null, null, null, null
                )),
                arguments(new TestResourceOptions(
                                null,
                                null,
                                null,
                                false,
                                List.of("a"),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        ),
                        new TestResourceOptions(
                                Input.of("id"),
                                null,
                                Input.empty(),
                                true,
                                List.of("b"),
                                "test",
                                null,
                                new CustomTimeouts(null, null, null),
                                List.of(),
                                List.of(),
                                "urn"
                        ),
                        new TestResourceOptions(
                                Input.of("id"),
                                null,
                                Input.empty(),
                                true,
                                List.of("a", "b"),
                                "test",
                                null,
                                new CustomTimeouts(null, null, null),
                                List.of(),
                                List.of(),
                                "urn"
                        )
                ) // FIXME
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMergeSharedOptions(ResourceOptions options1, ResourceOptions options2, ResourceOptions expected) {
        options1 = ResourceOptions.mergeSharedOptions(options1, options2);
        assertThat(options1.id != null ? InputOutputTests.waitFor(options1.id).getValueNullable() : null)
                .isEqualTo(expected.id != null ? InputOutputTests.waitFor(expected.id).getValueNullable() : null); // FIXME
        assertThat(options1.parent).isEqualTo(expected.parent);
        assertThatNullable(
                InputOutputTests.waitFor(options1.dependsOn).getValueNullable()
        ).containsAll(
                InputOutputTests.waitFor(expected.dependsOn).getValueNullable()
        );
        assertThat(options1.protect).isEqualTo(expected.protect);
        assertThatNullable(options1.ignoreChanges).containsAll(expected.ignoreChanges);
        assertThat(options1.version).isEqualTo(expected.version);
        assertThat(options1.provider).isEqualTo(expected.provider);
        assertThat(options1.customTimeouts).isEqualTo(expected.customTimeouts);
        assertThatNullable(options1.resourceTransformations).containsAll(expected.resourceTransformations);
        assertThatNullable(options1.aliases).containsAll(expected.aliases);
        assertThat(options1.urn).isEqualTo(expected.urn);
    }

    private static class TestResourceOptions extends ResourceOptions {
        public static final TestResourceOptions Empty = new TestResourceOptions();

        protected TestResourceOptions() { /* empty */ }

        public TestResourceOptions(@Nullable Input<String> id, @Nullable Resource parent, @Nullable Input<List<Resource>> dependsOn, boolean protect, @Nullable List<String> ignoreChanges, @Nullable String version, @Nullable ProviderResource provider, @Nullable CustomTimeouts customTimeouts, @Nullable List<ResourceTransformation> resourceTransformations, @Nullable List<Input<Alias>> aliases, @Nullable String urn) {
            super(id, parent, dependsOn, protect, ignoreChanges, version, provider, customTimeouts, resourceTransformations, aliases, urn);
        }
    }

}