// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The result of an up operation.
 */
public final class UpResult extends UpdateResult {
    private final Map<String, OutputValue> outputs;

    UpResult(
            String standardOutput,
            String standardError,
            UpdateSummary summary,
            Map<String, OutputValue> outputs) {
        super(standardOutput, standardError, summary);
        this.outputs = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(outputs)));
    }

    /**
     * Returns the outputs of the update.
     *
     * @return the outputs
     */
    public Map<String, OutputValue> outputs() {
        return outputs;
    }
}
