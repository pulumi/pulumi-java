package io.pulumi.resources;

import io.pulumi.core.Alias;
import io.pulumi.core.Output;
import io.pulumi.core.OutputTests;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.internal.CurrentDeployment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.pulumi.test.internal.assertj.PulumiAssertions.assertThatNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ResourceOptionsTest {
    @SuppressWarnings("unused")
    private static Stream<Arguments> testMergeSharedOptions() {
        return Stream.of(
                TestCase.builder()
                        .arg1(__ -> new TestResourceOptions())
                        .arg2(__ -> new TestResourceOptions())
                        .expected(out -> new TestResourceOptions(
                                null, null, out.of(List.of()), false, null,
                                null, null, null, null, null, null, null
                        ))
                        .buildArguments(),

                TestCase.builder()
                        .arg1(__ -> new TestResourceOptions(
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
                                null,
                                null
                        ))
                        .arg2(out -> new TestResourceOptions(
                                out.of("id"),
                                null,
                                out.empty(),
                                true,
                                List.of("b"),
                                "test",
                                null,
                                new CustomTimeouts(null, null, null),
                                List.of(),
                                List.of(),
                                "urn",
                                List.of()
                        ))
                        .expected(out ->
                                new TestResourceOptions(
                                        out.of("id"),
                                        null,
                                        out.of(List.of()),
                                        true,
                                        List.of("a", "b"),
                                        "test",
                                        null,
                                        new CustomTimeouts(null, null, null),
                                        List.of(),
                                        List.of(),
                                        "urn",
                                        List.of()
                                ))
                        .buildArguments()
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMergeSharedOptions(TestCase tc) {
        var deployment = OutputTests.testContext().deployment;
        CurrentDeployment.withCurrentDeployment(deployment, () -> {
            var expected = tc.expected();
            var options1 = ResourceOptions.mergeSharedOptions(deployment, tc.arg1(), tc.arg2());
            assertThat(options1.id != null ? OutputTests.waitFor(options1.id).getValueNullable() : null)
                    .isEqualTo(expected.id != null ? OutputTests.waitFor(expected.id).getValueNullable() : null); // FIXME
            assertThat(options1.parent).isEqualTo(expected.parent);
            assertThatNullable(
                    OutputTests.waitFor(options1.dependsOn).getValueNullable()
            ).containsAll(
                    OutputTests.waitFor(expected.dependsOn).getValueNullable()
            );
            assertThat(options1.protect).isEqualTo(expected.protect);
            assertThatNullable(options1.ignoreChanges).containsAll(expected.ignoreChanges);
            assertThat(options1.version).isEqualTo(expected.version);
            assertThat(options1.provider).isEqualTo(expected.provider);
            assertThat(options1.customTimeouts).isEqualTo(expected.customTimeouts);
            assertThatNullable(options1.resourceTransformations).containsAll(expected.resourceTransformations);
            assertThatNullable(options1.aliases).containsAll(expected.aliases);
            assertThat(options1.urn).isEqualTo(expected.urn);
            return (Void) null;
        });
    }

    private static class TestCase {
        public Function<OutputBuilder, TestResourceOptions> arg1Supplier = __ -> new TestResourceOptions();
        public Function<OutputBuilder ,TestResourceOptions> arg2Supplier = __ -> new TestResourceOptions();
        public Function<OutputBuilder, TestResourceOptions> expectedSupplier = __ -> new TestResourceOptions();

        public static Builder builder() {
            return new Builder();
        }

        private OutputBuilder ob() {
            return OutputBuilder.forDeployment(CurrentDeployment.getCurrentDeploymentOrThrow());
        }

        public TestResourceOptions arg1() {
            return this.arg1Supplier.apply(ob());
        }

        public TestResourceOptions arg2() {
            return this.arg2Supplier.apply(ob());
        }

        public TestResourceOptions expected() {
            return this.expectedSupplier.apply(ob());
        }

        public static class Builder {
            private final TestCase tc = new TestCase();

            public Builder expected(Function<OutputBuilder, TestResourceOptions> make) {
                tc.expectedSupplier = make;
                return this;
            }

            public Builder arg1(Function<OutputBuilder, TestResourceOptions> make) {
                tc.arg1Supplier = make;
                return this;
            }

            public Builder arg2(Function<OutputBuilder, TestResourceOptions> make) {
                tc.arg2Supplier = make;
                return this;
            }

            public TestCase build() {
                return tc;
            }

            public Arguments buildArguments() {
                return arguments(build());
            }
        }
    }

    private static class TestResourceOptions extends ResourceOptions {
        protected TestResourceOptions() {
        }

        public TestResourceOptions(
                @Nullable Output<String> id,
                @Nullable Resource parent,
                @Nullable Output<List<Resource>> dependsOn,
                boolean protect,
                @Nullable List<String> ignoreChanges,
                @Nullable String version,
                @Nullable ProviderResource provider,
                @Nullable CustomTimeouts customTimeouts,
                @Nullable List<ResourceTransformation> resourceTransformations,
                @Nullable List<Output<Alias>> aliases,
                @Nullable String urn,
                @Nullable List<String> replaceOnChanges
        ) {
            super(id, parent, dependsOn, protect, ignoreChanges, version, provider, customTimeouts,
                    resourceTransformations, aliases, urn, replaceOnChanges);
        }
    }
}
