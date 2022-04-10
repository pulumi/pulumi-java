// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.deploymentmanager;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.deploymentmanager.inputs.GetRolloutArgs;
import io.pulumi.azurenative.deploymentmanager.outputs.GetRolloutResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRollout {
    private GetRollout() {}
    /**
         * Defines the PUT rollout request body.
     * API Version: 2019-11-01-preview.
     * 
     *
         * Defines the rollout.
     * 
     */
    public static CompletableFuture<GetRolloutResult> invokeAsync(GetRolloutArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:deploymentmanager:getRollout", TypeShape.of(GetRolloutResult.class), args == null ? GetRolloutArgs.Empty : args, Utilities.withVersion(options));
    }
}