package com.pulumi.provider.internal;

import java.util.concurrent.CompletableFuture;

import com.pulumi.provider.internal.models.*;

public abstract class Provider {
    public CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request) {
        throw new UnsupportedOperationException("Method 'getSchema' is not implemented");
    }
}
