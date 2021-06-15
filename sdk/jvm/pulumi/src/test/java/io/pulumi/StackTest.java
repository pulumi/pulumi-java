package io.pulumi;

import io.pulumi.core.InputOutputTests;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.Resource;
import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class StackTest {

    private static class ValidStack extends Stack {

        @OutputExport(name = "foo", type = String.class)
        private final Output<String> explicitName;

        @OutputExport(type = String.class)
        private final Output<String> implicitName;

        public ValidStack() {
            this.explicitName = Output.of("bar");
            this.implicitName = Output.of("buzz");
        }

        public Output<String> getExplicitName() {
            return explicitName;
        }

        public Output<String> getImplicitName() {
            return implicitName;
        }
    }

    @Test
    void testValidStackInstantiationSucceeds() {
        var result = run(ValidStack::new);
        assertThat(result.t2).hasSize(3);
        assertThat(result.t2).containsKey("foo");
        assertThat(result.t2.get("foo")).isPresent();
        assertThat(
                InputOutputTests.waitFor(result.t1.explicitName)
        ).isSameAs(
                InputOutputTests.waitFor((Output) result.t2.get("foo").get())
        );

        assertThat(result.t2).containsKey("implicitName");
        assertThat(result.t2.get("implicitName")).isPresent();
        assertThat(
                InputOutputTests.waitFor(result.t1.implicitName)
        ).isSameAs(
                InputOutputTests.waitFor(((Output) result.t2.get("implicitName").get()))
        );
    }

    private static class NullOutputStack extends Stack {
        @SuppressWarnings("unused")
        @OutputExport(name = "foo", type = String.class)
        public Output<String> foo = null;
    }

    @Test
    void testStackWithNullOutputsThrows() {
        assertThatThrownBy(() -> run(NullOutputStack::new))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output(s) 'foo' have no value assigned");
    }

    private static class InvalidOutputTypeStack extends Stack {
        @OutputExport(name = "foo", type = String.class)
        public String foo;

        public InvalidOutputTypeStack() {
            this.foo = "bar";
        }
    }

    @Test
    void testStackWithInvalidOutputTypeThrows() {
        assertThatThrownBy(() -> run(InvalidOutputTypeStack::new))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output(s) 'foo' have incorrect type");
    }

    private <T extends Stack> Tuple2<T, Map<String, Optional<Object>>> run(Supplier<T> factory) {
        var mock = DeploymentMockBuilder.builder()
                .setOptions(new TestOptions("TestProject", "TestStack"))
                .setSpyGlobalInstance();

        var stack = factory.get();
        stack.internalRegisterPropertyOutputs();

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Optional<Object>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);

        // TODO: is this OK that we're called twice?
        verify(mock.deployment, atLeastOnce()).registerResourceOutputs(any(Resource.class), outputsCaptor.capture());

        var values = InputOutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        return Tuples.of(stack, values);
    }

    @AfterEach
    void cleanup() {
        cleanupDeploymentMocks();
    }
}