// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.documentdb;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.documentdb.inputs.GetGremlinResourceGremlinGraphArgs;
import io.pulumi.azurenative.documentdb.outputs.GetGremlinResourceGremlinGraphResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetGremlinResourceGremlinGraph {
    private GetGremlinResourceGremlinGraph() {}
    /**
         * An Azure Cosmos DB Gremlin graph.
     * API Version: 2021-03-15.
     * 
     *
         * An Azure Cosmos DB Gremlin graph.
     * 
     */
    public static CompletableFuture<GetGremlinResourceGremlinGraphResult> invokeAsync(GetGremlinResourceGremlinGraphArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:documentdb:getGremlinResourceGremlinGraph", TypeShape.of(GetGremlinResourceGremlinGraphResult.class), args == null ? GetGremlinResourceGremlinGraphArgs.Empty : args, Utilities.withVersion(options));
    }
}