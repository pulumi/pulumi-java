package com.pulumi.provider.internal;

import java.util.concurrent.CompletableFuture;

import com.pulumi.provider.internal.models.*;

public interface Provider {
    /**
     * Returns the schema for this provider's package.
     * @param request The schema request
     * @return A future containing the schema response
     */
    CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request);
}
