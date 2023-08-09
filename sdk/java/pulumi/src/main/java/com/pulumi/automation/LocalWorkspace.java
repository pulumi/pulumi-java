package com.pulumi.automation;

public class LocalWorkspace implements Workspace {
    @Override
    public WorkspaceStack upsertStack(StackSettings options) {
        return new WorkspaceStack(this);
    }
}
