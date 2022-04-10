// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.storagesync;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.storagesync.inputs.GetSyncGroupArgs;
import io.pulumi.azurenative.storagesync.outputs.GetSyncGroupResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSyncGroup {
    private GetSyncGroup() {}
    /**
         * Sync Group object.
     * API Version: 2020-03-01.
     * 
     *
         * Sync Group object.
     * 
     */
    public static CompletableFuture<GetSyncGroupResult> invokeAsync(GetSyncGroupArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:storagesync:getSyncGroup", TypeShape.of(GetSyncGroupResult.class), args == null ? GetSyncGroupArgs.Empty : args, Utilities.withVersion(options));
    }
}