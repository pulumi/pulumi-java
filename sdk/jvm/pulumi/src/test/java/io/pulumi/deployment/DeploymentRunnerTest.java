package io.pulumi.deployment;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.internal.TypedInputOutput;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.exceptions.RunException;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.deployment.internal.DeploymentTests.printErrorCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

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

        printErrorCount(mock.logger);
    }

    public static class TerminatesEarlyOnExceptionStack extends Stack {
        @OutputExport(type = Integer.class)
        public final Output<Integer> slowOutput;

        public TerminatesEarlyOnExceptionStack() {
            var delayed = CompletableFuture.delayedExecutor(1000L, TimeUnit.MILLISECONDS);
            var task = CompletableFuture
                    .supplyAsync(() -> {
                        throw new RunException("Deliberate test error");
                    }, delayed)
                    .thenApply(ignore -> 1);
            this.slowOutput = Output.of(task);
        }
    }

    @Test
    void testLogsTaskDescriptions() {
        var logger = new InMemoryLogger();
        logger.setLevel(Level.FINEST);

        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setStandardLogger(logger)
                .setSpyGlobalInstance();

        var runner = mock.getRunner();
        for (var i = 0; i < 2; i++) {
            var delayed = CompletableFuture.delayedExecutor(100L + i, TimeUnit.MILLISECONDS);
            runner.registerTask(String.format("task%d", i), CompletableFuture.supplyAsync(() -> null, delayed));
        }
        Supplier<CompletableFuture<Map<String, Optional<Object>>>> supplier =
                () -> CompletableFuture.completedFuture(Map.of());
        var code = mock.runner.runAsyncFuture(supplier, null).join();
        assertThat(code).isEqualTo(0);

        var messages = logger.getMessages();
        for (var i = 0; i < 2; i++) {
            assertThat(messages).haveAtLeastOne(containsStringCondition(String.format("Registering task: 'task%d'", i)));
            assertThat(messages).haveAtLeastOne(containsStringCondition(String.format("Completed task: 'task%d'", i)));
        }

        printErrorCount(mock.logger);
        System.out.println(messages);
    }

    private static HamcrestCondition<String> containsStringCondition(String s) {
        return new HamcrestCondition<>(containsString(s));
    }

    private static class InMemoryLogger extends Logger {
        private final Queue<String> messages = new ConcurrentLinkedQueue<>();

        public InMemoryLogger() {
            this(DeploymentRunnerTest.class.getTypeName(), null);
        }

        protected InMemoryLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
            this.setLevel(Level.FINEST);
        }

        public List<String> getMessages() {
            return List.copyOf(messages);
        }

        @Override
        public void log(LogRecord record) {
            this.messages.add(String.format("%s %s %s", record.getLevel(), record.getSequenceNumber(), record.getMessage()));
            super.log(record);
        }
    }
}
