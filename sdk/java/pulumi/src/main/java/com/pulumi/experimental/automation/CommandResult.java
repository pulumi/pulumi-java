// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

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

    public int getCode() {
        return code;
    }

    public String getStandardOutput() {
        return standardOutput;
    }

    public String getStandardError() {
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
