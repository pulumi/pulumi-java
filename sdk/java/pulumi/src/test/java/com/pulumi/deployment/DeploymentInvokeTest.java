package com.pulumi.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Constructor;
import com.pulumi.core.annotations.CustomType.Parameter;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.resources.InvokeArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
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
                .build();

        var out = CustomInvokes.doStuff(CustomArgs.Empty, InvokeOptions.Empty).applyValue(r -> {
            assertThat(r).hasSize(1);
            return (Void) null;
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

    @CustomType
    static class CustomResult {
        public final ImmutableList<ImmutableMap<String, Object>> result;

        @Constructor
        private CustomResult(@Parameter("result") ImmutableList<ImmutableMap<String, Object>> result) {
            this.result = result;
        }
    }

    @Test
    void testInvokeDoesNotCallMonitorWhenInputsNotKnown() {
        var mocks = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new Mocks() {
                    @Override
                    public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
                       throw new RuntimeException("new Resource not implemented");
                    }

                    @Override
                    public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
                        throw new RuntimeException("callAsync not implemented");
                    }
                })
                .build();
        var result = mocks.runTestAsync(ctx -> {
            var unk = new OutputInternal<String>(OutputData.unknown());
            var args = new IdentityArgs(unk);
            ctx.export("out", IdentityFunctions.invokeIdentity(args, new InvokeOptions()));
        }).join().throwOnError();
        assertThat(OutputTests.waitFor(result.output("out")).isKnown()).isFalse();
    }

    static class IdentityFunctions {
        public static Output<String> invokeIdentity(IdentityArgs args, @Nullable InvokeOptions options) {
            return Deployment.getInstance().invoke("tests:custom:identity",
                    TypeShape.of(String.class),
                    args,
                    options);
        }
    }

    static class IdentityArgs extends InvokeArgs {
        @Import(name = "incoming")
        public final Output<String> incoming;

        IdentityArgs(Output<String> incoming) {
            this.incoming = Objects.requireNonNull(incoming);
        }
    }
}
