// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link CommandResult} represents the result of a command execution.
 */
public final class CommandResult {
    private final int code;
    private final String standardOutput;
    private final String standardError;

    public CommandResult(
            int code,
            String standardOutput,
            String standardError) {
        this.code = code;
        this.standardOutput = standardOutput;
        this.standardError = standardError;
    }

    /**
     * Gets the exit code of the command.
     *
     * @return the exit code
     */
    public int code() {
        return code;
    }

    /**
     * Gets the standard output of the command.
     *
     * @return the standard output
     */
    public String standardOutput() {
        return standardOutput;
    }

    /**
     * Gets the standard error of the command.
     *
     * @return the standard error
     */
    public String standardError() {
        return standardError;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var lineSeparator = System.lineSeparator();
        sb.append("code: ").append(this.code).append(lineSeparator);
        sb.append("stdout: ").append(this.standardOutput).append(lineSeparator);
        sb.append("stderr: ").append(this.standardError).append(lineSeparator);
        return sb.toString();
    }
}
