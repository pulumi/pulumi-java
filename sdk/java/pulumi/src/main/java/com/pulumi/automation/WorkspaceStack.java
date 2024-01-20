package com.pulumi.automation;

import com.pulumi.automation.internal.ExecKind;
import com.pulumi.automation.internal.LanguageRuntimeContext;
import com.pulumi.automation.internal.LanguageRuntimeServer;
import com.pulumi.automation.internal.LanguageRuntimeService;
import com.pulumi.automation.internal.Shell;
import com.pulumi.core.internal.Arrays;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class WorkspaceStack {

    private final Logger logger;
    private final Workspace workspace;
    private final StackSettings settings;

    public WorkspaceStack(
            Logger logger,
            Workspace workspace,
            StackSettings settings
    ) {
        this(logger, workspace, settings, s -> defaultInitializer(s));
    }

    public WorkspaceStack(
            Logger logger,
            Workspace workspace,
            StackSettings settings,
            Consumer<WorkspaceStack> initializer
    ) {
        this.logger = requireNonNull(logger);
        this.workspace = requireNonNull(workspace);
        this.settings = requireNonNull(settings);
        requireNonNull(initializer).accept(this);
    }

    public Workspace workspace() {
        return this.workspace;
    }

    private ExecKind execKind() {
        if (this.workspace.program().isPresent()) {
            return ExecKind.Inline;
        }
        return ExecKind.Local;
    }

    /**
     * Creates or updates the resources in a stack by executing the program in the Workspace.
     *
     * @param options Options to customize the behavior of the update.
     * @return the update result future
     * @see <a href="https://www.pulumi.com/docs/reference/cli/pulumi_up/"></a>
     */
    public CompletableFuture<UpResult> upAsync(UpOptions options) {
        requireNonNull(options); // FIXME

        var args = new String[]{
                "up",
                "--yes",
                "--skip-preview",
                String.format("--stack=%s", this.settings.name()),
        };

        return pulumiCmd(execKind(), args).thenApply(__ -> new UpResult(
                "", "", new UpdateSummary(UpdateKind.Update, new UpdateState()), Map.of() // FIXME
        ));
    }

    public CompletableFuture<Void> previewAsync() {
        var args = new String[]{
                "preview",
                String.format("--stack=%s", this.settings.name()),
        };

        return pulumiCmd(execKind(), args).thenApply(__ -> null);
    }

    private CompletableFuture<Integer> pulumiCmd(ExecKind kind, String... args) {
        var shell = new Shell(
                () -> null,
                line -> System.out.println(line),
                line -> System.err.println(line),
                this.workspace.environmentVariables(),
                this.workspace.workDir()
        );
        var pulumi = new String[]{"pulumi"};
        args = Arrays.concat(pulumi, args);
        if (kind != ExecKind.None) {
            args = Arrays.concat(args, new String[]{
                    String.format("--exec-kind=%s", kind.flag())
            });
        }
        if (kind == ExecKind.Inline) {
            // we need the server only for inline programs
            var server = createAndStartServer();
            args = Arrays.concat(args, new String[]{
                    String.format("--client=127.0.0.1:%d", server.port()),
            });
            return shell.run(args).whenComplete((integer, throwable) -> {
                server.shutdown();
            });
        }
        return shell.run(args);
    }

    private LanguageRuntimeServer createAndStartServer() {
        var program = this.workspace.program().orElseThrow(
                () -> new IllegalStateException("expected inline program, got none")
        );
        var context = new LanguageRuntimeContext(program);
        var service = new LanguageRuntimeService(this.logger, context);
        var server = new LanguageRuntimeServer(this.logger, service);
        try {
            server.start();
        } catch (Throwable t) {
            server.shutdown();
            throw t;
        }
        return server;
    }

    private static void defaultInitializer(WorkspaceStack stack) {
        if (stack.select() > 0) {
            if (stack.create() > 0) {
                throw new IllegalStateException("stack creation failed");
            };
        }
    }

    private Integer select() {
        var args = new String[]{
                "stack",
                "init",
                this.settings.name(),
        };
        return pulumiCmd(ExecKind.None, args).join();
    }

    private Integer create() {
        var args = new String[]{
                "stack",
                "select",
                String.format("--stack=%s", this.settings.name()),
        };
        return pulumiCmd(ExecKind.None, args).join();
    }
}
