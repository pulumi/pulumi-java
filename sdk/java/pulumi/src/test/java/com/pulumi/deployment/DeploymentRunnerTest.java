package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.Internal;
import com.pulumi.exceptions.RunException;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.internal.InMemoryLogger;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.pulumi.deployment.internal.Runner.ProcessExitedAfterLoggingUserActionableMessage;
import static com.pulumi.test.internal.PulumiTestInternal.defaultLogger;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentRunnerTest {

    @AfterEach
    public void cleanup() {
        PulumiTest.cleanup();
    }

    @Test
    void testTerminatesEarlyOnException() {
        var logger = defaultLogger();
        logger.setLevel(Level.OFF);
        var mock = PulumiTestInternal.withDefaults()
                .standardLogger(logger)
                .useRealRunner()
                .build();

        var result = mock.runTestAsync(ctx -> {
            Output.of(CompletableFuture.failedFuture(new RunException("Deliberate test error")));
            ctx.export("slowOutput", Output.of(
                    new CompletableFuture<Integer>()
                            .completeOnTimeout(1, 60, TimeUnit.SECONDS)
            ));
        }).join();

        assertThat(result.errors()).isNotNull();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors()).haveAtLeastOne(containsString("Deliberate test error"));

        assertThat(result.exceptions()).hasSize(2);
        assertThat(result.exceptions().get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(result.exceptions().get(1)).isExactlyInstanceOf(RunException.class);
        assertThat(result.exceptions().get(1)).hasMessageContaining("Deliberate test error");

        assertThat(result.resources()).isNotNull();
        assertThat(result.resources()).isNotEmpty();
        assertThat(result.resources()).hasSize(1);
        var stack = result.stack();
        assertThat(Internal.of(stack.output("slowOutput", TypeShape.of(Object.class))).getDataAsync()).isNotCompleted();
        assertThat(Internal.of(stack.output("slowOutput", TypeShape.of(Object.class))).getValueNullable()).isNotCompleted();

        assertThat(result.exitCode()).isEqualTo(ProcessExitedAfterLoggingUserActionableMessage);
    }

    @Test
    void testLogsTaskDescriptions() {
        // The test requires Level.FINEST
        var logger = InMemoryLogger.getLogger(Level.FINEST, "DeploymentRunnerTest#testLogsTaskDescriptions");
        var mock = PulumiTestInternal.withDefaults()
                .standardLogger(logger)
                .useRealRunner()
                .build();

        for (var i = 0; i < 2; i++) {
            final var delay = 100L + i;
            mock.runner().registerTask(String.format("task%d", i), new CompletableFuture<Void>().completeOnTimeout(null, delay, TimeUnit.MILLISECONDS));
        }
        var code = mock.runner().registerAndRunAsync(() -> null).join();
        assertThat(code.exitCode()).isEqualTo(0);

        var messages = logger.getMessages();
        for (var i = 0; i < 2; i++) {
            assertThat(messages).haveAtLeastOne(containsString(String.format("Registering task: 'task%d'", i)));
            assertThat(messages).haveAtLeastOne(containsString(String.format("Completed task: 'task%d'", i)));
        }
    }
}
