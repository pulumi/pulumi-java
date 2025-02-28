package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.exceptions.RunException;
import com.pulumi.resources.internal.Stack;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.Runner.ProcessExitedAfterLoggingUserActionableMessage;
import static com.pulumi.deployment.internal.Runner.ProcessExitedSuccessfully;
import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentRunnerTest {

    @AfterEach
    public void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testVeryEarlyExceptionShortCircuitsBeforeMainLoopStarts() {
        var test = PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();

        test.runner().registerTask("exceptionThatShouldNotBe", CompletableFuture.completedFuture(
                new RuntimeException("test exception before the loop")
        ));
        var result = test.runTest(ctx -> {
            throw new RuntimeException("very early deliberate exception");
        });

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(RuntimeException.class);
        assertThat(result.exceptions().get(1)).hasMessage("very early deliberate exception");

        assertThat(result.resources())
                .isNotNull()
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);
        assertThat(Internal.of(result.output("slowOutput")).getDataAsync()).isNotCompleted();
        assertThat(Internal.of(result.output("slowOutput")).getValueNullable()).isNotCompleted();
        assertThat(result.exitCode()).isEqualTo(ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testTerminatesEarlyOnFirstException() {
        var test = PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();

        var exceptionThatShouldNotBe = new CompletableFuture<Void>();
        exceptionThatShouldNotBe.completeExceptionally(
                new RuntimeException("test exception before the loop")
        );
        test.runner().registerTask("exceptionThatShouldNotBe", exceptionThatShouldNotBe);
        var userCodeWasCalled = new AtomicBoolean(false);
        var result = test.runTest(ctx -> userCodeWasCalled.set(true));
        assertThat(userCodeWasCalled).isTrue();

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(RuntimeException.class);
        assertThat(result.exceptions().get(1)).hasMessage("test exception before the loop");

        assertThat(result.resources())
                .isNotNull()
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);
        assertThat(Internal.of(result.output("slowOutput")).getDataAsync()).isNotCompleted();
        assertThat(Internal.of(result.output("slowOutput")).getValueNullable()).isNotCompleted();
        assertThat(result.exitCode()).isEqualTo(ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testTerminatesEarlyOnExceptionInOutput() {
        var test = PulumiTestInternal.builder()
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(ctx -> {
            Output.of(CompletableFuture.failedFuture(new RunException("Deliberate test error")));
            ctx.export("slowOutput", Output.of(
                    new CompletableFuture<Integer>()
                            .completeOnTimeout(1, 60, TimeUnit.SECONDS)
            ));
        });

        assertThat(result.errors()).isNotNull();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors()).haveAtLeastOne(containsString("Deliberate test error"));

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(RunException.class);
        assertThat(result.exceptions().get(1)).hasMessage("Deliberate test error");

        assertThat(result.resources())
                .isNotNull()
                .hasSize(1)
                .hasExactlyElementsOfTypes(Stack.class);
        assertThat(Internal.of(result.output("slowOutput")).getDataAsync()).isNotCompleted();
        assertThat(Internal.of(result.output("slowOutput")).getValueNullable()).isNotCompleted();
        assertThat(result.exitCode()).isEqualTo(ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testLogsTaskDescriptions() {
        // The test requires Level.FINEST
        var logger = InMemoryLogger.getLogger(Level.FINEST, "DeploymentRunnerTest#testLogsTaskDescriptions");
        var mock = PulumiTestInternal.builder()
                .standardLogger(logger)
                .build();

        for (var i = 0; i < 2; i++) {
            final var delay = 100L + i;
            mock.runner().registerTask(String.format("task%d", i), new CompletableFuture<Void>().completeOnTimeout(null, delay, TimeUnit.MILLISECONDS));
        }
        var result = mock.runner().runAsync(() -> null).join();
        assertThat(result.exitCode()).isEqualTo(0);

        var messages = logger.getMessages();
        for (var i = 0; i < 2; i++) {
            assertThat(messages).haveAtLeastOne(containsString(String.format("Registering task: 'task%d'", i)));
            assertThat(messages).haveAtLeastOne(containsString(String.format("Completed task: 'task%d'", i)));
        }
    }

    @Test
    void testRunnerRuns() {
        var test = PulumiTestInternal.builder().build();

        var taskWasCalled = new AtomicBoolean(false);
        test.runner().registerTask("testRunnerRuns", ContextAwareCompletableFuture.runAsync(() -> taskWasCalled.set(true)));
        var result = test.runner().runAsync(() -> "foo").join();

        assertThat(taskWasCalled).isTrue();
        assertThat(result.exitCode()).isEqualTo(ProcessExitedSuccessfully);
        assertThat(result.exceptions()).isEmpty();
        assertThat(result.result()).hasValue("foo");
    }
}
