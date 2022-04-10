// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.cache;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.cache.inputs.GetDatabaseArgs;
import io.pulumi.azurenative.cache.outputs.GetDatabaseResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDatabase {
    private GetDatabase() {}
    /**
         * Describes a database on the RedisEnterprise cluster
     * API Version: 2021-03-01.
     * 
     *
         * Describes a database on the RedisEnterprise cluster
     * 
     */
    public static CompletableFuture<GetDatabaseResult> invokeAsync(GetDatabaseArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:cache:getDatabase", TypeShape.of(GetDatabaseResult.class), args == null ? GetDatabaseArgs.Empty : args, Utilities.withVersion(options));
    }
}