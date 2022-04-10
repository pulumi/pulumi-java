// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.compute;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.compute.inputs.GetSnapshotArgs;
import io.pulumi.azurenative.compute.outputs.GetSnapshotResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSnapshot {
    private GetSnapshot() {}
    /**
         * Snapshot resource.
     * API Version: 2020-12-01.
     * 
     *
         * Snapshot resource.
     * 
     */
    public static CompletableFuture<GetSnapshotResult> invokeAsync(GetSnapshotArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:compute:getSnapshot", TypeShape.of(GetSnapshotResult.class), args == null ? GetSnapshotArgs.Empty : args, Utilities.withVersion(options));
    }
}