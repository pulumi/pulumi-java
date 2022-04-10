// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.analysisservices;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.analysisservices.inputs.ListServerGatewayStatusArgs;
import io.pulumi.azurenative.analysisservices.outputs.ListServerGatewayStatusResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListServerGatewayStatus {
    private ListServerGatewayStatus() {}
    /**
         * Status of gateway is live.
     * API Version: 2017-08-01.
     * 
     *
         * Status of gateway is live.
     * 
     */
    public static CompletableFuture<ListServerGatewayStatusResult> invokeAsync(ListServerGatewayStatusArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:analysisservices:listServerGatewayStatus", TypeShape.of(ListServerGatewayStatusResult.class), args == null ? ListServerGatewayStatusArgs.Empty : args, Utilities.withVersion(options));
    }
}