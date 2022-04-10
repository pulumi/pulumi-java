// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.documentdb;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.documentdb.inputs.GetSqlResourceSqlTriggerArgs;
import io.pulumi.azurenative.documentdb.outputs.GetSqlResourceSqlTriggerResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSqlResourceSqlTrigger {
    private GetSqlResourceSqlTrigger() {}
    /**
         * An Azure Cosmos DB trigger.
     * API Version: 2021-03-15.
     * 
     *
         * An Azure Cosmos DB trigger.
     * 
     */
    public static CompletableFuture<GetSqlResourceSqlTriggerResult> invokeAsync(GetSqlResourceSqlTriggerArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:documentdb:getSqlResourceSqlTrigger", TypeShape.of(GetSqlResourceSqlTriggerResult.class), args == null ? GetSqlResourceSqlTriggerArgs.Empty : args, Utilities.withVersion(options));
    }
}