package com.pulumi.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Setter;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.resources.Resource;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.lang.Thread;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentInvokeDependsOnTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testCustomInvokesDependsOntestCustomInvokesDependsOn() {
        var marker = new ResolveMarker();
        
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(true).build())
                .mocks(new Mocks() {
                    @Override
                    public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
                        Objects.requireNonNull(args.type);
                        CompletableFuture<ResourceResult> result = CompletableFuture.supplyAsync(() -> {
                            try {
                                // Delay the resource creation to ensure we await it in the invoke
                                Thread.sleep(3000);
                                marker.resolved = true;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                            return ResourceResult.of(Optional.of(args.id + "_id"), ImmutableMap.of("prop", "some value")); 
                        });
                        return result;
                    }

                    @Override
                    public CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
                        return CompletableFuture.completedFuture(
                                ImmutableMap.of(
                                        "result",
                                        ImmutableList.of(ImmutableMap.of("root",
                                                ImmutableMap.of("test1", ImmutableList.of("1", "2", "3"))))));
                    }
                })
                .build();

        var result = test.runTest(ctx -> {
            var res = new MyCustomResource("r1", null, CustomResourceOptions.builder().build());
            assertThat(marker.resolved).isFalse();
            var deps = new ArrayList<Resource>();
            deps.add(res);

            var opts = new InvokeOutputOptions(null, null, null, deps);
            var out = CustomInvokes.doStuff(CustomArgs.Empty, opts).applyValue(r -> {
                assertThat(r).hasSize(1);
                assertThat(r).contains(ImmutableMap.of("root", ImmutableMap.of("test1", ImmutableList.of("1", "2", "3"))));
                return (Void) null;
            });
            
            // Check that the resource was resolved when we called the invoke
            assertThat(marker.resolved).isTrue();
    
            Internal.of(out).getDataAsync().join();
        });

        assertThat(result.exceptions()).hasSize(0);
        assertThat(result.exitCode()).isEqualTo(Runner.ProcessExitedSuccessfully);
    }

    public static final class MyArgs extends ResourceArgs {
        // Empty
    }
    
    public static final class ResolveMarker {
        public boolean resolved;
        
        public ResolveMarker() {
            this.resolved = false;
        }
    }

    @ResourceType(type = "test:DeploymentResourceDependencyGatheringTests:resource")
    private static class MyCustomResource extends CustomResource {
        public MyCustomResource(String name, @Nullable MyArgs args, @Nullable CustomResourceOptions options) {
            super("test:DeploymentResourceDependencyGatheringTests:resource", name, args == null ? new MyArgs() : args,
                    options);
        }
    }

    static class CustomInvokes {
        static Output<ImmutableList<ImmutableMap<String, Object>>> doStuff(
                @SuppressWarnings("SameParameterValue") CustomArgs args,
                @Nullable InvokeOutputOptions options) {
            return Deployment.getInstance().invoke(
                    "tests:custom:stuff",
                    TypeShape.of(CustomResult.class),
                    args,
                    options).applyValue(r -> {
                        return r.result;
                    });
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
        private @Nullable ImmutableList<ImmutableMap<String, Object>> result;

        @CustomType.Builder
        static final class Builder {
            private final CustomResult $;

            Builder() {
                this.$ = new CustomResult();
            }

            Builder(CustomResult defaults) {
                this.$ = defaults;
            }

            @Setter("result")
            Builder result(@Nullable ImmutableList<ImmutableMap<String, Object>> result) {
                this.$.result = result;
                return this;
            }

            CustomResult build() {
                return this.$;
            }
        }
    }
}
