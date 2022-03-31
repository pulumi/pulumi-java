package io.pulumi.deployment;

import io.pulumi.Config;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;

import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMock;
import static io.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
                .setMocks(new MocksTest.MyMocks())
                .buildSpyInstance();
    }

    @Test
    void testConfigRequire() {
        mock.overrideConfig("hello-jvm:name", "test");

        Supplier<CompletableFuture<Map<String, Optional<Object>>>> supplier = () -> {
            var config = Config.of(mock.getDeployment(), "hello-jvm");
            //noinspection unused
            var ignore = config.require("name");
            return CompletableFuture.completedFuture(Map.<String, Optional<Object>>of());
        };

        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(0);
    }

    @Test
    void testConfigRequireMissing() {
        Supplier<CompletableFuture<Map<String, Optional<Object>>>> supplier = () -> {
            var config = Config.of(mock.getDeployment(), "hello-jvm");
            //noinspection unused
            var ignore = config.require("missing");
            return CompletableFuture.completedFuture(Map.<String, Optional<Object>>of());
        };
        mock.standardLogger.setLevel(Level.OFF);
        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(32);
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = mock.runner.runAsyncFuture(() -> {
            //noinspection unused
            Output<Integer> orphaned = output().of(cf).applyValue(result::getAndSet); // the orphaned output
            return CompletableFuture.completedFuture(Map.of()); // empty outputs
        }, null);

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }

    private OutputBuilder output() {
        return OutputBuilder.forDeployment(mock.getDeployment());
    }
}
