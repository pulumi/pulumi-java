package com.pulumi.automation;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class UpResult {
    private final String stdout;
    private final String stderr;
    private final UpdateSummary summary;
    private final Map<String, ValueOrSecret> outputs;

    public UpResult(
            String stdout,
            String stderr,
            UpdateSummary summary,
            Map<String, ValueOrSecret> outputs
    ) {
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
        this.summary = requireNonNull(summary);
        this.outputs = requireNonNull(outputs);
    }

    public String stdout() {
        return stdout;
    }

    public String stderr() {
        return stderr;
    }

    public UpdateSummary summary() {
        return summary;
    }

    public Map<String, ValueOrSecret> outputs() {
        return outputs;
    }
}
