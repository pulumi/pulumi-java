package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.exceptions.RunException;
import com.pulumi.resources.Stack;
import com.pulumi.test.internal.InMemoryLogger;
import com.pulumi.test.mock.MonitorMocksTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentRunnerTest {

    @AfterEach
    public void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testTerminatesEarlyOnException() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
            .setMocks(new MonitorMocksTest.MyMocks())
            .setSpyGlobalInstance();

        mock.standardLogger.setLevel(Level.OFF);
        var result = mock.tryTestAsync(TerminatesEarlyOnExceptionStack::new).join();

        assertThat(mock.runner.getSwallowedExceptions()).hasSize(2);

        assertThat(result.exceptions).isNotNull();
        assertThat(result.exceptions).isNotEmpty();
        assertThat(result.exceptions).hasSize(1);
        assertThat(result.exceptions.get(0)).isExactlyInstanceOf(RunException.class);
        assertThat(result.exceptions.get(0)).hasMessageContaining("Deliberate test error");
        assertThat(result.resources).isNotNull();
        assertThat(result.resources).isNotEmpty();
        assertThat(result.resources).hasSize(1);
        var stack = (TerminatesEarlyOnExceptionStack) result.resources.get(0);
        assertThat(Internal.of(stack.slowOutput).getDataAsync()).isNotCompleted();
        assertThat(Internal.of(stack.slowOutput).getValueNullable()).isNotCompleted();
    }

    public static class TerminatesEarlyOnExceptionStack extends Stack {
        @Export(type = Integer.class)
        public final Output<Integer> slowOutput;

        public TerminatesEarlyOnExceptionStack() {
            Output.of(CompletableFuture.failedFuture(new RunException("Deliberate test error")));
            this.slowOutput = Output.of(new CompletableFuture<Integer>()
                    .completeOnTimeout(1, 60, TimeUnit.SECONDS));
        }
    }

    @Test
    void testLogsTaskDescriptions() {
        // The test requires Level.FINEST
        var logger = InMemoryLogger.getLogger(Level.FINEST, "DeploymentRunnerTest#testLogsTaskDescriptions");

        var mock = DeploymentTests.DeploymentMockBuilder.builder()
            .setMocks(new MonitorMocksTest.MyMocks())
            .setStandardLogger(logger)
            .setSpyGlobalInstance();

        for (var i = 0; i < 2; i++) {
            final var delay = 100L + i;
            mock.runner.registerTask(String.format("task%d", i), new CompletableFuture<Void>().completeOnTimeout(null, delay, TimeUnit.MILLISECONDS));
        }
        Supplier<CompletableFuture<Map<String, Output<?>>>> supplier =
                () -> CompletableFuture.completedFuture(Map.of());
        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(0);

        var messages = logger.getMessages();
        for (var i = 0; i < 2; i++) {
            assertThat(messages).haveAtLeastOne(containsString(String.format("Registering task: 'task%d'", i)));
            assertThat(messages).haveAtLeastOne(containsString(String.format("Completed task: 'task%d'", i)));
        }
    }
}
