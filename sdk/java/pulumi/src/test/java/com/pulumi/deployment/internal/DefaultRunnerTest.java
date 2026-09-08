package com.pulumi.deployment.internal;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for https://github.com/pulumi/pulumi-java/issues/2270: the
 * drain loop in {@link DeploymentImpl.DefaultRunner} must not re-submit itself
 * to the common pool in a tight loop, otherwise a program livelocks on Java 25+
 * when the common ForkJoinPool parallelism is 1.
 */
public class DefaultRunnerTest {

    @Test
    void testDrainLoopDoesNotBusySpin() throws Exception {
        var log = InMemoryLogger.getLogger(Level.FINEST, "DefaultRunnerTest#testDrainLoopDoesNotBusySpin");
        var runner = new DeploymentImpl.DefaultRunner(log, EngineLogger.ignore());

        var result = runner.runAsync(() -> {
            registerDelayedTask(runner, 250);
            return 0;
        }).get(60, TimeUnit.SECONDS);
        assertThat(result.exitCode()).isZero();

        // The loop logs "Remaining tasks" once per iteration. Polling every
        // 10ms while the 250ms task runs yields a few dozen iterations; a
        // tight resubmission loop yields tens of thousands.
        var polls = log.getMessages().stream().filter(m -> m.contains("Remaining tasks")).count();
        assertThat(polls).isLessThan(1000);
    }

    @Test
    void testCompletesWithCommonPoolParallelismOne() throws Exception {
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var process = new ProcessBuilder(
                java,
                "-Djava.util.concurrent.ForkJoinPool.common.parallelism=1",
                "-cp", System.getProperty("java.class.path"),
                ParallelismOneProgram.class.getName())
                .inheritIO()
                .start();
        var finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished)
                .as("a DefaultRunner program with ForkJoinPool.common.parallelism=1 should not livelock")
                .isTrue();
        assertThat(process.exitValue()).isZero();
    }

    public static class ParallelismOneProgram {
        public static void main(String[] args) throws Exception {
            var runner = new DeploymentImpl.DefaultRunner(
                    Logger.getLogger("ParallelismOneProgram"), EngineLogger.ignore());
            var result = runner.runAsync(() -> {
                registerDelayedTask(runner, 250);
                return 0;
            }).get(45, TimeUnit.SECONDS);
            System.exit(result.exitCode());
        }
    }

    /**
     * Registers a task that is completed from a plain thread after a delay, so
     * that its async continuation has to run on the common pool, like the
     * continuations of gRPC calls in a real program.
     */
    private static void registerDelayedTask(DeploymentImpl.DefaultRunner runner, long millis) {
        var task = new CompletableFuture<Void>();
        var thread = new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            task.complete(null);
        });
        thread.start();
        runner.registerTask("DefaultRunnerTest", task.thenApplyAsync(v -> v));
    }
}
