// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.netapp;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.netapp.inputs.GetSnapshotPolicyArgs;
import io.pulumi.azurenative.netapp.outputs.GetSnapshotPolicyResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSnapshotPolicy {
    private GetSnapshotPolicy() {}
    /**
         * Snapshot policy information
     * API Version: 2020-12-01.
     * 
     *
         * Snapshot policy information
     * 
     */
    public static CompletableFuture<GetSnapshotPolicyResult> invokeAsync(GetSnapshotPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:netapp:getSnapshotPolicy", TypeShape.of(GetSnapshotPolicyResult.class), args == null ? GetSnapshotPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}