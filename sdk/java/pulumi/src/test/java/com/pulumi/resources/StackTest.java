package com.pulumi.resources;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.core.Tuples.Tuple2;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.MocksTest;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.exceptions.RunException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StackTest {

    private static class ValidStack {
        public final Output<String> implicitName;
        public final Output<String> explicitName;

        public ValidStack(DeploymentTests.DeploymentMock.TestAsyncResult result) {
            this.implicitName = result.getStackOutput("implicitName");
            this.explicitName = result.getStackOutput("explicitName");
        }

        public static void init(Context ctx) {
            ctx.export("explicitName", Output.of("bar"));
            ctx.export("implicitName", Output.of("buzz"));
        }
    }

    @Test
    void testValidStackInstantiationSucceeds() {
        var result = run(ValidStack::init, ValidStack::new);
        assertThat(result.t2).hasSize(3);
        assertThat(result.t2).containsKey("foo");
        assertThat(
                OutputTests.waitFor(result.t1.explicitName)
        ).isSameAs(
                OutputTests.waitFor(result.t2.get("foo"))
        );

        assertThat(result.t2).containsKey("implicitName");
        assertThat(
                OutputTests.waitFor(result.t1.implicitName)
        ).isSameAs(
                OutputTests.waitFor(result.t2.get("implicitName"))
        );
    }

    private static class NullOutputStack {
        private final Output<String> foo;

        public NullOutputStack(DeploymentTests.DeploymentMock.TestAsyncResult result) {
            this.foo = result.getStackOutput("foo");
        }

        public static void init(Context ctx) {
            Output<String> foo = null;
            ctx.export("foo", foo);
        }
    }

    @Test
    void testStackWithNullOutputsThrows() {
        assertThatThrownBy(() -> run(NullOutputStack::init, NullOutputStack::new))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output(s) 'foo' have no value assigned");
    }

    private static class InvalidOutputTypeStack  {
        public final Output<String> foo;

        public InvalidOutputTypeStack(DeploymentTests.DeploymentMock.TestAsyncResult result) {
            this.foo = result.getStackOutput("foo");
        }

        public static void init(Context ctx) {
            ctx.export("foo", Output.of("bar"));
        }
    }

    @Test
    void testStackWithInvalidOutputTypeThrows() {
        assertThatThrownBy(() -> run(InvalidOutputTypeStack::init, InvalidOutputTypeStack::new))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output field(s) 'foo' have incorrect type");
    }

    private <T> Tuple2<T, Map<String, Output<?>>> run(
            Consumer<Context> factory,
            Function<DeploymentTests.DeploymentMock.TestAsyncResult, T> parseResult) {
        var mock = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .setOptions(new TestOptions("TestProject", "TestStack"))
                .setSpyGlobalInstance();

        var result = mock.tryTestAsync(factory).join();
        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Output<?>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);

        verify(mock.deployment, times(1))
                .registerResourceOutputs(any(Resource.class), outputsCaptor.capture());

        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        return Tuples.of(parseResult.apply(result), values);
    }

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }
}
