package com.pulumi.provider.internal;

import java.util.concurrent.CompletableFuture;

import com.pulumi.provider.internal.models.*;

/**
 * An interface representing Pulumi Provider that can be configured, provide schemas, and construct resources.
 */
public interface Provider {
    /**
     * Configure the provider with the given configuration parameters.
     * @param request The configuration request
     * @return A future containing the configuration response
     */
    default CompletableFuture<ConfigureResponse> configure(ConfigureRequest request) {
        return CompletableFuture.completedFuture(
            new ConfigureResponse(true, true, true, true));
    }

    /**
     * Returns the schema for this provider's package.
     * @param request The schema request
     * @return A future containing the schema response
     */
    CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request);    
    
    /**
     * Construct a component resource given the type name and inputs, and returning the URN and
     * any outputs.
     * @param request The construction request
     * @return A future containing the construction response
     */
    CompletableFuture<ConstructResponse> construct(ConstructRequest request);
}
