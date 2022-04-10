// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.synapse;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.synapse.inputs.GetSqlPoolArgs;
import io.pulumi.azurenative.synapse.outputs.GetSqlPoolResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSqlPool {
    private GetSqlPool() {}
    /**
         * A SQL Analytics pool
     * API Version: 2021-03-01.
     * 
     *
         * A SQL Analytics pool
     * 
     */
    public static CompletableFuture<GetSqlPoolResult> invokeAsync(GetSqlPoolArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:synapse:getSqlPool", TypeShape.of(GetSqlPoolResult.class), args == null ? GetSqlPoolArgs.Empty : args, Utilities.withVersion(options));
    }
}