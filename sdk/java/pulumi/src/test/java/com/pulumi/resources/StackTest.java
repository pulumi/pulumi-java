package com.pulumi.resources;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.pulumi.core.OutputTests.waitForValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class StackTest {

    @Test
    void testValidStackInstantiationSucceeds() {
        assertThatCode(() -> run(
                ctx -> {
                    ctx.export("foo", Output.of("bar"));
                    ctx.export("faz", Output.of("baz"));
                }
        )).doesNotThrowAnyException();
    }

    private void run(Consumer<Context> callback) {
        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);

        var deploymentSpyReference = new AtomicReference<DeploymentImpl>();
        var mock = PulumiTestInternal.withDefaults()
                .deploymentFactory(state -> {
                    var spy = Mockito.spy(new DeploymentImpl(state));
                    deploymentSpyReference.set(spy);
                    return spy;
                })
                .build();

        var result = mock.runTestAsync(callback).join();

        var resources = result.resources();
        var stack = result.stack();
        var exports = waitForValue(stack.outputs());

        // TODO: is this OK that we're called twice?
        verify(deploymentSpyReference.get(), atLeastOnce()).registerResourceOutputs(any(Resource.class), outputsCaptor.capture());
        var values = waitForValue(outputsCaptor.getValue());

        assertThat(exports).containsExactlyEntriesOf(values);
    }

    @AfterEach
    void cleanup() {
        PulumiTest.cleanup();
    }
}
