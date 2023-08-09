package com.pulumi.automation;

public interface Workspace {
    WorkspaceStack upsertStack(StackSettings options);
}
