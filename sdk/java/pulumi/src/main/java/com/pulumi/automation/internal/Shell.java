package com.pulumi.automation.internal;

import com.google.common.collect.ImmutableMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class Shell {

    private final Supplier<String> stdin;
    private final Consumer<String> stdout;
    private final Consumer<String> stderr;
    private final Map<String, String> environmentVariables;
    private final Path workDir;

    public Shell(
            Supplier<String> stdin,
            Consumer<String> stdout,
            Consumer<String> stderr,
            Map<String, String> environmentVariables,
            Path workDir) {
        this.stdin = requireNonNull(stdin);
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
        this.environmentVariables = ImmutableMap.copyOf(environmentVariables);
        this.workDir = requireNonNull(workDir);
    }

    public CompletableFuture<Integer> run(String... command) {
        return CompletableFuture.supplyAsync(() -> {
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(workDir.toFile());
            builder.environment().putAll(this.environmentVariables);
            builder.command(command);
            try {
                return builder.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).thenCompose(process -> {
            var codeAsync = process.onExit();
            var done = CompletableFuture.allOf(
                    codeAsync,
                    redirect(process.getErrorStream(), this.stderr),
                    redirect(process.getInputStream(), this.stdout),
                    redirect(this.stdin, process.getOutputStream())
            );
            return done.thenApply(ignore -> codeAsync.join().exitValue());
        });
    }

    private CompletableFuture<Void> redirect(final InputStream stream, final Consumer<String> lines) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                reader.lines().forEachOrdered(lines);
                future.complete(null);
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Void> redirect(final Supplier<String> lines, final OutputStream stream) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {
                produce(lines, writer);
                future.complete(null);
            } catch (IOException e) {
                future.completeExceptionally(e);
            } catch (UncheckedIOException e) {
                future.completeExceptionally(e.getCause());
            }
        });
        return future;
    }

    private static void produce(final Supplier<String> supplier, final BufferedWriter writer) {
        Stream.generate(supplier).takeWhile(line -> line != null)
                .forEachOrdered(line -> {
                    try {
                        writer.write(line);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
