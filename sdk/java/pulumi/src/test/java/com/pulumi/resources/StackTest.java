package com.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockMonitor;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.DeploymentTests.DeploymentMock.TestResult;
import com.pulumi.deployment.internal.InMemoryLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.pulumi.core.OutputTests.waitForValue;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StackTest {

    @Test
    void testValidStackInstantiationSucceeds() {
        var result = run(ctx -> {
            ctx.export("foo", Output.of("bar"));
        });
        var foo = waitForValue(result.stackOutput("foo", String.class));
        assertThat(foo).isEqualTo("bar");
    }

    @Test
    void testStackWithNullOutputsThrows() {
        assertThatThrownBy(() -> run(ctx -> {
            Output<String> foo = null;
            ctx.export("foo", foo);
        })).hasMessageContaining("The 'output' of an 'export' cannot be 'null'");
    }

    @Test
    void testImmediateStackReference() {
        var result = run(ctx -> {
            var ref = new StackReference(ctx.stackName());
            ctx.export("ref", Output.of(ref));
        });

        var ref = waitForValue(result.stackOutput("ref", StackReference.class));
        assertThat(waitForValue(ref.getOutput("ref"))).isNotNull();
    }

    private TestResult run(Consumer<Context> factory) {
        var log = InMemoryLogger.getLogger("StackTest#run");
        var monitorMocks = new RecurrentStackMocks();
        var mock = DeploymentMockBuilder.builder()
                .setMocks(monitorMocks)
                .setStandardLogger(log)
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .build();
        monitorMocks.setMock(mock);

        var result = mock.runTestAsync(factory).join();
        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);

        verify(mock.deployment, times(1))
                .registerResourceOutputs(any(Resource.class), outputsCaptor.capture());

        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        assertThat(result.stackOutputs).containsExactlyEntriesOf(values);
        return result;
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
