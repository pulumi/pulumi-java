// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.pulumi.automation.events.internal.EventLogWatcher;
import com.pulumi.automation.events.internal.EventsServer;
import com.pulumi.core.internal.ContextAwareCompletableFuture;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * A {@link PulumiCommand} implementation that uses a locally installed Pulumi CLI.
 */
public class LocalPulumiCommand implements PulumiCommand {
    static final String SKIP_VERSION_CHECK_VAR = "PULUMI_AUTOMATION_API_SKIP_VERSION_CHECK";

    private static final Version MINIMUM_VERSION = Version.of(3, 95);
    private static final Version GRPC_EVENT_LOG_VERSION = Version.of(3, 205, 0);

    private static final Pattern NOT_FOUND_REGEX_PATTERN = Pattern.compile("no stack named.*found");
    private static final Pattern ALREADY_EXISTS_REGEX_PATTERN = Pattern.compile("stack.*already exists");
    private static final String CONFLICT_TEXT = "[409] Conflict: Another update is currently in progress.";
    private static final String LOCAL_BACKEND_CONFLICT_TEXT = "the stack is currently locked by";

    private final String command;
    @Nullable
    private final Version version;

    private LocalPulumiCommand(String command, @Nullable Version version) {
        this.command = command;
        this.version = version;
    }

    /**
     * Creates a new {@link LocalPulumiCommand} instance.
     *
     * @return a new {@link LocalPulumiCommand} instance
     * @throws AutomationException if an error occurs
     */
    public static LocalPulumiCommand create() throws AutomationException {
        return create(LocalPulumiCommandOptions.Empty);
    }

    /**
     * Creates a new {@link LocalPulumiCommand} instance.
     *
     * @param options options to configure the {@link LocalPulumiCommand}
     * @return a new {@link LocalPulumiCommand} instance
     * @throws AutomationException if an error occurs
     */
    public static LocalPulumiCommand create(LocalPulumiCommandOptions options) throws AutomationException {
        var command = "pulumi";
        var optOut = options != null
                ? options.isSkipVersionCheck()
                : System.getenv(SKIP_VERSION_CHECK_VAR) != null;
        var version = getPulumiVersion(MINIMUM_VERSION, command, optOut).orElse(null);
        return new LocalPulumiCommand(command, version);
    }

    private static Optional<Version> getPulumiVersion(
            Version minimumVersion,
            String command,
            boolean optOut) throws AutomationException {

        var processBuilder = new ProcessBuilder(command, "version");
        try {
            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new AutomationException("Failed to get Pulumi version: " + output);
            }
            var version = output.trim();
            if (version.startsWith("v")) {
                version = version.substring(1);
            }
            return parseAndValidatePulumiVersion(minimumVersion, version, optOut);
        } catch (AutomationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupt flag
            throw new AutomationException(e);
        } catch (Exception e) {
            throw new AutomationException(e);
        }
    }

    private static Optional<Version> parseAndValidatePulumiVersion(
            Version minVersion,
            String currentVersion,
            boolean optOut) throws AutomationException {
        var optionalVersion = Version.tryParse(currentVersion);
        if (optOut) {
            return optionalVersion;
        }
        if (optionalVersion.isEmpty()) {
            throw new AutomationException("Failed to get Pulumi version. This is probably a pulumi error. " +
                    "You can override version checking by setting " + SKIP_VERSION_CHECK_VAR + "=true");
        }
        var version = optionalVersion.get();
        if (minVersion.majorVersion() < version.majorVersion()) {
            throw new AutomationException("Major version mismatch. You are using Pulumi CLI version " + version +
                    " with Automation SDK that requires v" + minVersion.majorVersion() + ". Please update the SDK.");
        }
        if (minVersion.compareTo(version) > 0) {
            throw new AutomationException("Minimum version requirement failed. The minimum CLI version requirement " +
                    "is " + minVersion + ", your current CLI version is " + version + ". " +
                    "Please update the Pulumi CLI.");
        }
        return optionalVersion;
    }

    @Nullable
    @Override
    public Version version() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandResult run(List<String> args, CommandRunOptions options) throws AutomationException {
        if (options != null && options.onEngineEvent() != null) {
            if (version != null && version.compareTo(GRPC_EVENT_LOG_VERSION) > 0) {
                return runWithGrpcEventLog(args, options);
            } else {
                return runWithFileEventLog(args, options);
            }
        }

        return runInternal(args, options, null);
    }

    /**
     * Run the command with gRPC-based event logging.
     */
    private CommandResult runWithGrpcEventLog(List<String> args, CommandRunOptions options) throws AutomationException {
        var maxRpcMessageSize = 400 * 1024 * 1024; // 400MB
        var eventsServer = new EventsServer(options.onEngineEvent());
        var server = ServerBuilder.forPort(0)
                .maxInboundMessageSize(maxRpcMessageSize)
                .addService(eventsServer)
                .build();

        try {
            server.start();
            var port = server.getPort();
            var eventLogAddress = "tcp://127.0.0.1:" + port;

            return runInternal(args, options, eventLogAddress);
        } catch (IOException e) {
            throw new AutomationException("Failed to start gRPC events server", e);
        } catch (AutomationException e) {
            throw e;
        } catch (Exception e) {
            throw new AutomationException(e);
        } finally {
            if (server != null) {
                server.shutdown();
            }
        }
    }

    /**
     * Run the command with file-based event logging.
     */
    private CommandResult runWithFileEventLog(List<String> args, CommandRunOptions options) throws AutomationException {
        var firstArg = args != null && !args.isEmpty() ? args.get(0) : null;
        var commandName = sanitizeCommandName(firstArg);
        try (var eventLogFile = new EventLogFile(commandName);
                var eventLogWatcher = new EventLogWatcher(eventLogFile.filePath(),
                        options.onEngineEvent())) {
            return runInternal(args, options, eventLogFile.filePath().toString());
        } catch (AutomationException e) {
            throw e;
        } catch (Exception e) {
            throw new AutomationException(e);
        }
    }

    private CommandResult runInternal(
            List<String> args,
            CommandRunOptions options,
            @Nullable String eventLogLocation) throws AutomationException {
        var processBuilder = new ProcessBuilder(command);
        processBuilder.command().addAll(pulumiArgs(args, eventLogLocation));
        var workingDir = options.workingDir();
        if (workingDir != null) {
            processBuilder.directory(workingDir.toFile());
        }

        var env = processBuilder.environment();
        var debugCommands = eventLogLocation != null;
        env.putAll(pulumiEnvironment(options.additionalEnv(), command, debugCommands));

        var executor = Executors.newFixedThreadPool(2);

        try {
            var process = processBuilder.start();

            var stdoutFuture = readStreamAsync(process.getInputStream(), executor, options.onStandardOutput());
            var stderrFuture = readStreamAsync(process.getErrorStream(), executor, options.onStandardError());

            var stdIn = options.standardInput();
            if (stdIn != null && !stdIn.isBlank()) {
                try (var writer = new OutputStreamWriter(process.getOutputStream())) {
                    writer.write(stdIn);
                    writer.flush();
                }
            }

            int exitCode = process.waitFor();
            String stdout = stdoutFuture.join();
            String stderr = stderrFuture.join();

            var result = new CommandResult(exitCode, stdout, stderr);

            if (exitCode != 0) {
                throw createExceptionFromResult(result);
            }

            return result;
        } catch (AutomationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupt flag
            throw new AutomationException(e);
        } catch (Exception e) {
            throw new AutomationException(e);
        } finally {
            executor.shutdown();
        }
    }

    private CompletableFuture<String> readStreamAsync(
            InputStream in,
            Executor executor,
            @Nullable Consumer<String> lineConsumer) {
        return ContextAwareCompletableFuture.supplyAsync(() -> {
            var sb = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                    if (lineConsumer != null) {
                        lineConsumer.accept(line);
                    }
                }
                return sb.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor);
    }

    static List<String> pulumiArgs(List<String> args, String eventLogLocation) {
        var result = new ArrayList<String>(args);

        // all commands should be run in non-interactive mode.
        // this causes commands to fail rather than prompting for input (and thus
        // hanging indefinitely)
        if (!result.contains("--non-interactive")) {
            result.add("--non-interactive");
        }

        if (eventLogLocation != null) {
            result.add("--event-log");
            result.add(eventLogLocation);
        }

        return result;
    }

    static Map<String, String> pulumiEnvironment(
            Map<String, String> additionalEnv,
            String command,
            boolean debugCommands) {

        var env = new HashMap<String, String>(additionalEnv);

        if (debugCommands) {
            // Required for event log
            // We add it after the provided env vars to ensure it is set to true
            env.put("PULUMI_DEBUG_COMMANDS", "true");
        }

        var commandPath = Path.of(command);
        if (commandPath.isAbsolute()) {
            // Prefix PATH with the directory of the pulumi command being run to ensure we
            // prioritize the bundled plugins from the CLI installation.
            var originalEnvPath = env.getOrDefault("PATH", "");
            if (!originalEnvPath.isEmpty()) {
                originalEnvPath = File.pathSeparator + originalEnvPath;
            }
            env.put("PATH", commandPath.getParent().toString() + originalEnvPath);
        }

        return env;
    }

    static CommandException createExceptionFromResult(CommandResult result) {
        if (NOT_FOUND_REGEX_PATTERN.matcher(result.standardError()).find()) {
            return new StackNotFoundException(result);
        } else if (ALREADY_EXISTS_REGEX_PATTERN.matcher(result.standardError()).find()) {
            return new StackAlreadyExistsException(result);
        } else if (result.standardError().indexOf(CONFLICT_TEXT) >= 0 ||
                result.standardError().indexOf(LOCAL_BACKEND_CONFLICT_TEXT) >= 0) {
            return new ConcurrentUpdateException(result);
        } else {
            return new CommandException(result);
        }
    }

    private static String sanitizeCommandName(String firstArgument) {
        var alphaNumWord = Pattern.compile("^[-A-Za-z0-9_]{1,20}$");
        if (firstArgument == null) {
            return "event-log";
        }
        return alphaNumWord.matcher(firstArgument).matches() ? firstArgument : "event-log";
    }

    private final class EventLogFile implements AutoCloseable {
        private final Path filePath;

        public EventLogFile(String command) throws IOException {
            var tempDir = Files.createTempDirectory("automation-logs-" + command + "-");
            filePath = tempDir.resolve("eventlog.txt");
            Files.createFile(filePath);
        }

        public Path filePath() {
            return filePath;
        }

        @Override
        public void close() throws Exception {
            try {
                Files.walk(this.filePath.getParent())
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Silently ignore
                            }
                        });
            } catch (Exception e) {
                // allow graceful exit if for some reason
                // we're not able to delete the directory
                // will rely on OS to clean temp directory
                // in this case.
            }
        }
    }
}
