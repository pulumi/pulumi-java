package com.pulumi.resources.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.resources.Resource;
import com.pulumi.resources.StackReference;
import com.pulumi.test.Mocks;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.logging.Level;

import static com.pulumi.test.PulumiTest.extractValue;
import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StackTest {

    @Test
    void testValidStackInstantiationSucceeds() {
        var test = PulumiTestInternal.builder()
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(
                ctx -> ctx.export("foo", Output.of("bar"))
        );

        var foo = extractValue(result.output("foo", String.class));
        assertThat(foo).isEqualTo("bar");

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(test.readOrRegisterResource(), times(1))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);
        ArgumentCaptor<Resource> resourceOutputCaptor = ArgumentCaptor.forClass(Resource.class);

        verify(test.registerResourceOutputs(), times(1))
                .registerResourceOutputs(resourceOutputCaptor.capture(), outputsCaptor.capture());

        assertThat(resourceOutputCaptor.getValue()).isInstanceOf(Stack.class);
        var values = extractValue(outputsCaptor.getValue());
        assertThat(result.outputs()).containsExactlyEntriesOf(values);
    }

    @Test
    void testStackWithNullOutputsThrows() {
        var test = PulumiTestInternal.builder()
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(
                ctx -> ctx.export("foo", null)
        );

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(NullPointerException.class);
        assertThat(result.exceptions().get(1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("The 'output' of an 'export' cannot be 'null'");
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0))
                .contains("The 'output' of an 'export' cannot be 'null'");
        assertThat(result.outputs()).isEmpty();

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(test.readOrRegisterResource(), times(1))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);

        //noinspection unchecked
        verify(test.registerResourceOutputs(), times(1))
                .registerResourceOutputs(any(Resource.class), any(Output.class));

        assertThat(result.outputs()).isEmpty();
    }

    @Test
    void testImmediateStackReference() {
        var monitorMocks = new RecurrentStackMocks();
        var test = PulumiTestInternal.builder()
                .mocks(monitorMocks)
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .build();

        monitorMocks.setStack(() -> {
            var stack = test.monitor().resources.stream()
                    .filter(r -> r instanceof Stack)
                    .map(r -> (Stack) r)
                    .findFirst()
                    .orElseThrow();
            return Map.entry(test.options().stackName(), stack);
        });

        var result = test.runTest(ctx -> {
            assertThat(test.monitor().resources)
                    .hasSize(1)
                    .hasExactlyElementsOfTypes(Stack.class);
            var ref = new StackReference(ctx.stackName());
            ctx.export("ref", Output.of(ref));
        });

        var ref = extractValue(result.output("ref", StackReference.class));
        assertThat(extractValue(ref.output("ref"))).isNotNull();

        assertThat(result.exceptions()).isEmpty();
        assertThat(result.errors()).isEmpty();

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(test.readOrRegisterResource(), times(2))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(2)
                .hasExactlyElementsOfTypes(Stack.class, StackReference.class);

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);
        ArgumentCaptor<Resource> resourceOutputCaptor = ArgumentCaptor.forClass(Resource.class);

        verify(test.registerResourceOutputs(), times(1))
                .registerResourceOutputs(resourceOutputCaptor.capture(), outputsCaptor.capture());

        assertThat(resourceOutputCaptor.getValue()).isInstanceOf(Stack.class);
        var values = extractValue(outputsCaptor.getValue());
        assertThat(result.outputs()).containsExactlyEntriesOf(values);
    }

    private static final class RecurrentStackMocks implements Mocks {

        private Supplier<Map.Entry<String, Stack>> stack;

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(this.stack, "forgot to call setStack?");
            requireNonNull(args.type);
            switch (args.type) {
                case "pulumi:pulumi:StackReference":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(
                                    Optional.of(this.stack.get().getKey()),
                                    ImmutableMap.of("outputs",
                                            ImmutableMap.of("ref", this.stack.get().getValue())
                                    )
                            )
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        public void setStack(Supplier<Map.Entry<String, Stack>> stack) {
            this.stack = stack;
        }
    }

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }
}
