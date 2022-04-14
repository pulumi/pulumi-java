package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Output;
import io.pulumi.internal.PulumiMock;
import io.pulumi.internal.TestRuntimeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;

import static io.pulumi.internal.PulumiMock.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentTest {

    @AfterEach
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testConfigRequire() {
        var mock = PulumiMock.builder()
                .setMocks(new MocksTest.MyMocks())
                .setRuntimeContext(TestRuntimeContext.builder()
                        .setConfig(ImmutableMap.of("hello-jvm:name", "test"))
                        .build())
                .buildSpyGlobalInstance();

        Supplier<CompletableFuture<Map<String, Output<?>>>> supplier = () -> {
            var config = mock.stackContext.config("hello-jvm");
            //noinspection unused
            var ignore = config.require("name");
            return CompletableFuture.completedFuture(Map.<String, Output<?>>of());
        };

        var code = mock.runAsync(ctx -> ctx.exports()).join();
        assertThat(code).isEqualTo(0);
    }

    @Test
    void testConfigRequireMissing() {
        var mock = PulumiMock.builder()
                .setMocks(new MocksTest.MyMocks())
                .buildSpyGlobalInstance();

        Supplier<CompletableFuture<Map<String, Output<?>>>> supplier = () -> {
            var config = mock.stackContext.config("hello-jvm");
            //noinspection unused
            var ignore = config.require("missing");
            return CompletableFuture.completedFuture(Map.<String, Output<?>>of());
        };
        mock.standardLogger.setLevel(Level.OFF);
        var code = mock.runAsync(ctx -> ctx.exports()).join();
        assertThat(code).isEqualTo(32);
    }

    @Test
    void testRunWaitsForOrphanedOutput() {
        var mock = PulumiMock.builder()
                .setMocks(new MocksTest.MyMocks())
                .buildSpyGlobalInstance();

        final var result = new AtomicInteger(0);
        var cf = new CompletableFuture<Integer>();
        var runTaskOne = mock.runAsync(ctx -> {
            //noinspection unused
            Output<Integer> orphaned = Output.of(cf).applyValue(result::getAndSet); // the orphaned output
            return ctx.exports(); // empty outputs
        });

        var triggered = cf.complete(42);
        assertThat(triggered).isTrue();
        runTaskOne.join();

        assertThat(result.get()).isEqualTo(42);
    }
}
