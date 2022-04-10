// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.compute;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.compute.inputs.GetRestorePointCollectionArgs;
import io.pulumi.azurenative.compute.outputs.GetRestorePointCollectionResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRestorePointCollection {
    private GetRestorePointCollection() {}
    /**
         * Create or update Restore Point collection parameters.
     * API Version: 2021-03-01.
     * 
     *
         * Create or update Restore Point collection parameters.
     * 
     */
    public static CompletableFuture<GetRestorePointCollectionResult> invokeAsync(GetRestorePointCollectionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:compute:getRestorePointCollection", TypeShape.of(GetRestorePointCollectionResult.class), args == null ? GetRestorePointCollectionArgs.Empty : args, Utilities.withVersion(options));
    }
}