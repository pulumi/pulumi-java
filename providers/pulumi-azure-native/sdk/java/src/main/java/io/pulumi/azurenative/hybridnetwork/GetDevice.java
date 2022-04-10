// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.hybridnetwork;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.hybridnetwork.inputs.GetDeviceArgs;
import io.pulumi.azurenative.hybridnetwork.outputs.GetDeviceResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDevice {
    private GetDevice() {}
    /**
         * Device resource.
     * API Version: 2020-01-01-preview.
     * 
     *
         * Device resource.
     * 
     */
    public static CompletableFuture<GetDeviceResult> invokeAsync(GetDeviceArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:hybridnetwork:getDevice", TypeShape.of(GetDeviceResult.class), args == null ? GetDeviceArgs.Empty : args, Utilities.withVersion(options));
    }
}