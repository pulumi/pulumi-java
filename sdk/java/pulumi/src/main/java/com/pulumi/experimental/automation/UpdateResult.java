// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

/**
 * The result of an update operation.
 */
public class UpdateResult {
    private final String standardOutput;
    private final String standardError;
    private final UpdateSummary summary;

    UpdateResult(
            String standardOutput,
            String standardError,
            UpdateSummary summary) {
        this.standardOutput = standardOutput;
        this.standardError = standardError;
        this.summary = summary;
    }

    /**
     * Returns the standard output of the update operation.
     *
     * @return the standard output
     */
    public String standardOutput() {
        return standardOutput;
    }

    /**
     * Returns the standard error of the update operation.
     *
     * @return the standard error
     */
    public String standardError() {
        return standardError;
    }

    /**
     * Returns a summary of the changes that were applied by the update operation.
     *
     * @return the summary
     */
    public UpdateSummary summary() {
        return summary;
    }
}
