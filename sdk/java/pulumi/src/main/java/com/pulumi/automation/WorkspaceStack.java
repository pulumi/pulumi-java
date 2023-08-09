package com.pulumi.automation;

import com.pulumi.automation.internal.Shell;

import static java.util.Objects.requireNonNull;

public class WorkspaceStack {

    private final Workspace workspace;

    public WorkspaceStack(Workspace workspace) {
        this.workspace = requireNonNull(workspace);
    }

    public Workspace workspace() {
        return workspace;
    }

    /**
     * Creates or updates the resources in a stack by executing the program in the Workspace.
     * @see <a href="https://www.pulumi.com/docs/reference/cli/pulumi_up/"></a>
     * @param options Options to customize the behavior of the update.
     * @return the update result
     */
    public UpResult up(UpOptions options) {
        requireNonNull(options);

        var shell = new Shell();
        shell.run(
                "pulumi",
                "up",
                "--yes",
                "--skip-preview",
                String.format("--stack=%s", "test"),
                String.format("--client=127.0.0.1:%d", this.languageServerPort),
                String.format("--exec-kind=%s", "auto.inline")
        );
        return new UpResult(

        );
    }
}
