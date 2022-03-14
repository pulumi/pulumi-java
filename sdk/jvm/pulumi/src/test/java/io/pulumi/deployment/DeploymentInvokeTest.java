package io.pulumi.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.TypeShape;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.annotations.OutputCustomType;
import io.pulumi.core.annotations.OutputCustomType.Constructor;
import io.pulumi.core.annotations.OutputCustomType.Parameter;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.resources.InvokeArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentInvokeTest {

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testCustomInvokes() {
        DeploymentTests.DeploymentMockBuilder.builder()
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
                .setSpyGlobalInstance();

        var out = CustomInvokes.doStuff(CustomArgs.Empty, InvokeOptions.Empty).applyVoid(r -> {
            assertThat(r).hasSize(1);
        });

        Internal.of(out).getDataAsync().join();
    }

    static class CustomInvokes {
        static Output<ImmutableList<ImmutableMap<String, Object>>> doStuff(
                @SuppressWarnings("SameParameterValue") CustomArgs args,
                @Nullable InvokeOptions options) {
            return Output.of(
                    Deployment.getInstance().invokeAsync(
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

    @OutputCustomType
    static class CustomResult {
        public final ImmutableList<ImmutableMap<String, Object>> result;

        @Constructor
        private CustomResult(@Parameter("result") ImmutableList<ImmutableMap<String, Object>> result) {
            this.result = result;
        }
    }
}
