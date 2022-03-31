package io.pulumi;

import io.pulumi.core.Output;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.annotations.Export;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.MocksTest;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class StackTest {

    private static class ValidStack extends Stack {

        @Export(name = "foo", type = String.class)
        private final Output<String> explicitName;

        @Export(type = String.class)
        private final Output<String> implicitName;

        public ValidStack(Deployment deployment) {
            super(deployment);
            var output = OutputBuilder.forDeployment(deployment);
            this.explicitName = output.of("bar");
            this.implicitName = output.of("buzz");
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
        var ctx = OutputTests.testContext();
        var result = run(() -> new ValidStack(ctx.deployment));
        assertThat(result.t2).hasSize(3);
        assertThat(result.t2).containsKey("foo");
        assertThat(result.t2.get("foo")).isPresent();
        assertThat(
                OutputTests.waitFor(result.t1.explicitName)
        ).isSameAs(
                OutputTests.waitFor((Output) result.t2.get("foo").get())
        );

        assertThat(result.t2).containsKey("implicitName");
        assertThat(result.t2.get("implicitName")).isPresent();
        assertThat(
                OutputTests.waitFor(result.t1.implicitName)
        ).isSameAs(
                OutputTests.waitFor(((Output) result.t2.get("implicitName").get()))
        );
    }

    private static class NullOutputStack extends Stack {
        public NullOutputStack(Deployment deployment) {
            super(deployment);
        }

        @SuppressWarnings("unused")
        @Export(name = "foo", type = String.class)
        public Output<String> foo = null;

    }

    @Test
    void testStackWithNullOutputsThrows() {
        var ctx = OutputTests.testContext();
        assertThatThrownBy(() -> run(() -> new NullOutputStack(ctx.deployment)))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output(s) 'foo' have no value assigned");
    }

    private static class InvalidOutputTypeStack extends Stack {
        @Export(name = "foo", type = String.class)
        public String foo;

        public InvalidOutputTypeStack(Deployment deployment) {
            super(deployment);
            this.foo = "bar";
        }
    }

    @Test
    void testStackWithInvalidOutputTypeThrows() {
        var ctx = OutputTests.testContext();
        assertThatThrownBy(() -> run(() -> new InvalidOutputTypeStack(ctx.deployment)))
                .isInstanceOf(RunException.class)
                .hasMessageContaining("Output(s) 'foo' have incorrect type");
    }

    private <T extends Stack> Tuple2<T, Map<String, Optional<Object>>> run(Supplier<T> factory) {
        var mock = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .setOptions(new TestOptions("TestProject", "TestStack"))
                .buildSpyInstance();

        var stack = factory.get();
        Internal.from(stack).registerPropertyOutputs();

        //noinspection unchecked
        ArgumentCaptor<Output<Map<String, Optional<Object>>>> outputsCaptor = ArgumentCaptor.forClass(Output.class);


        // TODO: is this OK that we're called twice?

        // TODO Uncomment
        // verify(mock.deployment, atLeastOnce()).registerResourceOutputs(any(Resource.class), outputsCaptor.capture());

        var values = OutputTests.waitFor(outputsCaptor.getValue()).getValueNullable();
        return Tuples.of(stack, values);
    }

}
