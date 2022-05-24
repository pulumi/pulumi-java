package com.pulumi.resources;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.deployment.MocksTest;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentTests.DeploymentMock.TestResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.function.Consumer;

import static com.pulumi.core.OutputTests.waitForValue;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
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

    private TestResult run(Consumer<Context> factory) {
        var mock = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .deploymentFactory(state -> Mockito.spy(new DeploymentImpl(state)))
                .build();

        var result = mock.runTestAsync(factory).join();
        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);

        verify(mock.deployment, times(1))
                .registerResourceOutputs(any(Resource.class), outputsCaptor.capture());

        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        assertThat(result.stackOutputs).containsExactlyEntriesOf(values);
        return result;
    }

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }
}
