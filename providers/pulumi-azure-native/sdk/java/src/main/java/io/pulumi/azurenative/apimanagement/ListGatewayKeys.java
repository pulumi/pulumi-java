// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.apimanagement;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.apimanagement.inputs.ListGatewayKeysArgs;
import io.pulumi.azurenative.apimanagement.outputs.ListGatewayKeysResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListGatewayKeys {
    private ListGatewayKeys() {}
    /**
         * Gateway authentication keys.
     * API Version: 2020-12-01.
     * 
     *
         * Gateway authentication keys.
     * 
     */
    public static CompletableFuture<ListGatewayKeysResult> invokeAsync(ListGatewayKeysArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:apimanagement:listGatewayKeys", TypeShape.of(ListGatewayKeysResult.class), args == null ? ListGatewayKeysArgs.Empty : args, Utilities.withVersion(options));
    }
}