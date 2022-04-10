// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.synapse;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.synapse.inputs.GetPrivateLinkHubArgs;
import io.pulumi.azurenative.synapse.outputs.GetPrivateLinkHubResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPrivateLinkHub {
    private GetPrivateLinkHub() {}
    /**
         * A privateLinkHub
     * API Version: 2021-03-01.
     * 
     *
         * A privateLinkHub
     * 
     */
    public static CompletableFuture<GetPrivateLinkHubResult> invokeAsync(GetPrivateLinkHubArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:synapse:getPrivateLinkHub", TypeShape.of(GetPrivateLinkHubResult.class), args == null ? GetPrivateLinkHubArgs.Empty : args, Utilities.withVersion(options));
    }
}