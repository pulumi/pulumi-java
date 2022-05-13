package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.Runner.ProcessExitedAfterLoggingUserActionableMessage;
import static com.pulumi.deployment.internal.Runner.ProcessExitedSuccessfully;
import static com.pulumi.test.internal.PulumiTestInternal.defaultLogger;
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
        var runTaskOne = mock.runner().registerAndRunAsync(() -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
            return null;
        });

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
