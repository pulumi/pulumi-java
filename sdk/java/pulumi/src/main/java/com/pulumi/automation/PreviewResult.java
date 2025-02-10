// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.Map;

/**
 * Represents the result of a preview operation.
 */
public class PreviewResult {
    private final String standardOutput;
    private final String standardError;
    private final Map<OperationType, Integer> changeSummary;

    PreviewResult(
            String standardOutput,
            String standardError,
            Map<OperationType, Integer> changeSummary) {
        this.standardOutput = standardOutput;
        this.standardError = standardError;
        this.changeSummary = changeSummary;
    }

    /**
     * Returns the standard output of the preview operation.
     *
     * @return the standard output
     */
    public String standardOutput() {
        return standardOutput;
    }

    /**
     * Returns the standard error of the preview operation.
     *
     * @return the standard error
     */
    public String standardError() {
        return standardError;
    }

    /**
     * Returns a summary of the changes that would be applied by the preview
     * operation.
     *
     * @return
     */
    public Map<OperationType, Integer> changeSummary() {
        return changeSummary;
    }
}
