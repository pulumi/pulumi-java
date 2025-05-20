// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class for manipulating stack state for a given {@link WorkspaceStack}.
 */
public final class WorkspaceStackState {
    private final WorkspaceStack workspaceStack;

    public WorkspaceStackState(WorkspaceStack workspaceStack) {
        this.workspaceStack = Objects.requireNonNull(workspaceStack);
    }

    /**
     * This command deletes a resource from a stack's state, as long as it is safe
     * to do so. The resource is specified by its Pulumi URN.
     *
     * Resources can't be deleted if there exist other resources that depend on it
     * or are parented to it. Protected resources will not be deleted unless it is
     * specifically requested using the force flag.
     *
     * @param urn The Pulumi URN of the resource to be deleted
     * @throws AutomationException if an error occurs
     */
    public void delete(String urn) throws AutomationException {
        delete(urn, false);
    }

    /**
     * This command deletes a resource from a stack's state, as long as it is safe
     * to do so. The resource is specified by its Pulumi URN.
     *
     * Resources can't be deleted if there exist other resources that depend on it
     * or are parented to it. Protected resources will not be deleted unless it is
     * specifically requested using the force flag.
     *
     * @param urn   The Pulumi URN of the resource to be deleted
     * @param force A boolean indicating whether the deletion should be forced
     * @throws AutomationException if an error occurs
     */
    public void delete(String urn, boolean force) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("state");
        args.add("delete");
        args.add(Objects.requireNonNull(urn));

        if (force) {
            args.add("--force");
        }

        workspaceStack.runCommand(args);
    }

    /**
     * Unprotect a resource in a stack's state. This command clears the 'protect'
     * bit on the provided resource URN, allowing the resource to be deleted.
     *
     * @param urn The Pulumi URN to be unprotected
     * @throws AutomationException if an error occurs
     */
    public void unprotect(String urn) throws AutomationException {
        var args = List.of("state", "unprotect", Objects.requireNonNull(urn));
        workspaceStack.runCommand(args);
    }

    /**
     * Unprotect all resources in a stack's state.
     * @throws AutomationException if an error occurs
     */
    public void unprotectAll() throws AutomationException {
        var args = List.of("state", "unprotect", "--all");
        workspaceStack.runCommand(args);
    }
}
