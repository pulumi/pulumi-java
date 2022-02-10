package io.pulumi.deployment;


import io.pulumi.Stack;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutputTests;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InputImport;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.core.internal.annotations.ResourceType;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.resources.ResourceArgs;
import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.deployment.internal.DeploymentTests.printErrorCount;
import static org.assertj.core.api.Assertions.assertThat;

public class MocksTest {
    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMonitor(new MockMonitor(new MyMocks()))
                .setSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @AfterEach
    public void printInternalErrorCount() {
        printErrorCount(mock.logger);
    }

    @Test
    void testCustomMocks() {
        var resources = mock.testAsync(MyStack.class).join();

        var instance = resources.stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isPresent();

        var ip = InputOutputTests.waitFor(instance.get().publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");

    }

    @Test
    void testCustomWithResourceReference() {
        var resources = mock.testAsync(MyStack.class).join();

        var myCustom = resources.stream()
                .filter(r -> r instanceof MyCustom)
                .map(r -> (MyCustom) r)
                .findFirst();
        assertThat(myCustom).isPresent();

        var instance = InputOutputTests.waitFor(myCustom.get().instance).getValueNullable();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(Instance.class);

        var ip = InputOutputTests.waitFor(instance.publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    @Test
    void testStack() {
        var resources = mock.testAsync(MyStack.class).join();

        var stack = resources.stream()
                .filter(r -> r instanceof MyStack)
                .map(r -> (MyStack) r)
                .findFirst();
        assertThat(stack).isPresent();

        var ip = InputOutputTests.waitFor(stack.get().publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    @ResourceType(type = "aws:ec2/instance:Instance")
    public static class Instance extends CustomResource {
        @OutputExport(type = String.class)
        public Output<String> publicIp;

        public Instance(String name, InstanceArgs args, @Nullable CustomResourceOptions options) {
            super("aws:ec2/instance:Instance", name, args, options);
        }
    }

    public static final class InstanceArgs extends ResourceArgs {}

    public static class MyCustom extends CustomResource {
        @OutputExport(type = Instance.class)
        public Output<Instance> instance;

        public MyCustom(String name, MyCustomArgs args, @Nullable CustomResourceOptions options) {
            super("pkg:index:MyCustom", name, args, options);
        }
    }

    public static final class MyCustomArgs extends ResourceArgs {
        @InputImport
        @Nullable
        public final Input<Instance> instance;

        public MyCustomArgs(@Nullable Instance instance) {
            this.instance = Input.of(instance);
        }
    }

    public static class MyStack extends Stack {
        @OutputExport(type = String.class)
        public final Output<String> publicIp;

        public MyStack() {
            var myInstance = new Instance("instance", new InstanceArgs(), null);
            var res = new MyCustom("mycustom", new MyCustomArgs(myInstance), null);
            this.publicIp = myInstance.publicIp;
        }
    }
}
