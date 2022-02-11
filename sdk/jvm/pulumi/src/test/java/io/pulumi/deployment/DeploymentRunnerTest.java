package io.pulumi.deployment;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.InMemoryLogger;
import io.pulumi.exceptions.RunException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentRunnerTest {

    @AfterEach
    public void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testTerminatesEarlyOnException() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setSpyGlobalInstance();

        var result = mock.tryTestAsync(TerminatesEarlyOnExceptionStack.class).join();

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
        assertThat(TypedInputOutput.cast(stack.slowOutput).internalGetDataAsync()).isNotCompleted();
        assertThat(TypedInputOutput.cast(stack.slowOutput).view(InputOutputData::getValueNullable)).isNotCompleted();
    }

    public static class TerminatesEarlyOnExceptionStack extends Stack {
        @OutputExport(type = Integer.class)
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
                .setStandardLogger(logger)
                .setSpyGlobalInstance();

        for (var i = 0; i < 2; i++) {
            final var delay = 100L + i;
            mock.runner.registerTask(String.format("task%d", i), new CompletableFuture<Void>().completeOnTimeout(null, delay, TimeUnit.MILLISECONDS));
        }
        Supplier<CompletableFuture<Map<String, Optional<Object>>>> supplier =
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
