package com.pulumi.deployment;

import com.pulumi.Config;
import com.pulumi.core.Output;
import com.pulumi.deployment.internal.DeploymentInternal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.DeploymentTests.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
            .setMocks(new MocksTest.MyMocks())
            .setSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testConfigRequire() {
        mock.overrideConfig("hello-jvm:name", "test");

        Supplier<CompletableFuture<Map<String, Output<?>>>> supplier = () -> {
            var config = Config.of("hello-jvm");
            //noinspection unused
            var ignore = config.require("name");
            return CompletableFuture.completedFuture(Map.<String, Output<?>>of());
        };

        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(0);
    }

    @Test
    void testConfigRequireMissing() {
        Supplier<CompletableFuture<Map<String, Output<?>>>> supplier = () -> {
            var config = Config.of("hello-jvm");
            //noinspection unused
            var ignore = config.require("missing");
            return CompletableFuture.completedFuture(Map.<String, Output<?>>of());
        };
        mock.standardLogger.setLevel(Level.OFF);
        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(32);
    }

    // FIXME
    @Disabled
    @Test
    void testDeploymentInstancesAreSeparate() {
        // this test is more of a sanity check that two separate
        // executions have their own deployment instance
        @Nullable
        AtomicReference<DeploymentInstance> instanceOne = new AtomicReference<>();
        @Nullable
        AtomicReference<DeploymentInstance> instanceTwo = new AtomicReference<>();

        var cf = new CompletableFuture<Integer>();
        var runTaskOne = DeploymentInternal.createRunnerAndRunAsync(
                () -> mock.deployment,
                runner -> {
                    instanceOne.set(Deployment.getInstance());
                    return cf;
                }
        );

        var runTaskTwo = DeploymentInternal.createRunnerAndRunAsync(
                () -> mock.deployment,
                runner -> {
                    instanceTwo.set(Deployment.getInstance());
                    return CompletableFuture.completedFuture(1);
                }
        );

        runTaskTwo.join();
        cf.complete(1);
        runTaskOne.join();

        assertThat(instanceOne).isNotNull();
        assertThat(instanceTwo).isNotNull();
        assertThat(instanceOne).isNotSameAs(instanceTwo);
    }

    // FIXME
    @Disabled
    @Test
    void testDeploymentInstanceIsProtectedFromParallelSynchronousRunAsync() {
        // this test is ensuring that CreateRunnerAndRunAsync method is async
        var cf = new CompletableFuture<Integer>();

        var runTaskOne = DeploymentInternal.createRunnerAndRunAsync(
                () -> mock.deployment, runner -> cf
        );

        // this will throw if we didn't protect
        // the AsyncLocal scope of Deployment#instance
        // by keeping internalCreateRunnerAndRunAsync async
        var runTaskTwo = DeploymentInternal.createRunnerAndRunAsync(
                () -> mock.deployment, runner -> CompletableFuture.completedFuture(1)
        );
        runTaskTwo.join();

        cf.complete(1);
        runTaskOne.join();
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = mock.runner.runAsyncFuture(() -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
            return CompletableFuture.completedFuture(Map.of()); // empty outputs
        }, null);

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
