package com.pulumi.automation;

import static java.util.Objects.requireNonNull;

public class UpdateSummary {

    // pre-update information
    private final UpdateKind kind;
    // post-update information
    private final UpdateState result;

    public UpdateSummary(
            UpdateKind kind,
            UpdateState result
    ) {
        this.kind = requireNonNull(kind);
        this.result = requireNonNull(result);
    }
}
