// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.storage;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.storage.inputs.GetObjectReplicationPolicyArgs;
import io.pulumi.azurenative.storage.outputs.GetObjectReplicationPolicyResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetObjectReplicationPolicy {
    private GetObjectReplicationPolicy() {}
    /**
         * The replication policy between two storage accounts. Multiple rules can be defined in one policy.
     * API Version: 2021-02-01.
     * 
     *
         * The replication policy between two storage accounts. Multiple rules can be defined in one policy.
     * 
     */
    public static CompletableFuture<GetObjectReplicationPolicyResult> invokeAsync(GetObjectReplicationPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:storage:getObjectReplicationPolicy", TypeShape.of(GetObjectReplicationPolicyResult.class), args == null ? GetObjectReplicationPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}