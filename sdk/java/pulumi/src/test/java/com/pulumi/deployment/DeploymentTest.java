package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testConfigRequire() {
        var test = PulumiTestInternal.builder()
                .config(Map.of("hello-java:name", "test"))
                .build();

        var result = test.runTest(ctx -> {
            var config = ctx.config("hello-java");
            //noinspection unused
            var ignore = config.require("name");
        });
        assertThat(result.exitCode()).isEqualTo(Runner.ProcessExitedSuccessfully);
        assertThat(result.exceptions()).isEmpty();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void testConfigRequireMissing() {
        var test = PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(ctx -> {
            var config = ctx.config("hello-java");
            //noinspection unused
            var ignore = config.require("missing");
        });
        assertThat(result.exitCode()).isEqualTo(Runner.ProcessExitedAfterLoggingUserActionableMessage);
        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = PulumiTest.runTestAsync(ctx -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
        });

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
