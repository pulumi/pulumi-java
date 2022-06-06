package com.pulumi.resources.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockMonitor;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.resources.Resource;
import com.pulumi.resources.StackReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import static com.pulumi.core.OutputTests.waitForValue;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StackTest {

    @Test
    void testValidStackInstantiationSucceeds() {
        var mock = DeploymentMockBuilder.builder()
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .build();
        mock.standardLogger.setLevel(Level.OFF);

        var result = mock.runTestAsync(ctx -> {
            ctx.export("foo", Output.of("bar"));
        }).join();

        var foo = waitForValue(result.output("foo", String.class));
        assertThat(foo).isEqualTo("bar");

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mock.deployment, times(1))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);
        ArgumentCaptor<Resource> resourceOutputCaptor = ArgumentCaptor.forClass(Resource.class);

        verify(mock.deployment, times(1))
                .registerResourceOutputs(resourceOutputCaptor.capture(), outputsCaptor.capture());

        assertThat(resourceOutputCaptor.getValue()).isInstanceOf(Stack.class);
        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        assertThat(result.outputs()).containsExactlyEntriesOf(values);
    }

    @Test
    void testStackWithNullOutputsThrows() {
        var mock = DeploymentMockBuilder.builder()
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .build();
        mock.standardLogger.setLevel(Level.OFF);

        var resultFuture = mock.runTestAsync(
                ctx -> ctx.export("foo", null)
        );

        var result = resultFuture.join();

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
        verify(mock.deployment, times(1))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);

        //noinspection unchecked
        verify(mock.deployment, times(1))
                .registerResourceOutputs(any(Resource.class), any(Output.class));

        assertThat(result.outputs()).isEmpty();
    }

    @Test
    void testImmediateStackReference() {
        var monitorMocks = new RecurrentStackMocks();
        var mock = DeploymentMockBuilder.builder()
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .setMocks(monitorMocks)
                .build();
        monitorMocks.setMock(mock);

        var result = mock.runTestAsync(ctx -> {
            assertThat(((MockMonitor) mock.monitor).resources)
                    .hasSize(1)
                    .hasExactlyElementsOfTypes(Stack.class);
            var ref = new StackReference(ctx.stackName());
            ctx.export("ref", Output.of(ref));
        }).join();

        var ref = waitForValue(result.output("ref", StackReference.class));
        assertThat(waitForValue(ref.getOutput("ref"))).isNotNull();

        assertThat(result.exceptions()).isEmpty();
        assertThat(result.errors()).isEmpty();

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(mock.deployment, times(2))
                .readOrRegisterResource(resourceCaptor.capture(), anyBoolean(), any(), any(), any(), any());
        assertThat(resourceCaptor.getAllValues())
                .hasSize(2)
                .hasExactlyElementsOfTypes(Stack.class, StackReference.class);

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);
        ArgumentCaptor<Resource> resourceOutputCaptor = ArgumentCaptor.forClass(Resource.class);

        verify(mock.deployment, times(1))
                .registerResourceOutputs(resourceOutputCaptor.capture(), outputsCaptor.capture());

        assertThat(resourceOutputCaptor.getValue()).isInstanceOf(Stack.class);
        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        assertThat(result.outputs()).containsExactlyEntriesOf(values);
    }

    private static final class RecurrentStackMocks implements Mocks {
        private DeploymentTests.DeploymentMock mock;

        private Optional<Stack> queryStack() {
            if (!(mock.monitor instanceof MockMonitor)) {
                throw new IllegalStateException("Expected monitor to be an instanceof MockMonitor");
            }
            var mockMonitor = (MockMonitor) mock.monitor;
            return mockMonitor.resources.stream()
                    .filter(r -> r instanceof Stack)
                    .map(r -> (Stack) r)
                    .findFirst();
        }

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            requireNonNull(this.mock, "forgot to call setMock?");
            requireNonNull(args.type);
            switch (args.type) {
                case "pulumi:pulumi:StackReference":
                    return CompletableFuture.completedFuture(
                            Tuples.of(
                                    Optional.of(mock.options.getStackName()),
                                    ImmutableMap.of("outputs",
                                            ImmutableMap.of("ref", queryStack().orElseThrow())
                                    )
                            )
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }

        public void setMock(DeploymentTests.DeploymentMock mock) {
            this.mock = mock;
        }
    }

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }
}
