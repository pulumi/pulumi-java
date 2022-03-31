package io.pulumi.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.TypeShape;
import io.pulumi.core.annotations.CustomType;
import io.pulumi.core.annotations.CustomType.Constructor;
import io.pulumi.core.annotations.CustomType.Parameter;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.resources.InvokeArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentInvokeTest {

    @Test
    void testCustomInvokes() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new Mocks() {
                    @Override
                    public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
                        return CompletableFuture.completedFuture(
                                ImmutableMap.of(
                                        "result",
                                        ImmutableList.of(ImmutableMap.of("root",
                                                ImmutableMap.of("test1", ImmutableList.of("1", "2", "3"))
                                        ))
                                )
                        );
                    }
                })
                .buildSpyInstance();

        var out = CustomInvokes.doStuff(mock.getDeployment(),
                CustomArgs.Empty, InvokeOptions.Empty).applyVoid(r -> {
            assertThat(r).hasSize(1);
        });

        Internal.of(out).getDataAsync().join();
    }

    static class CustomInvokes {
        static Output<ImmutableList<ImmutableMap<String, Object>>> doStuff(
                Deployment deployment,
                @SuppressWarnings("SameParameterValue") CustomArgs args,
                @Nullable InvokeOptions options) {
            var output = OutputBuilder.forDeployment(deployment);
            return output.of(
                    deployment.invokeAsync(
                            "tests:custom:stuff",
                            TypeShape.of(CustomResult.class),
                            args,
                            options
                    ).thenApply(result -> result.result)
            );
        }
    }

    static class CustomArgs extends InvokeArgs {
        public static final CustomArgs Empty = new CustomArgs(null, null);

        @Import(name = "text")
        @Nullable
        public final String text;

        @Import(name = "defaultNamespace")
        @Nullable
        public final String defaultNamespace;

        CustomArgs(@Nullable String text, @Nullable String defaultNamespace) {
            this.text = text;
            this.defaultNamespace = defaultNamespace;
        }
    }

    @CustomType
    static class CustomResult {
        public final ImmutableList<ImmutableMap<String, Object>> result;

        @Constructor
        private CustomResult(@Parameter("result") ImmutableList<ImmutableMap<String, Object>> result) {
            this.result = result;
        }
    }
}
