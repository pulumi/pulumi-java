package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.deployment.internal.Runner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMock;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    private static DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentMockBuilder.builder()
            .setMocks(new MocksTest.MyMocks())
            .build();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testConfigRequire() {
        mock.overrideConfig("hello-java:name", "test");

        var result = mock.runTestAsync(ctx -> {
            var config = ctx.config("hello-java");
            //noinspection unused
            var ignore = config.require("name");
        }).join();
        assertThat(result.exitCode).isEqualTo(Runner.ProcessExitedSuccessfully);
    }

    @Test
    void testConfigRequireMissing() {
        mock.standardLogger.setLevel(Level.OFF);
        var result = mock.runTestAsync(ctx -> {
            var config = ctx.config("hello-java");
            //noinspection unused
            var ignore = config.require("missing");
        }).join();
        assertThat(result.exitCode).isEqualTo(Runner.ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = mock.runTestAsync(ctx -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
        });

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
