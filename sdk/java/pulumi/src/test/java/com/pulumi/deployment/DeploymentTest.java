package com.pulumi.deployment;

import com.pulumi.Config;
import com.pulumi.core.Output;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.mock.MonitorMocksTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMock;
import static com.pulumi.deployment.internal.DeploymentTests.DeploymentMockBuilder;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static com.pulumi.deployment.internal.DeploymentTests.defaultLogger;
import static com.pulumi.deployment.internal.Runner.ProcessExitedAfterLoggingUserActionableMessage;
import static com.pulumi.deployment.internal.Runner.ProcessExitedSuccessfully;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    @AfterEach
    void cleanup() {
        PulumiTest.cleanup();
    }

    @Test
    void testConfigRequire() {
        var mock = PulumiTestInternal.withDefaults()
                .config(Map.of("hello-java:name", "test"))
                .useRealRunner()
                .build();

        var code = mock.runAsync(ctx -> {
            var config = ctx.config("hello-java");
            config.require("name"); // check the config key existence
        }).join();
        assertThat(code).isEqualTo(ProcessExitedSuccessfully);
    }

    @Test
    void testConfigRequireMissing() {
        var logger = defaultLogger();
        logger.setLevel(Level.OFF);

        var mock = PulumiTestInternal.withDefaults()
                .standardLogger(logger)
                .useRealRunner()
                .build();

        var code = mock.runAsync(ctx -> {
            var config = ctx.config("hello-java");
            config.require("missing"); // trigger error
        }).join();
        assertThat(code).isEqualTo(ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        var mock = PulumiTestInternal.withDefaults()
                .useRealRunner()
                .build();

        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = mock.runner().runAsyncFuture(() -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
            return CompletableFuture.completedFuture(Map.of()); // empty outputs
        });

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
